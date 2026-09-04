<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use Dompdf\Dompdf;
use Dompdf\Options;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class OfficialPrintService
{
    private const TYPES = [
        'JOURNAL' => 'Libro giornale',
        'TRIAL_BALANCE' => 'Bilancio di verifica',
        'VAT_SALES' => 'Registro IVA vendite',
        'VAT_PURCHASES' => 'Registro IVA acquisti',
        'VAT_CORRISPETTIVI' => 'Registro corrispettivi',
        'VAT_REVERSE_CHARGE' => 'Registro IVA reverse charge',
        'VAT_SELF_INVOICES' => 'Registro IVA autofatture',
        'VAT_LIQUIDATION' => 'Liquidazione IVA per articolo e aliquota',
        'ASSET_REGISTER' => 'Registro cespiti',
    ];

    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
        private readonly string $basePath,
    ) {
    }

    public static function types(): array
    {
        return self::TYPES;
    }

    public function all(): array
    {
        $statement = $this->db->prepare(
            'SELECT p.*, u.name AS generated_by_name, v.name AS validated_by_name
             FROM official_print_runs p
             LEFT JOIN users u ON u.id = p.generated_by
             LEFT JOIN users v ON v.id = p.validated_by
             WHERE p.organization_id = ? ORDER BY p.id DESC LIMIT 100'
        );
        $statement->execute([$this->organizationId]);
        return $statement->fetchAll();
    }

    public function generate(string $type, string $from, string $to, ?string $notes): int
    {
        if (!isset(self::TYPES[$type])) {
            throw new InvalidArgumentException('Tipo di stampa non valido.');
        }
        [$from, $to] = $this->period($from, $to);
        [$columns, $rows] = $this->dataset($type, $from, $to);
        $organization = $this->db->prepare('SELECT * FROM organizations WHERE id = ?');
        $organization->execute([$this->organizationId]);
        $company = $organization->fetch();
        if (!$company) {
            throw new RuntimeException('Azienda non trovata.');
        }

        $this->db->beginTransaction();
        try {
            $sequence = $this->nextSequence($type);
            $title = self::TYPES[$type] . ' · ' . (new DateTimeImmutable($from))->format('d/m/Y')
                . ' – ' . (new DateTimeImmutable($to))->format('d/m/Y');
            $statement = $this->db->prepare(
                "INSERT INTO official_print_runs
                 (organization_id, print_type, sequence_number, title, period_start, period_end, status,
                  row_count, generated_by, notes, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?, ?, NOW(), NOW())"
            );
            $statement->execute([
                $this->organizationId, $type, $sequence, $title, $from, $to, count($rows), $this->userId,
                $this->nullable($notes),
            ]);
            $id = (int) $this->db->lastInsertId();
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }

        try {
            $html = $this->html($company, $type, $sequence, $from, $to, $columns, $rows);
            $options = new Options();
            $options->set('isRemoteEnabled', false);
            $options->set('isPhpEnabled', false);
            $options->set('defaultFont', 'DejaVu Sans');
            $dompdf = new Dompdf($options);
            $dompdf->loadHtml($html, 'UTF-8');
            $dompdf->setPaper('A4', count($columns) > 6 ? 'landscape' : 'portrait');
            $dompdf->render();
            $directory = $this->basePath . '/storage/private/official-prints/' . $this->organizationId;
            if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
                throw new RuntimeException('Impossibile creare l’archivio delle stampe.');
            }
            $filename = strtolower($type) . '-' . str_pad((string) $sequence, 6, '0', STR_PAD_LEFT) . '.pdf';
            $path = $directory . '/' . $filename;
            $pdf = $dompdf->output();
            if (file_put_contents($path, $pdf, LOCK_EX) === false) {
                throw new RuntimeException('Impossibile salvare il PDF.');
            }
            $hash = hash_file('sha256', $path);
            $this->db->prepare(
                "UPDATE official_print_runs SET status = 'GENERATED', file_path = ?, file_sha256 = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?"
            )->execute([$this->relative($path), $hash, $id, $this->organizationId]);
            return $id;
        } catch (Throwable $exception) {
            $this->db->prepare(
                "UPDATE official_print_runs SET status = 'CANCELLED', notes = CONCAT(COALESCE(notes,''), ?) WHERE id = ? AND organization_id = ?"
            )->execute(["\nErrore generazione: " . mb_substr($exception->getMessage(), 0, 300), $id, $this->organizationId]);
            throw $exception;
        }
    }

    public function validate(int $id, string $reference): void
    {
        $reference = trim($reference);
        if ($reference === '') {
            throw new InvalidArgumentException('Indicare il riferimento della validazione professionale.');
        }
        $statement = $this->db->prepare(
            "UPDATE official_print_runs
             SET status = 'VALIDATED', professional_validation_reference = ?, validated_by = ?, validated_at = NOW(), updated_at = NOW()
             WHERE id = ? AND organization_id = ? AND status = 'GENERATED'"
        );
        $statement->execute([mb_substr($reference, 0, 190), $this->userId, $id, $this->organizationId]);
        if ($statement->rowCount() !== 1) {
            throw new InvalidArgumentException('La stampa non è validabile nello stato attuale.');
        }
    }

    public function lock(int $id): void
    {
        $statement = $this->db->prepare(
            "UPDATE official_print_runs SET status = 'LOCKED', locked_at = NOW(), updated_at = NOW()
             WHERE id = ? AND organization_id = ? AND status = 'VALIDATED' AND file_sha256 IS NOT NULL"
        );
        $statement->execute([$id, $this->organizationId]);
        if ($statement->rowCount() !== 1) {
            throw new InvalidArgumentException('Validare la stampa prima di bloccarla.');
        }
    }

    public function file(int $id): array
    {
        $statement = $this->db->prepare('SELECT * FROM official_print_runs WHERE id = ? AND organization_id = ?');
        $statement->execute([$id, $this->organizationId]);
        $row = $statement->fetch();
        if (!$row || empty($row['file_path'])) {
            throw new InvalidArgumentException('Stampa non disponibile.');
        }
        $path = $this->basePath . '/' . ltrim((string) $row['file_path'], '/');
        if (!is_file($path) || hash_file('sha256', $path) !== $row['file_sha256']) {
            throw new RuntimeException('Il file non supera il controllo di integrità.');
        }
        return [$path, $row];
    }

    private function dataset(string $type, string $from, string $to): array
    {
        if ($type === 'JOURNAL') {
            return $this->query(
                ['Data', 'Protocollo', 'Conto', 'Descrizione', 'Dare', 'Avere'],
                "SELECT e.entry_date, e.protocol_number, CONCAT(a.code, ' · ', a.name), COALESCE(l.description, e.description),
                        l.debit, l.credit
                 FROM journal_entries e
                 INNER JOIN journal_entry_lines l ON l.journal_entry_id = e.id AND l.organization_id = e.organization_id
                 INNER JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = e.organization_id
                 WHERE e.organization_id = ? AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
                 ORDER BY e.entry_date, e.protocol_number, l.line_number",
                [$this->organizationId, $from, $to],
            );
        }
        if ($type === 'TRIAL_BALANCE') {
            [$columns, $rows] = $this->query(
                ['Codice', 'Conto', 'Saldo Dare', 'Saldo Avere'],
                "SELECT a.code, a.name,
                        GREATEST(COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit-l.credit ELSE 0 END),0), 0),
                        GREATEST(COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.credit-l.debit ELSE 0 END),0), 0)
                 FROM chart_of_accounts a
                 LEFT JOIN journal_entry_lines l ON l.account_id = a.id AND l.organization_id = a.organization_id
                 LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
                 WHERE a.organization_id = ?
                 GROUP BY a.id, a.code, a.name
                 HAVING ABS(COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit-l.credit ELSE 0 END),0)) > 0.005
                 ORDER BY a.code",
                [$from, $to, $this->organizationId],
            );
            $debit = round((float) array_sum(array_column($rows, 2)), 2);
            $credit = round((float) array_sum(array_column($rows, 3)), 2);
            $difference = round($debit - $credit, 2);
            $rows[] = ['', 'TOTALI SALDI', $debit, $credit];
            $rows[] = ['', 'DIFFERENZA DARE / AVERE', $difference > 0 ? $difference : 0, $difference < 0 ? abs($difference) : 0];
            return [$columns, $rows];
        }
        if ($type === 'VAT_LIQUIDATION') {
            $columns = ['Periodo', 'Registro', 'Codice IVA', 'Articolo IVA', 'Aliquota %', 'Natura', 'Imponibile', 'IVA a debito', 'IVA detraibile', 'Credito precedente', 'Interessi', 'Saldo'];
            $rows = [];
            $settlements = $this->db->prepare(
                "SELECT * FROM vat_settlements
                 WHERE organization_id = ? AND period_year BETWEEN ? AND ? AND status IN ('CALCULATED','SUBMITTED','PAID','OVERDUE')
                 ORDER BY period_year, period_type, period_number"
            );
            $settlements->execute([$this->organizationId, (int) substr($from, 0, 4), (int) substr($to, 0, 4)]);
            $details = $this->db->prepare(
                "SELECT d.register_type, d.vat_code, COALESCE(c.description, d.vat_code), COALESCE(c.rate, 0),
                        COALESCE(c.nature, ''), d.taxable_amount, d.vat_amount, d.deductible_vat
                 FROM vat_settlement_details d
                 LEFT JOIN vat_codes c ON c.organization_id = d.organization_id AND c.code = d.vat_code
                 WHERE d.organization_id = ? AND d.settlement_id = ? ORDER BY d.register_type, d.vat_code, c.rate"
            );
            foreach ($settlements->fetchAll() as $settlement) {
                [$periodStart, $periodEnd] = $this->settlementPeriod($settlement);
                if ($periodEnd < $from || $periodStart > $to) {
                    continue;
                }
                $periodLabel = ($settlement['period_type'] === 'MONTHLY' ? 'M' : 'T')
                    . $settlement['period_number'] . '/' . $settlement['period_year'];
                $details->execute([$this->organizationId, $settlement['id']]);
                foreach ($details->fetchAll(PDO::FETCH_NUM) as $detail) {
                    $rows[] = [$periodLabel, ...$detail, '', '', ''];
                }
                $rows[] = [
                    $periodLabel . ' · TOTALE LIQUIDAZIONE', '', '', '', '', '', 0,
                    $settlement['vat_debit'], $settlement['vat_credit'], $settlement['previous_credit'],
                    $settlement['interest_amount'], $settlement['balance'],
                ];
            }
            return [$columns, $rows];
        }
        if (str_starts_with($type, 'VAT_')) {
            $filter = match ($type) {
                'VAT_SALES' => "m.register_type = 'SALES'",
                'VAT_PURCHASES' => "m.register_type = 'PURCHASES'",
                'VAT_REVERSE_CHARGE' => "m.operation_type = 'REVERSE_CHARGE'",
                'VAT_SELF_INVOICES' => "d.fatturapa_type IN ('TD16','TD17','TD18','TD19','TD20','TD21','TD27','TD28')",
                default => "m.register_type = 'CORRISPETTIVI'",
            };
            [$columns, $rows] = $this->query(
                ['Data', 'Protocollo', 'Controparte', 'Codice IVA', 'Articolo IVA', 'Aliquota %', 'Natura', 'Imponibile', 'IVA', 'IVA dovuta', 'IVA detraibile'],
                "SELECT m.movement_date, m.protocol_number, m.counterparty_name, COALESCE(m.vat_code, 'N/D'),
                        COALESCE(c.description, m.vat_code, 'N/D'), COALESCE(c.rate, 0), COALESCE(c.nature, ''),
                        m.taxable_amount, m.vat_amount, m.vat_due_amount, m.deductible_vat
                 FROM vat_movements m
                 LEFT JOIN vat_codes c ON c.organization_id = m.organization_id AND c.code = m.vat_code
                 LEFT JOIN documents d ON d.id = m.document_id AND d.organization_id = m.organization_id
                 WHERE m.organization_id = ? AND {$filter} AND m.movement_date BETWEEN ? AND ?
                 ORDER BY m.movement_date, m.protocol_number, m.id",
                [$this->organizationId, $from, $to],
            );
            $summary = [];
            foreach ($rows as $row) {
                $key = ($row[3] ?? 'N/D') . '|' . ($row[5] ?? 0) . '|' . ($row[6] ?? '');
                if (!isset($summary[$key])) {
                    $summary[$key] = ['', 'RIEPILOGO', '', $row[3], $row[4], $row[5], $row[6], 0.0, 0.0, 0.0, 0.0];
                }
                foreach ([7, 8, 9, 10] as $index) {
                    $summary[$key][$index] += (float) ($row[$index] ?? 0);
                }
            }
            foreach ($summary as $total) {
                $rows[] = $total;
            }
            return [$columns, $rows];
        }
        return $this->query(
            ['Codice', 'Descrizione', 'Data acquisto', 'Costo storico', 'Fondo civilistico', 'Valore netto', 'Stato'],
            'SELECT asset_code, description, purchase_date, purchase_cost, accumulated_depreciation, net_book_value, status
             FROM fixed_assets WHERE organization_id = ? AND purchase_date <= ? ORDER BY asset_code',
            [$this->organizationId, $to],
        );
    }

    private function query(array $columns, string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return [$columns, $statement->fetchAll(PDO::FETCH_NUM)];
    }

    private function html(array $company, string $type, int $sequence, string $from, string $to, array $columns, array $rows): string
    {
        $escape = static fn (mixed $value): string => htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $headers = implode('', array_map(static fn (string $column): string => '<th>' . htmlspecialchars($column, ENT_QUOTES, 'UTF-8') . '</th>', $columns));
        $numericColumns = ['Dare', 'Avere', 'Saldo', 'Saldo Dare', 'Saldo Avere', 'Aliquota %', 'Imponibile', 'IVA', 'IVA dovuta', 'IVA detraibile', 'Credito precedente', 'Interessi', 'Costo storico', 'Fondo civilistico', 'Valore netto'];
        $body = '';
        foreach ($rows as $row) {
            $isSummary = in_array('RIEPILOGO', $row, true)
                || in_array('TOTALI SALDI', $row, true)
                || in_array('DIFFERENZA DARE / AVERE', $row, true)
                || str_contains((string) ($row[0] ?? ''), 'TOTALE LIQUIDAZIONE');
            $body .= $isSummary ? '<tr class="summary-row">' : '<tr>';
            foreach ($row as $index => $value) {
                $display = in_array($columns[$index] ?? '', $numericColumns, true) && is_numeric($value)
                    ? number_format((float) $value, 2, ',', '.')
                    : (string) $value;
                $body .= '<td>' . $escape($display) . '</td>';
            }
            $body .= '</tr>';
        }
        if ($body === '') {
            $body = '<tr><td colspan="' . count($columns) . '">Nessun movimento nel periodo selezionato.</td></tr>';
        }
        return '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>
            @page { margin: 24mm 12mm 18mm; }
            body { font: 8.5pt DejaVu Sans, sans-serif; color:#172033; }
            header { border-bottom:2px solid #163b68; padding-bottom:10px; margin-bottom:14px; }
            h1 { margin:0 0 5px; font-size:17pt; color:#102a4c; } p { margin:2px 0; }
            .meta { color:#536277; } table { width:100%; border-collapse:collapse; }
            th { background:#edf3fa; color:#193b64; text-align:left; font-size:7.5pt; }
            th, td { border:1px solid #ccd6e3; padding:4px 5px; vertical-align:top; }
            tr:nth-child(even) td { background:#f8fafc; }
            .summary-row td { background:#edf3fa; font-weight:bold; border-top:1.5px solid #163b68; }
            footer { position:fixed; bottom:-12mm; left:0; right:0; border-top:1px solid #ccd6e3; padding-top:5px; font-size:7pt; color:#66758a; }
        </style></head><body><header><h1>' . $escape(self::TYPES[$type]) . '</h1>
        <p><strong>' . $escape($company['business_name']) . '</strong> · P.IVA ' . $escape($company['vat_number'] ?? '—') . '</p>
        <p class="meta">Periodo ' . $escape((new DateTimeImmutable($from))->format('d/m/Y')) . ' – ' . $escape((new DateTimeImmutable($to))->format('d/m/Y'))
        . ' · Progressivo ' . str_pad((string) $sequence, 6, '0', STR_PAD_LEFT) . '</p></header>
        <table><thead><tr>' . $headers . '</tr></thead><tbody>' . $body . '</tbody></table>
        <footer>Generato da Luna2 il ' . date('d/m/Y H:i') . ' · Documento sottoposto a controllo di integrità SHA-256.</footer></body></html>';
    }

    private function nextSequence(string $type): int
    {
        $key = 'OFFICIAL-' . $type;
        $statement = $this->db->prepare('SELECT id, next_value FROM document_sequences WHERE organization_id = ? AND sequence_key = ? FOR UPDATE');
        $statement->execute([$this->organizationId, $key]);
        $sequence = $statement->fetch();
        if (!$sequence) {
            $this->db->prepare(
                "INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at)
                 VALUES (?, ?, '', 2, 6, NOW(), NOW())"
            )->execute([$this->organizationId, $key]);
            return 1;
        }
        $this->db->prepare('UPDATE document_sequences SET next_value = next_value + 1, updated_at = NOW() WHERE id = ?')
            ->execute([$sequence['id']]);
        return (int) $sequence['next_value'];
    }

    private function period(string $from, string $to): array
    {
        $start = DateTimeImmutable::createFromFormat('!Y-m-d', $from);
        $end = DateTimeImmutable::createFromFormat('!Y-m-d', $to);
        if (!$start || !$end || $start > $end) {
            throw new InvalidArgumentException('Periodo di stampa non valido.');
        }
        return [$start->format('Y-m-d'), $end->format('Y-m-d')];
    }

    private function settlementPeriod(array $settlement): array
    {
        $year = (int) $settlement['period_year'];
        $number = (int) $settlement['period_number'];
        $firstMonth = $settlement['period_type'] === 'QUARTERLY' ? (($number - 1) * 3) + 1 : $number;
        $months = $settlement['period_type'] === 'QUARTERLY' ? 3 : 1;
        $start = new DateTimeImmutable(sprintf('%04d-%02d-01', $year, $firstMonth));
        $end = $start->modify('+' . $months . ' months -1 day');
        return [$start->format('Y-m-d'), $end->format('Y-m-d')];
    }

    private function nullable(?string $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
    }

    private function relative(string $path): string
    {
        return str_replace('\\', '/', ltrim(str_replace($this->basePath, '', $path), '/\\'));
    }
}
