<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class ClosingService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function prepare(int $year): int
    {
        $this->validateYear($year);
        [$startsOn, $endsOn] = $this->period($year);
        $drafts = $this->db->prepare(
            "SELECT COUNT(*) FROM journal_entries WHERE organization_id = ? AND entry_date BETWEEN ? AND ? AND status = 'DRAFT'"
        );
        $drafts->execute([$this->organizationId, $startsOn, $endsOn]);
        $balances = $this->balances($year, ['REVENUE', 'EXPENSE']);
        $debit = round(array_sum(array_column($balances, 'debit')), 2);
        $credit = round(array_sum(array_column($balances, 'credit')), 2);
        $errors = [];
        if ((int) $drafts->fetchColumn() > 0) {
            $errors[] = 'Sono presenti registrazioni in bozza nell’esercizio.';
        }
        $trial = $this->db->prepare(
            "SELECT COALESCE(SUM(total_debit),0), COALESCE(SUM(total_credit),0)
             FROM journal_entries WHERE organization_id = ? AND entry_date BETWEEN ? AND ? AND status = 'POSTED'"
        );
        $trial->execute([$this->organizationId, $startsOn, $endsOn]);
        [$trialDebit, $trialCredit] = array_map('floatval', $trial->fetch(PDO::FETCH_NUM));
        if (abs($trialDebit - $trialCredit) > .005) {
            $errors[] = 'Il libro giornale non è quadrato.';
        }
        $profitLoss = round($credit - $debit, 2);
        $snapshot = [
            'year' => $year, 'profit_loss_accounts' => $balances, 'trial_debit' => $trialDebit,
            'trial_credit' => $trialCredit, 'profit_loss' => $profitLoss, 'prepared_at' => date(DATE_ATOM),
        ];
        $validation = ['valid' => $errors === [], 'errors' => $errors, 'checked_at' => date(DATE_ATOM)];
        $statement = $this->db->prepare(
            "INSERT INTO accounting_closing_runs
             (organization_id, fiscal_year, status, profit_loss_amount, snapshot_json, validation_json,
              created_by, created_at, updated_at)
             VALUES (?, ?, 'DRAFT', ?, ?, ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE profit_loss_amount = VALUES(profit_loss_amount), snapshot_json = VALUES(snapshot_json),
              validation_json = VALUES(validation_json), status = IF(status = 'POSTED', status, 'DRAFT'), updated_at = NOW()"
        );
        $statement->execute([
            $this->organizationId, $year, $profitLoss, $this->json($snapshot), $this->json($validation), $this->userId,
        ]);
        $find = $this->db->prepare('SELECT id FROM accounting_closing_runs WHERE organization_id = ? AND fiscal_year = ?');
        $find->execute([$this->organizationId, $year]);
        return (int) $find->fetchColumn();
    }

    public function post(int $runId): array
    {
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT * FROM accounting_closing_runs
                 WHERE id = ? AND organization_id = ? AND status IN ('DRAFT','VALIDATED') FOR UPDATE"
            );
            $statement->execute([$runId, $this->organizationId]);
            $run = $statement->fetch();
            if (!$run) {
                throw new InvalidArgumentException('Chiusura non trovata o già contabilizzata.');
            }
            $validation = json_decode((string) $run['validation_json'], true) ?: [];
            if (!($validation['valid'] ?? false)) {
                throw new RuntimeException('La chiusura contiene controlli bloccanti. Rigenera il prospetto dopo averli risolti.');
            }
            $year = (int) $run['fiscal_year'];
            [$startsOn, $endsOn, $openingDate] = $this->period($year);
            $balances = $this->balances($year, ['REVENUE', 'EXPENSE']);
            $lines = [];
            $totalDebit = 0.0;
            $totalCredit = 0.0;
            foreach ($balances as $balance) {
                $net = round((float) $balance['debit'] - (float) $balance['credit'], 2);
                if (abs($net) <= .005) {
                    continue;
                }
                if ($net > 0) {
                    $lines[] = ['account_id' => $balance['id'], 'debit' => 0, 'credit' => $net, 'description' => 'Chiusura ' . $balance['code']];
                    $totalCredit += $net;
                } else {
                    $lines[] = ['account_id' => $balance['id'], 'debit' => abs($net), 'credit' => 0, 'description' => 'Chiusura ' . $balance['code']];
                    $totalDebit += abs($net);
                }
            }
            if ($lines === []) {
                throw new RuntimeException('Nessun conto economico da chiudere.');
            }
            $profitLossAccount = $this->mapping('PROFIT_LOSS');
            $retainedAccount = $this->mapping('RETAINED_EARNINGS');
            $difference = round($totalDebit - $totalCredit, 2);
            if ($difference > 0) {
                $lines[] = ['account_id' => $profitLossAccount, 'debit' => 0, 'credit' => $difference, 'description' => 'Utile d’esercizio'];
                $lines[] = ['account_id' => $profitLossAccount, 'debit' => $difference, 'credit' => 0, 'description' => 'Destinazione utile'];
                $lines[] = ['account_id' => $retainedAccount, 'debit' => 0, 'credit' => $difference, 'description' => 'Utile portato a nuovo'];
            } elseif ($difference < 0) {
                $loss = abs($difference);
                $lines[] = ['account_id' => $profitLossAccount, 'debit' => $loss, 'credit' => 0, 'description' => 'Perdita d’esercizio'];
                $lines[] = ['account_id' => $retainedAccount, 'debit' => $loss, 'credit' => 0, 'description' => 'Perdita portata a nuovo'];
                $lines[] = ['account_id' => $profitLossAccount, 'debit' => 0, 'credit' => $loss, 'description' => 'Destinazione perdita'];
            }
            $service = new AccountingService($this->db, $this->organizationId, $this->userId);
            $closingEntryId = $service->postAutomated([
                'entry_date' => $endsOn, 'competence_date' => $endsOn,
                'entry_type' => 'CLOSING', 'description' => 'Chiusura esercizio ' . $year,
            ], $lines, 'CLOSING_RUN', $runId);

            $openingBalances = $this->balances($year, ['ASSET', 'LIABILITY', 'EQUITY']);
            $openingLines = [];
            $openingDebit = 0.0;
            $openingCredit = 0.0;
            foreach ($openingBalances as $balance) {
                $net = round((float) $balance['debit'] - (float) $balance['credit'], 2);
                if (abs($net) <= .005 || (int) $balance['id'] === $this->mapping('OPENING_BALANCE')) {
                    continue;
                }
                if ($net > 0) {
                    $openingLines[] = ['account_id' => $balance['id'], 'debit' => $net, 'credit' => 0, 'description' => 'Apertura ' . $balance['code']];
                    $openingDebit += $net;
                } else {
                    $openingLines[] = ['account_id' => $balance['id'], 'debit' => 0, 'credit' => abs($net), 'description' => 'Apertura ' . $balance['code']];
                    $openingCredit += abs($net);
                }
            }
            $openingDifference = round($openingDebit - $openingCredit, 2);
            if (abs($openingDifference) > .005) {
                $openingAccount = $this->mapping('OPENING_BALANCE');
                $openingLines[] = $openingDifference > 0
                    ? ['account_id' => $openingAccount, 'debit' => 0, 'credit' => $openingDifference, 'description' => 'Quadratura apertura']
                    : ['account_id' => $openingAccount, 'debit' => abs($openingDifference), 'credit' => 0, 'description' => 'Quadratura apertura'];
            }
            $openingEntryId = $service->postAutomated([
                'entry_date' => $openingDate, 'competence_date' => $openingDate,
                'entry_type' => 'OPENING', 'description' => 'Apertura esercizio ' . ($year + 1),
            ], $openingLines, 'OPENING_RUN', $runId);

            $this->db->prepare(
                "INSERT INTO fiscal_years (organization_id, year, starts_on, ends_on, status, closed_at, created_at, updated_at)
                 VALUES (?, ?, ?, ?, 'CLOSED', NOW(), NOW(), NOW())
                 ON DUPLICATE KEY UPDATE status = 'CLOSED', closed_at = NOW(), updated_at = NOW()"
            )->execute([$this->organizationId, $year, $startsOn, $endsOn]);
            [, $nextEndsOn] = $this->period($year + 1);
            $this->db->prepare(
                "INSERT INTO fiscal_years (organization_id, year, starts_on, ends_on, status, created_at, updated_at)
                 VALUES (?, ?, ?, ?, 'OPEN', NOW(), NOW())
                 ON DUPLICATE KEY UPDATE status = IF(status = 'CLOSED', status, 'OPEN'), updated_at = NOW()"
            )->execute([$this->organizationId, $year + 1, $openingDate, $nextEndsOn]);
            $this->db->prepare(
                "INSERT INTO accounting_period_locks (organization_id, scope, starts_on, ends_on, reason, locked_by, created_at)
                 VALUES (?, 'ACCOUNTING', ?, ?, ?, ?, NOW())"
            )->execute([$this->organizationId, $startsOn, $endsOn, 'Chiusura esercizio ' . $year, $this->userId]);
            $this->db->prepare(
                "UPDATE accounting_closing_runs SET status = 'POSTED', closing_journal_entry_id = ?, opening_journal_entry_id = ?,
                 posted_by = ?, posted_at = NOW(), updated_at = NOW() WHERE id = ? AND organization_id = ?"
            )->execute([$closingEntryId, $openingEntryId, $this->userId, $runId, $this->organizationId]);
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return ['closing_entry_id' => $closingEntryId, 'opening_entry_id' => $openingEntryId];
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function saveAdjustment(array $data, bool $post): int
    {
        $type = strtoupper((string) ($data['adjustment_type'] ?? 'OTHER'));
        $description = trim((string) ($data['description'] ?? ''));
        $amount = round($this->decimal($data['amount'] ?? 0), 2);
        $from = $this->date((string) ($data['competence_from'] ?? ''));
        $to = $this->date((string) ($data['competence_to'] ?? ''));
        $posting = $this->date((string) ($data['posting_date'] ?? ''));
        $sourceAccountId = (int) ($data['source_account_id'] ?? 0);
        $counterpartAccountId = (int) ($data['counterpart_account_id'] ?? 0);
        if (!in_array($type, ['ACCRUAL','DEFERRAL','PREPAID','DEFERRED_INCOME','OTHER'], true)
            || $description === '' || $amount <= 0 || $to < $from || $sourceAccountId <= 0 || $counterpartAccountId <= 0
            || $sourceAccountId === $counterpartAccountId) {
            throw new InvalidArgumentException('Dati dell’assestamento non validi.');
        }
        $statement = $this->db->prepare(
            "INSERT INTO accounting_adjustment_schedules
             (organization_id, adjustment_type, description, source_account_id, counterpart_account_id, amount,
              competence_from, competence_to, posting_date, reversal_date, status, created_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, NOW(), NOW())"
        );
        $statement->execute([
            $this->organizationId, $type, $description, $sourceAccountId, $counterpartAccountId, $amount,
            $from, $to, $posting, !empty($data['reversal_date']) ? $this->date((string) $data['reversal_date']) : null, $this->userId,
        ]);
        $id = (int) $this->db->lastInsertId();
        if ($post) {
            $this->postAdjustment($id);
        }
        return $id;
    }

    public function postAdjustment(int $scheduleId): int
    {
        $statement = $this->db->prepare(
            "SELECT * FROM accounting_adjustment_schedules
             WHERE id = ? AND organization_id = ? AND status = 'DRAFT'"
        );
        $statement->execute([$scheduleId, $this->organizationId]);
        $schedule = $statement->fetch();
        if (!$schedule) {
            throw new InvalidArgumentException('Assestamento non trovato o già contabilizzato.');
        }
        $debitSource = in_array($schedule['adjustment_type'], ['ACCRUAL','PREPAID'], true);
        $lines = $debitSource
            ? [
                ['account_id' => $schedule['source_account_id'], 'debit' => $schedule['amount'], 'credit' => 0, 'description' => $schedule['description']],
                ['account_id' => $schedule['counterpart_account_id'], 'debit' => 0, 'credit' => $schedule['amount'], 'description' => $schedule['description']],
            ]
            : [
                ['account_id' => $schedule['counterpart_account_id'], 'debit' => $schedule['amount'], 'credit' => 0, 'description' => $schedule['description']],
                ['account_id' => $schedule['source_account_id'], 'debit' => 0, 'credit' => $schedule['amount'], 'description' => $schedule['description']],
            ];
        $entryId = (new AccountingService($this->db, $this->organizationId, $this->userId))->postAutomated([
            'entry_date' => $schedule['posting_date'], 'competence_date' => $schedule['competence_to'],
            'entry_type' => 'ADJUSTMENT', 'description' => $schedule['description'],
        ], $lines, 'ADJUSTMENT_SCHEDULE', $scheduleId);
        $this->db->prepare(
            "UPDATE accounting_adjustment_schedules SET status = 'POSTED', journal_entry_id = ?, updated_at = NOW()
             WHERE id = ? AND organization_id = ?"
        )->execute([$entryId, $scheduleId, $this->organizationId]);
        return $entryId;
    }

    private function balances(int $year, array $types): array
    {
        [$startsOn, $endsOn] = $this->period($year);
        $placeholders = implode(',', array_fill(0, count($types), '?'));
        $statement = $this->db->prepare(
            "SELECT a.id, a.code, a.name, a.account_type, a.statement_section,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit ELSE 0 END),0) AS debit,
                    COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.credit ELSE 0 END),0) AS credit
             FROM chart_of_accounts a
             LEFT JOIN journal_entry_lines l ON l.account_id = a.id AND l.organization_id = a.organization_id
             LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
             WHERE a.organization_id = ? AND a.account_type IN ({$placeholders})
             GROUP BY a.id, a.code, a.name, a.account_type, a.statement_section
             HAVING debit <> 0 OR credit <> 0 ORDER BY a.code"
        );
        $statement->execute(array_merge([$startsOn, $endsOn, $this->organizationId], $types));
        return $statement->fetchAll();
    }

    private function period(int $year): array
    {
        $statement = $this->db->prepare('SELECT fiscal_year_start_month FROM accounting_settings WHERE organization_id = ?');
        $statement->execute([$this->organizationId]);
        $month = min(12, max(1, (int) ($statement->fetchColumn() ?: 1)));
        $start = new DateTimeImmutable(sprintf('%04d-%02d-01', $year, $month));
        $opening = $start->modify('+1 year');
        $end = $opening->modify('-1 day');
        return [$start->format('Y-m-d'), $end->format('Y-m-d'), $opening->format('Y-m-d')];
    }

    private function mapping(string $key): int
    {
        $statement = $this->db->prepare(
            'SELECT account_id FROM accounting_account_mappings WHERE organization_id = ? AND mapping_key = ?'
        );
        $statement->execute([$this->organizationId, $key]);
        $id = (int) $statement->fetchColumn();
        return $id > 0 ? $id : throw new RuntimeException('Manca il conto automatico “' . $key . '”.');
    }

    private function validateYear(int $year): void
    {
        if ($year < 2000 || $year > 2200) {
            throw new InvalidArgumentException('Esercizio non valido.');
        }
    }

    private function date(string $value): string
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
        $errors = DateTimeImmutable::getLastErrors();
        if ($date === false || ($errors !== false && ($errors['warning_count'] > 0 || $errors['error_count'] > 0))) {
            throw new InvalidArgumentException('Data non valida.');
        }
        return $date->format('Y-m-d');
    }

    private function decimal(mixed $value): float
    {
        $value = trim((string) $value);
        if (str_contains($value, ',')) {
            $value = str_replace('.', '', $value);
            $value = str_replace(',', '.', $value);
        }
        return (float) $value;
    }

    private function json(array $value): string
    {
        return json_encode($value, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    }
}
