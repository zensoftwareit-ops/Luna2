<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;

final class TrialBalanceService
{
    private const BALANCE_SHEET_TYPES = ['ASSET', 'LIABILITY', 'EQUITY'];

    public function __construct(private readonly PDO $db, private readonly int $organizationId) {}

    public function dataset(string $from, string $to, string $search = '', string $accountType = ''): array
    {
        foreach ([$from, $to] as $value) {
            $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
            if (!$date || $date->format('Y-m-d') !== $value) {
                throw new InvalidArgumentException('Intervallo della situazione contabile non valido.');
            }
        }
        if ($from > $to) {
            throw new InvalidArgumentException('La data iniziale non può essere successiva alla data finale.');
        }

        $accountType = strtoupper(trim($accountType));
        if ($accountType !== '' && !in_array($accountType, ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'], true)) {
            throw new InvalidArgumentException('Tipo conto non valido.');
        }

        $opening = $this->db->prepare(
            "SELECT COUNT(*) FROM journal_entries
             WHERE organization_id = ? AND status = 'POSTED' AND entry_type = 'OPENING'
               AND entry_date BETWEEN ? AND ?"
        );
        $opening->execute([$this->organizationId, $from, $to]);
        $hasPostedOpening = (int) $opening->fetchColumn() > 0;

        $statement = $this->db->prepare(
            "SELECT a.id, a.code, a.name, a.account_type, a.parent_id, a.is_postable,
                    COALESCE(SUM(CASE WHEN e.entry_date < :prior_before THEN l.debit ELSE 0 END), 0) AS prior_debit,
                    COALESCE(SUM(CASE WHEN e.entry_date < :prior_before_credit THEN l.credit ELSE 0 END), 0) AS prior_credit,
                    COALESCE(SUM(CASE WHEN e.entry_date BETWEEN :period_from_debit AND :period_to_debit THEN l.debit ELSE 0 END), 0) AS period_debit,
                    COALESCE(SUM(CASE WHEN e.entry_date BETWEEN :period_from_credit AND :period_to_credit THEN l.credit ELSE 0 END), 0) AS period_credit
             FROM chart_of_accounts a
             LEFT JOIN journal_entry_lines l ON l.account_id = a.id AND l.organization_id = a.organization_id
             LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = a.organization_id
                  AND e.status = 'POSTED' AND e.entry_date <= :maximum_date
             WHERE a.organization_id = :organization_id
             GROUP BY a.id, a.code, a.name, a.account_type, a.parent_id, a.is_postable
             ORDER BY a.code"
        );
        $statement->execute([
            'prior_before' => $from, 'prior_before_credit' => $from,
            'period_from_debit' => $from, 'period_to_debit' => $to,
            'period_from_credit' => $from, 'period_to_credit' => $to,
            'maximum_date' => $to, 'organization_id' => $this->organizationId,
        ]);
        $accounts = [];
        foreach ($statement->fetchAll() as $row) {
            $type = (string) $row['account_type'];
            $prior = in_array($type, self::BALANCE_SHEET_TYPES, true) && !$hasPostedOpening
                ? self::cents($row['prior_debit']) - self::cents($row['prior_credit'])
                : 0;
            $periodDebit = self::cents($row['period_debit']);
            $periodCredit = self::cents($row['period_credit']);
            $row['opening_net_cents'] = $prior;
            $row['period_debit_cents'] = $periodDebit;
            $row['period_credit_cents'] = $periodCredit;
            $row['direct_net_cents'] = $prior + $periodDebit - $periodCredit;
            $row['children'] = [];
            $accounts[(int) $row['id']] = $row;
        }

        foreach ($accounts as $id => $row) {
            $parent = (int) ($row['parent_id'] ?? 0);
            if ($parent > 0 && isset($accounts[$parent]) && $accounts[$parent]['account_type'] === $row['account_type']) {
                $accounts[$parent]['children'][] = $id;
            }
        }

        $totals = [];
        $openingTotals = [];
        $visiting = [];
        $sum = function (int $id) use (&$sum, &$accounts, &$totals, &$openingTotals, &$visiting): array {
            if (isset($totals[$id])) {
                return [$totals[$id], $openingTotals[$id]];
            }
            if (isset($visiting[$id])) {
                throw new InvalidArgumentException('Il piano dei conti contiene una gerarchia circolare.');
            }
            $visiting[$id] = true;
            $total = (int) $accounts[$id]['direct_net_cents'];
            $openingTotal = (int) $accounts[$id]['opening_net_cents'];
            foreach ($accounts[$id]['children'] as $child) {
                [$childTotal, $childOpening] = $sum((int) $child);
                $total += $childTotal;
                $openingTotal += $childOpening;
            }
            unset($visiting[$id]);
            return [$totals[$id] = $total, $openingTotals[$id] = $openingTotal];
        };
        foreach (array_keys($accounts) as $id) {
            [$accounts[$id]['total_net_cents'], $accounts[$id]['opening_total_cents']] = $sum((int) $id);
        }

        $roots = ['ASSET' => [], 'LIABILITY' => [], 'EQUITY' => [], 'EXPENSE' => [], 'REVENUE' => []];
        foreach ($accounts as $id => $row) {
            $parent = (int) ($row['parent_id'] ?? 0);
            if ($parent <= 0 || !isset($accounts[$parent]) || $accounts[$parent]['account_type'] !== $row['account_type']) {
                $roots[$row['account_type']][] = $id;
            }
        }

        $search = mb_strtolower(trim($search));
        $matches = function (int $id) use (&$matches, $accounts, $search): bool {
            if ($search === '') {
                return true;
            }
            $row = $accounts[$id];
            if (str_contains(mb_strtolower($row['code'] . ' ' . $row['name']), $search)) {
                return true;
            }
            foreach ($row['children'] as $child) {
                if ($matches((int) $child)) {
                    return true;
                }
            }
            return false;
        };
        $flatten = function (int $id, int $level = 0) use (&$flatten, $accounts, $matches): array {
            $row = $accounts[$id];
            if (abs((int) $row['total_net_cents']) < 1 || !$matches($id)) {
                return [];
            }
            $result = [[
                'id' => (int) $row['id'], 'code' => $row['code'], 'name' => $row['name'],
                'account_type' => $row['account_type'], 'level' => $level,
                'is_postable' => (bool) $row['is_postable'],
                'net' => $row['total_net_cents'] / 100,
                'amount' => self::displayAmount((string) $row['account_type'], (int) $row['total_net_cents']),
                'opening' => $row['opening_total_cents'] / 100,
                'period_debit' => $row['period_debit_cents'] / 100,
                'period_credit' => $row['period_credit_cents'] / 100,
            ]];
            foreach ($row['children'] as $child) {
                $result = array_merge($result, $flatten((int) $child, $level + 1));
            }
            return $result;
        };

        $sections = [];
        foreach ($roots as $type => $ids) {
            $sections[$type] = [];
            if ($accountType !== '' && $accountType !== $type) {
                continue;
            }
            foreach ($ids as $id) {
                $sections[$type] = array_merge($sections[$type], $flatten((int) $id));
            }
        }

        $typeNet = [];
        foreach ($roots as $type => $ids) {
            $typeNet[$type] = array_sum(array_map(static fn (int $id): int => (int) $accounts[$id]['total_net_cents'], $ids));
        }
        $priorBalanceSheetNet = array_sum(array_map(
            static fn (array $row): int => in_array($row['account_type'], self::BALANCE_SHEET_TYPES, true)
                ? (int) $row['opening_net_cents'] : 0,
            $accounts,
        ));
        $openingAdjustment = !$hasPostedOpening ? -$priorBalanceSheetNet : 0;
        if ($openingAdjustment !== 0 && ($accountType === '' || $accountType === 'EQUITY')) {
            $sections['EQUITY'][] = [
                'id' => 0, 'code' => 'APERTURA', 'name' => 'Risultato esercizi precedenti da contabilizzare',
                'account_type' => 'EQUITY', 'level' => 0, 'is_postable' => false,
                'net' => $openingAdjustment / 100,
                'amount' => self::displayAmount('EQUITY', $openingAdjustment),
                'opening' => $openingAdjustment / 100, 'period_debit' => 0.0, 'period_credit' => 0.0,
            ];
            $typeNet['EQUITY'] += $openingAdjustment;
        }

        $assets = self::displayAmount('ASSET', $typeNet['ASSET']);
        $liabilities = self::displayAmount('LIABILITY', $typeNet['LIABILITY'])
            + self::displayAmount('EQUITY', $typeNet['EQUITY']);
        $expenses = self::displayAmount('EXPENSE', $typeNet['EXPENSE']);
        $revenues = self::displayAmount('REVENUE', $typeNet['REVENUE']);
        $profitLoss = round($revenues - $expenses, 2);

        $leafDebit = $leafCredit = 0;
        foreach ($accounts as $row) {
            $net = (int) $row['direct_net_cents'];
            if ($net > 0) $leafDebit += $net;
            elseif ($net < 0) $leafCredit += abs($net);
        }
        if ($openingAdjustment > 0) $leafDebit += $openingAdjustment;
        elseif ($openingAdjustment < 0) $leafCredit += abs($openingAdjustment);

        return [
            'sections' => $sections,
            'totals' => [
                'debit' => $leafDebit / 100, 'credit' => $leafCredit / 100,
                'assets' => $assets, 'liabilities' => $liabilities,
                'expenses' => $expenses, 'revenues' => $revenues,
                'profit_loss' => $profitLoss,
                'balance_assets' => $assets + max(0, -$profitLoss),
                'balance_liabilities' => $liabilities + max(0, $profitLoss),
            ],
            'opening' => [
                'posted' => $hasPostedOpening,
                'carried' => !$hasPostedOpening && $priorBalanceSheetNet !== 0,
                'adjustment' => $openingAdjustment / 100,
            ],
        ];
    }

    private static function cents(mixed $value): int
    {
        return (int) round((float) $value * 100);
    }

    private static function displayAmount(string $type, int $net): float
    {
        return (in_array($type, ['LIABILITY', 'EQUITY', 'REVENUE'], true) ? -$net : $net) / 100;
    }
}
