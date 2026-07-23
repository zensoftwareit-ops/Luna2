<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use SimpleXMLElement;
use Throwable;

final class BankStatementService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
        private readonly string $basePath,
    ) {
    }

    public function import(string $temporaryPath, string $originalName, int $bankAccountId, string $format): array
    {
        $format = strtoupper(trim($format));
        if (!in_array($format, ['CSV', 'CAMT053', 'MT940'], true) || !is_file($temporaryPath)) {
            throw new InvalidArgumentException('File o formato dell’estratto conto non valido.');
        }
        $account = $this->db->prepare('SELECT id FROM bank_accounts WHERE id = ? AND organization_id = ? AND active = 1');
        $account->execute([$bankAccountId, $this->organizationId]);
        if (!$account->fetchColumn()) {
            throw new InvalidArgumentException('Conto bancario non valido.');
        }

        $checksum = hash_file('sha256', $temporaryPath);
        if ($checksum === false) {
            throw new RuntimeException('Impossibile calcolare il checksum del file.');
        }
        $safeName = preg_replace('/[^a-zA-Z0-9._-]+/', '-', basename($originalName)) ?: 'estratto-conto';
        $directory = $this->basePath . '/storage/private/bank-statements/' . $this->organizationId . '/' . $bankAccountId;
        if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
            throw new RuntimeException('Impossibile creare l’archivio privato degli estratti conto.');
        }
        $storedPath = $directory . '/' . date('Ymd-His') . '-' . bin2hex(random_bytes(4))
            . '-' . substr($checksum, 0, 12) . '-' . $safeName;
        if (!copy($temporaryPath, $storedPath)) {
            throw new RuntimeException('Impossibile archiviare il file caricato.');
        }

        try {
            $statement = $this->db->prepare(
                "INSERT INTO bank_statement_imports
                 (organization_id, bank_account_id, source_format, original_filename, stored_path, checksum_sha256,
                  status, imported_by, created_at)
                 VALUES (?, ?, ?, ?, ?, ?, 'STAGED', ?, NOW())"
            );
            $statement->execute([
                $this->organizationId, $bankAccountId, $format, mb_substr($originalName, 0, 255),
                $this->relative($storedPath), $checksum, $this->userId,
            ]);
        } catch (Throwable $exception) {
            @unlink($storedPath);
            if (str_contains(strtolower($exception->getMessage()), 'duplicate')) {
                throw new InvalidArgumentException('Questo estratto conto è già stato acquisito.');
            }
            throw $exception;
        }
        $batchId = (int) $this->db->lastInsertId();

        $errors = [];
        $imported = 0;
        $duplicates = 0;
        try {
            $rows = match ($format) {
                'CSV' => $this->parseCsv($storedPath),
                'CAMT053' => $this->parseCamt($storedPath),
                'MT940' => $this->parseMt940($storedPath),
            };
            $banking = new BankingService($this->db, $this->organizationId, $this->userId);
            foreach ($rows as $index => $row) {
                try {
                    $row['bank_account_id'] = $bankAccountId;
                    $row['bank_statement_import_id'] = $batchId;
                    $banking->saveTransaction($row);
                    $imported++;
                } catch (InvalidArgumentException $exception) {
                    if (str_contains(mb_strtolower($exception->getMessage()), 'già importato')) {
                        $duplicates++;
                    } else {
                        $errors[] = ['row' => $index + 1, 'message' => $exception->getMessage()];
                    }
                } catch (Throwable $exception) {
                    $errors[] = ['row' => $index + 1, 'message' => $exception->getMessage()];
                }
            }
            $status = $errors === [] ? 'IMPORTED' : ($imported > 0 ? 'PARTIAL' : 'FAILED');
            $this->db->prepare(
                'UPDATE bank_statement_imports
                 SET status = ?, rows_count = ?, imported_count = ?, duplicate_count = ?, error_count = ?,
                     error_json = ?, imported_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([
                $status, count($rows), $imported, $duplicates, count($errors),
                $errors === [] ? null : json_encode(array_slice($errors, 0, 100), JSON_UNESCAPED_UNICODE),
                $batchId, $this->organizationId,
            ]);
            $suggestions = $this->generateSuggestions($bankAccountId);
            return compact('batchId', 'status', 'imported', 'duplicates', 'errors', 'suggestions') + ['rows' => count($rows)];
        } catch (Throwable $exception) {
            $this->db->prepare(
                "UPDATE bank_statement_imports SET status = 'FAILED', error_count = 1, error_json = ? WHERE id = ? AND organization_id = ?"
            )->execute([
                json_encode([['row' => 0, 'message' => $exception->getMessage()]], JSON_UNESCAPED_UNICODE),
                $batchId, $this->organizationId,
            ]);
            throw $exception;
        }
    }

    public function imports(): array
    {
        $statement = $this->db->prepare(
            'SELECT i.*, b.name AS bank_name
             FROM bank_statement_imports i
             INNER JOIN bank_accounts b ON b.id = i.bank_account_id AND b.organization_id = i.organization_id
             WHERE i.organization_id = ? ORDER BY i.id DESC LIMIT 50'
        );
        $statement->execute([$this->organizationId]);
        return $statement->fetchAll();
    }

    public function suggestions(): array
    {
        $statement = $this->db->prepare(
            "SELECT s.*, t.booking_date, t.amount AS transaction_amount, t.description AS transaction_description,
                    t.counterparty AS transaction_counterparty, p.payment_date, p.amount AS payment_amount,
                    p.reference_number, COALESCE(c.business_name, f.business_name) AS party_name
             FROM bank_reconciliation_suggestions s
             INNER JOIN bank_transactions t ON t.id = s.bank_transaction_id
             LEFT JOIN payments p ON p.id = s.payment_id
             LEFT JOIN customers c ON c.id = p.customer_id
             LEFT JOIN suppliers f ON f.id = p.supplier_id
             WHERE s.organization_id = ? AND s.status = 'PROPOSED'
             ORDER BY s.confidence_score DESC, t.booking_date DESC LIMIT 100"
        );
        $statement->execute([$this->organizationId]);
        return $statement->fetchAll();
    }

    public function generateSuggestions(?int $bankAccountId = null): int
    {
        $sql = "SELECT * FROM bank_transactions
                WHERE organization_id = ? AND reconciliation_status = 'UNMATCHED'";
        $params = [$this->organizationId];
        if ($bankAccountId !== null) {
            $sql .= ' AND bank_account_id = ?';
            $params[] = $bankAccountId;
        }
        $sql .= ' ORDER BY booking_date DESC LIMIT 500';
        $transactions = $this->db->prepare($sql);
        $transactions->execute($params);

        $payments = $this->db->prepare(
            "SELECT p.*, COALESCE(c.business_name, f.business_name, p.description, '') AS party_name
             FROM payments p
             LEFT JOIN customers c ON c.id = p.customer_id AND c.organization_id = p.organization_id
             LEFT JOIN suppliers f ON f.id = p.supplier_id AND f.organization_id = p.organization_id
             WHERE p.organization_id = ? AND p.status = 'POSTED' AND p.reconciled = 0
             ORDER BY p.payment_date DESC LIMIT 1000"
        );
        $payments->execute([$this->organizationId]);
        $paymentRows = $payments->fetchAll();
        $upsert = $this->db->prepare(
            "INSERT INTO bank_reconciliation_suggestions
             (organization_id, bank_transaction_id, payment_id, confidence_score, reason_json, status, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, 'PROPOSED', NOW(), NOW())
             ON DUPLICATE KEY UPDATE confidence_score = VALUES(confidence_score), reason_json = VALUES(reason_json),
                 status = IF(status = 'ACCEPTED', status, 'PROPOSED'), updated_at = NOW()"
        );

        $written = 0;
        foreach ($transactions->fetchAll() as $transaction) {
            $candidates = [];
            foreach ($paymentRows as $payment) {
                if (!empty($payment['bank_account_id']) && (int) $payment['bank_account_id'] !== (int) $transaction['bank_account_id']) {
                    continue;
                }
                $expectedPositive = $payment['payment_type'] === 'RECEIPT';
                if ($expectedPositive !== ((float) $transaction['amount'] > 0)) {
                    continue;
                }
                $targetAmount = abs((float) ($payment['bank_amount'] ?: $payment['amount']));
                $difference = abs(abs((float) $transaction['amount']) - $targetAmount);
                if ($difference > max(2.0, $targetAmount * .02)) {
                    continue;
                }
                $score = $difference < .005 ? 60.0 : max(20.0, 55.0 - ($difference / max($targetAmount, 1) * 100));
                $reasons = ['amount' => $difference < .005 ? 'Importo esatto' : 'Importo vicino'];
                $days = abs((int) ((new DateTimeImmutable((string) $transaction['booking_date']))
                    ->diff(new DateTimeImmutable((string) $payment['payment_date']))->format('%r%a')));
                if ($days <= 2) {
                    $score += 20;
                    $reasons['date'] = 'Data entro 2 giorni';
                } elseif ($days <= 7) {
                    $score += 10;
                    $reasons['date'] = 'Data entro 7 giorni';
                } elseif ($days > 45) {
                    continue;
                }
                $haystack = $this->normalizeText(implode(' ', [
                    $transaction['description'] ?? '', $transaction['counterparty'] ?? '', $transaction['reference'] ?? '',
                ]));
                $needles = array_filter([
                    $this->normalizeText((string) ($payment['party_name'] ?? '')),
                    $this->normalizeText((string) ($payment['reference_number'] ?? '')),
                ], static fn (string $value): bool => mb_strlen($value) >= 4);
                foreach ($needles as $needle) {
                    if (str_contains($haystack, $needle)) {
                        $score += 20;
                        $reasons['text'] = 'Controparte o riferimento riconosciuto';
                        break;
                    }
                }
                if ($score >= 60) {
                    $candidates[] = ['payment' => $payment, 'score' => min(100, round($score, 2)), 'reasons' => $reasons];
                }
            }
            usort($candidates, static fn (array $a, array $b): int => $b['score'] <=> $a['score']);
            foreach (array_slice($candidates, 0, 3) as $candidate) {
                $upsert->execute([
                    $this->organizationId, $transaction['id'], $candidate['payment']['id'], $candidate['score'],
                    json_encode($candidate['reasons'], JSON_UNESCAPED_UNICODE),
                ]);
                $written++;
            }
        }
        return $written;
    }

    public function reviewSuggestion(int $suggestionId, bool $accept): void
    {
        $statement = $this->db->prepare(
            "SELECT * FROM bank_reconciliation_suggestions
             WHERE id = ? AND organization_id = ? AND status = 'PROPOSED' FOR UPDATE"
        );
        $this->db->beginTransaction();
        try {
            $statement->execute([$suggestionId, $this->organizationId]);
            $suggestion = $statement->fetch();
            if (!$suggestion) {
                throw new InvalidArgumentException('Suggerimento non disponibile.');
            }
            if ($accept && !empty($suggestion['payment_id'])) {
                $transaction = $this->db->prepare('SELECT ABS(amount) - reconciled_amount FROM bank_transactions WHERE id = ? AND organization_id = ?');
                $transaction->execute([$suggestion['bank_transaction_id'], $this->organizationId]);
                $amount = (float) $transaction->fetchColumn();
                (new BankingService($this->db, $this->organizationId, $this->userId))->reconcile(
                    (int) $suggestion['bank_transaction_id'], (int) $suggestion['payment_id'], $amount,
                );
            }
            $this->db->prepare(
                'UPDATE bank_reconciliation_suggestions SET status = ?, reviewed_by = ?, reviewed_at = NOW(), updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([$accept ? 'ACCEPTED' : 'REJECTED', $this->userId, $suggestionId, $this->organizationId]);
            if ($accept) {
                $this->db->prepare(
                    "UPDATE bank_reconciliation_suggestions SET status = 'EXPIRED', updated_at = NOW()
                     WHERE organization_id = ? AND bank_transaction_id = ? AND id <> ? AND status = 'PROPOSED'"
                )->execute([$this->organizationId, $suggestion['bank_transaction_id'], $suggestionId]);
            }
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function parseCsv(string $path): array
    {
        $handle = fopen($path, 'rb');
        if ($handle === false) {
            throw new RuntimeException('CSV non leggibile.');
        }
        $first = fgets($handle);
        if ($first === false) {
            fclose($handle);
            return [];
        }
        $delimiter = $this->detectDelimiter($first);
        rewind($handle);
        $headers = fgetcsv($handle, 0, $delimiter);
        if (!$headers) {
            fclose($handle);
            throw new InvalidArgumentException('Intestazione CSV mancante.');
        }
        $headers = array_map([$this, 'normalizeHeader'], $headers);
        $rows = [];
        while (($values = fgetcsv($handle, 0, $delimiter)) !== false) {
            if (count(array_filter($values, static fn ($value): bool => trim((string) $value) !== '')) === 0) {
                continue;
            }
            $row = [];
            foreach ($headers as $index => $header) {
                $row[$header] = $values[$index] ?? null;
            }
            $amount = $this->pick($row, ['importo', 'amount', 'valore']);
            if ($amount === null) {
                $credit = $this->decimal($this->pick($row, ['avere', 'credito', 'credit']) ?? 0);
                $debit = $this->decimal($this->pick($row, ['dare', 'debito', 'debit']) ?? 0);
                $amount = (string) ($credit - $debit);
            }
            $rows[] = [
                'booking_date' => $this->dateCandidate($this->pick($row, ['data_contabile', 'data', 'booking_date', 'data_operazione'])),
                'value_date' => $this->dateCandidate($this->pick($row, ['data_valuta', 'value_date']), true),
                'amount' => $this->decimal($amount),
                'description' => (string) ($this->pick($row, ['descrizione', 'causale', 'description', 'narrativa']) ?? 'Movimento bancario'),
                'counterparty' => $this->pick($row, ['controparte', 'beneficiario', 'ordinante', 'counterparty']),
                'reference' => $this->pick($row, ['riferimento', 'cro', 'trn', 'reference']),
                'external_id' => $this->pick($row, ['id', 'identificativo', 'external_id']),
            ];
        }
        fclose($handle);
        return $rows;
    }

    private function parseCamt(string $path): array
    {
        $xml = simplexml_load_file($path, SimpleXMLElement::class, LIBXML_NONET | LIBXML_NOBLANKS);
        if (!$xml) {
            throw new InvalidArgumentException('File CAMT.053 non valido.');
        }
        $entries = $xml->xpath('//*[local-name()="Ntry"]') ?: [];
        $rows = [];
        foreach ($entries as $entry) {
            $amountNode = $entry->xpath('./*[local-name()="Amt"]')[0] ?? null;
            $amount = $amountNode ? $this->decimal((string) $amountNode) : 0.0;
            $direction = (string) (($entry->xpath('./*[local-name()="CdtDbtInd"]')[0] ?? null));
            if ($direction === 'DBIT') {
                $amount *= -1;
            }
            $rows[] = [
                'booking_date' => $this->dateCandidate($this->xpathText($entry, './*[local-name()="BookgDt"]/*[local-name()="Dt"]')),
                'value_date' => $this->dateCandidate($this->xpathText($entry, './*[local-name()="ValDt"]/*[local-name()="Dt"]'), true),
                'amount' => $amount,
                'description' => $this->firstNonEmpty([
                    $this->xpathText($entry, './/*[local-name()="RmtInf"]/*[local-name()="Ustrd"]'),
                    $this->xpathText($entry, './*[local-name()="AddtlNtryInf"]'),
                    'Movimento CAMT.053',
                ]),
                'counterparty' => $this->firstNonEmpty([
                    $this->xpathText($entry, './/*[local-name()="RltdPties"]//*[local-name()="Nm"]'),
                    null,
                ]),
                'reference' => $this->firstNonEmpty([
                    $this->xpathText($entry, './/*[local-name()="EndToEndId"]'),
                    $this->xpathText($entry, './/*[local-name()="AcctSvcrRef"]'),
                    null,
                ]),
                'external_id' => $this->firstNonEmpty([
                    $this->xpathText($entry, './*[local-name()="NtryRef"]'),
                    $this->xpathText($entry, './/*[local-name()="AcctSvcrRef"]'),
                    null,
                ]),
            ];
        }
        return $rows;
    }

    private function parseMt940(string $path): array
    {
        $content = file_get_contents($path);
        if ($content === false) {
            throw new RuntimeException('File MT940 non leggibile.');
        }
        $content = str_replace(["\r\n", "\r"], "\n", $content);
        preg_match_all('/^:61:(.+?)(?=^:61:|^:62[FM]:|\z)/ms', $content, $matches);
        $rows = [];
        foreach ($matches[1] ?? [] as $block) {
            $lines = explode("\n", trim($block));
            $line61 = array_shift($lines) ?? '';
            if (!preg_match('/^(\d{6})(\d{4})?([CD])([0-9,]+)(.*)$/', $line61, $match)) {
                continue;
            }
            $year = (int) substr($match[1], 0, 2);
            $year += $year >= 70 ? 1900 : 2000;
            $date = sprintf('%04d-%s-%s', $year, substr($match[1], 2, 2), substr($match[1], 4, 2));
            $amount = $this->decimal($match[4]) * ($match[3] === 'D' ? -1 : 1);
            $details = implode(' ', $lines);
            $description = preg_replace('/^:86:/', '', $details) ?: 'Movimento MT940';
            preg_match('/(?:NTRF|EREF\+|KREF\+)([A-Za-z0-9\/._-]+)/', $description, $reference);
            $rows[] = [
                'booking_date' => $this->dateCandidate($date),
                'value_date' => null,
                'amount' => $amount,
                'description' => trim($description),
                'counterparty' => null,
                'reference' => $reference[1] ?? null,
                'external_id' => hash('sha256', $line61 . '|' . $description),
            ];
        }
        return $rows;
    }

    private function detectDelimiter(string $line): string
    {
        $scores = [';' => substr_count($line, ';'), ',' => substr_count($line, ','), "\t" => substr_count($line, "\t")];
        arsort($scores);
        return (string) array_key_first($scores);
    }

    private function normalizeHeader(string $header): string
    {
        $header = preg_replace('/^\xEF\xBB\xBF/', '', trim($header)) ?? trim($header);
        $ascii = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $header);
        $header = mb_strtolower((string) ($ascii !== false ? $ascii : $header));
        return trim((string) preg_replace('/[^a-z0-9]+/', '_', $header), '_');
    }

    private function pick(array $row, array $keys): mixed
    {
        foreach ($keys as $key) {
            if (array_key_exists($key, $row) && trim((string) $row[$key]) !== '') {
                return $row[$key];
            }
        }
        return null;
    }

    private function date(string $value): string
    {
        $value = trim($value);
        foreach (['!Y-m-d', '!d/m/Y', '!d-m-Y', '!Ymd'] as $format) {
            $date = DateTimeImmutable::createFromFormat($format, $value);
            $errors = DateTimeImmutable::getLastErrors();
            if ($date !== false && ($errors === false || ($errors['warning_count'] === 0 && $errors['error_count'] === 0))) {
                return $date->format('Y-m-d');
            }
        }
        throw new InvalidArgumentException('Data bancaria non valida: ' . mb_substr($value, 0, 30));
    }

    private function dateCandidate(mixed $value, bool $nullable = false): ?string
    {
        $value = trim((string) $value);
        if ($value === '') {
            return $nullable ? null : '';
        }
        try {
            return $this->date($value);
        } catch (InvalidArgumentException) {
            // La validazione riga per riga avviene in BankingService, senza perdere l'intero file.
            return mb_substr($value, 0, 30);
        }
    }

    private function decimal(mixed $value): float
    {
        $value = trim((string) $value);
        if (str_contains($value, ',')) {
            $value = str_replace('.', '', $value);
            $value = str_replace(',', '.', $value);
        }
        return round((float) preg_replace('/[^0-9.\-+]/', '', $value), 2);
    }

    private function xpathText(SimpleXMLElement $node, string $path): ?string
    {
        $result = $node->xpath($path);
        $value = isset($result[0]) ? trim((string) $result[0]) : '';
        return $value === '' ? null : $value;
    }

    private function firstNonEmpty(array $values): ?string
    {
        foreach ($values as $value) {
            if ($value !== null && trim((string) $value) !== '') {
                return trim((string) $value);
            }
        }
        return null;
    }

    private function normalizeText(string $value): string
    {
        $ascii = iconv('UTF-8', 'ASCII//TRANSLIT//IGNORE', $value);
        return trim((string) preg_replace('/[^a-z0-9]+/', ' ', mb_strtolower((string) ($ascii !== false ? $ascii : $value))));
    }

    private function relative(string $path): string
    {
        return str_replace('\\', '/', ltrim(str_replace($this->basePath, '', $path), '/\\'));
    }
}
