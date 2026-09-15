<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;

final class LedgerReportService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId) {}

    public function dataset(int $id, string $from, string $to, string $search = ''): array
    {
        foreach ([$from, $to] as $value) {
            $date = \DateTimeImmutable::createFromFormat('!Y-m-d', $value);
            if (!$date || $date->format('Y-m-d') !== $value) {
                throw new InvalidArgumentException('Data del mastrino non valida.');
            }
        }
        if ($from > $to) { throw new InvalidArgumentException('Intervallo del mastrino non valido.'); }
        $statement = $this->db->prepare('SELECT id, code, name FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
        $statement->execute([$id, $this->organizationId]);
        $account = $statement->fetch();
        if (!$account) { throw new InvalidArgumentException('Conto non trovato.'); }
        $statement = $this->db->prepare(
            "SELECT e.id AS entry_id, e.entry_date, e.protocol_number, e.description AS entry_description,
                    e.document_number, e.counterparty, l.description, l.debit, l.credit
             FROM journal_entry_lines l JOIN journal_entries e
               ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
             WHERE l.organization_id = ? AND l.account_id = ? AND e.status = 'POSTED'
               AND e.entry_date <= ? ORDER BY e.entry_date, e.id, l.line_number, l.id"
        );
        $statement->execute([$this->organizationId, $id, $to]);
        return self::calculate($account, $statement->fetchAll(), $from, $to, $search);
    }

    // Work in cents; search never changes the accounting balance.
    public static function calculate(array $account, array $lines, string $from, string $to, string $search = ''): array
    {
        $balance = $opening = $debit = $credit = 0;
        $rows = [];
        foreach ($lines as $line) {
            if ($line['entry_date'] > $to) { continue; }
            $d = (int) round((float) $line['debit'] * 100);
            $c = (int) round((float) $line['credit'] * 100);
            $balance += $d - $c;
            if ($line['entry_date'] < $from) { $opening = $balance; continue; }
            $debit += $d; $credit += $c;
            $line['running_balance'] = $balance / 100;
            $text = implode(' ', array_map(static fn ($key): string => (string) ($line[$key] ?? ''),
                ['protocol_number', 'entry_description', 'description', 'document_number', 'counterparty']));
            if ($search === '' || mb_stripos($text, $search) !== false) { $rows[] = $line; }
        }
        $account += ['opening_balance' => $opening / 100, 'closing_balance' => $balance / 100,
            'period_debit' => $debit / 100, 'period_credit' => $credit / 100];
        return [$account, $rows, $from, $to, $search];
    }
}
