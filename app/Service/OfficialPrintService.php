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
        'LEDGERS' => 'Mastrini - tutti i conti',
        'TRIAL_BALANCE' => 'Bilancio di verifica',
        'VAT_SALES' => 'Registro IVA vendite',
        'VAT_PURCHASES' => 'Registro IVA acquisti',
        'VAT_CORRISPETTIVI' => 'Registro corrispettivi',
        'VAT_REVERSE_CHARGE' => 'Registro IVA reverse charge',
        'VAT_SELF_INVOICES' => 'Registro IVA autofatture',
        'VAT_LIQUIDATION' => 'Liquidazione IVA per articolo e aliquota',
        'ASSET_REGISTER' => 'Registro cespiti',
    ];

    private array $issues = [];

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
        $this->issues = [];
        $this->db->beginTransaction();
        try {
        [$columns, $rows] = $this->dataset($type, $from, $to);
        $organization = $this->db->prepare('SELECT * FROM organizations WHERE id = ?');
        $organization->execute([$this->organizationId]);
        $company = $organization->fetch();
        if (!$company) {
            throw new RuntimeException('Azienda non trovata.');
        }
        foreach (['business_name', 'tax_code', 'address', 'postal_code', 'city'] as $field) {
            if (trim((string) ($company[$field] ?? '')) === '') { $this->issues[] = 'Anagrafica azienda incompleta: ' . $field; }
        }
        $snapshot = json_encode(['version' => 'fiscal-prints-2', 'company' => $company, 'type' => $type,
            'from' => $from, 'to' => $to, 'columns' => $columns, 'rows' => $rows,
            'issues' => array_values(array_unique($this->issues)), 'generated_at' => date(DATE_ATOM)],
            JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
            $this->db->commit();
        } catch (Throwable $error) {
            if ($this->db->inTransaction()) { $this->db->rollBack(); }
            throw $error;
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
            $this->db->prepare('UPDATE official_print_runs SET snapshot_json = ?, snapshot_sha256 = ? WHERE id = ? AND organization_id = ?')
                ->execute([$snapshot, hash('sha256', $snapshot), $id, $this->organizationId]);
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
            $dompdf->setPaper('A4', 'landscape');
            $dompdf->render();
            $canvas = $dompdf->getCanvas();
            $pageCount = $canvas->get_page_count();
            $this->db->beginTransaction();
            try {
                $firstPage = $this->reservePages($type, substr($from, 0, 4), $pageCount);
                $this->db->prepare('UPDATE official_print_runs SET first_page = ?, page_count = ? WHERE id = ? AND organization_id = ?')
                    ->execute([$firstPage, $pageCount, $id, $this->organizationId]);
                $this->db->commit();
            } catch (Throwable $error) {
                if ($this->db->inTransaction()) { $this->db->rollBack(); }
                throw $error;
            }
            $year = substr($from, 0, 4);
            $canvas->page_script(static function ($page, $pages, $canvas, $fonts) use ($firstPage, $year, $sequence): void {
                $font = $fonts->getFont('DejaVu Sans');
                $canvas->text(34, $canvas->get_height() - 13,
                    'Anno ' . $year . ' - pagina ' . ($firstPage + $page - 1) . ' | Elaborazione ' . $sequence . ' | Foglio ' . $page . '/' . $pages, $font, 7);
            });
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
        [, $row] = $this->file($id);
        $snapshot = $this->snapshot($row);
        if (!empty($snapshot['issues'])) {
            throw new InvalidArgumentException('Correggere i dati e rigenerare la stampa: ' . implode('; ', array_slice($snapshot['issues'], 0, 6)));
        }
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
        [, $row] = $this->file($id);
        $this->snapshot($row);
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

    public function dossier(int $id): string
    {
        [$path, $row] = $this->file($id);
        $snapshot = $this->snapshot($row);
        $zip = new \ZipArchive();
        $temp = tempnam(dirname($path), 'dossier-');
        if ($temp === false || $zip->open($temp, \ZipArchive::OVERWRITE) !== true) {
            throw new RuntimeException('Impossibile creare il fascicolo.');
        }
        try {
            if (!$zip->addFile($path, basename($path))
                || !$zip->addFromString('dati.json', $row['snapshot_json'])
                || !$zip->addFromString('manifest.json', json_encode([
                    'format' => 'LUNA2-EVIDENCE-1', 'print_id' => $id, 'organization_id' => $this->organizationId,
                    'pdf' => basename($path), 'pdf_sha256' => $row['file_sha256'], 'data_sha256' => $row['snapshot_sha256'],
                    'first_page' => $row['first_page'], 'page_count' => $row['page_count'], 'status' => $row['status'],
                    'validation_reference' => $row['professional_validation_reference'], 'validated_at' => $row['validated_at'],
                    'validated_by' => $row['validated_by'], 'locked_at' => $row['locked_at'],
                    'issues' => $snapshot['issues'], 'preservation_status' => 'NOT_SUBMITTED',
                ], JSON_THROW_ON_ERROR | JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE))
                || !$zip->addFromString('LEGGIMI.txt', "Fascicolo di evidenza Luna2. Contiene PDF, dati e impronte SHA-256.\nIl blocco applicativo e questo ZIP non attestano firma, marcatura temporale o conservazione digitale a norma.\nAffidare il pacchetto alla procedura di tenuta/conservazione concordata con il professionista.\n")) {
                throw new RuntimeException('Creazione fascicolo incompleta.');
            }
            if (!$zip->close()) { throw new RuntimeException('Chiusura fascicolo fallita.'); }
            return $temp;
        } catch (Throwable $error) {
            @$zip->close(); @unlink($temp); throw $error;
        }
    }

    private function snapshot(array $row): array
    {
        if (empty($row['snapshot_json']) || empty($row['snapshot_sha256'])
            || !hash_equals($row['snapshot_sha256'], hash('sha256', $row['snapshot_json']))) {
            throw new RuntimeException('Dati storici assenti o alterati: rigenerare la stampa con la nuova versione.');
        }
        return json_decode($row['snapshot_json'], true, 512, JSON_THROW_ON_ERROR);
    }

    private function reservePages(string $type, string $year, int $count): int
    {
        $key = 'PAGES-' . $type . '-' . $year;
        $this->db->prepare("INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at)
            VALUES (?, ?, '', 1, 6, NOW(), NOW()) ON DUPLICATE KEY UPDATE sequence_key = VALUES(sequence_key)")
            ->execute([$this->organizationId, $key]);
        $statement = $this->db->prepare('SELECT next_value FROM document_sequences WHERE organization_id = ? AND sequence_key = ? FOR UPDATE');
        $statement->execute([$this->organizationId, $key]);
        $first = (int) $statement->fetchColumn();
        $this->db->prepare('UPDATE document_sequences SET next_value = next_value + ? WHERE organization_id = ? AND sequence_key = ?')
            ->execute([$count, $this->organizationId, $key]);
        return $first;
    }

    private function dataset(string $type, string $from, string $to): array
    {
        if ($type === 'LEDGERS') {
            $accounts = $this->db->prepare('SELECT id FROM chart_of_accounts WHERE organization_id = ? ORDER BY code');
            $accounts->execute([$this->organizationId]);
            $rows = [];
            foreach ($accounts->fetchAll(PDO::FETCH_COLUMN) as $accountId) {
                [$a, $lines] = (new LedgerReportService($this->db, $this->organizationId))->dataset((int) $accountId, $from, $to);
                if (!$lines && abs($a['opening_balance']) < 0.005) { continue; }
                $name = $a['code'] . ' - ' . $a['name'];
                $rows[] = [$name, $from, '', 'SALDO INIZIALE', '', '', '', '', $a['opening_balance']];
                foreach ($lines as $line) {
                    $rows[] = [$name, $line['entry_date'], $line['protocol_number'], $line['description'] ?: $line['entry_description'],
                        $line['document_number'], $line['counterparty'], $line['debit'], $line['credit'], $line['running_balance']];
                }
                $rows[] = [$name, $to, '', 'TOTALI PERIODO / SALDO FINALE', '', '', $a['period_debit'], $a['period_credit'], $a['closing_balance']];
            }
            return [['Conto', 'Data', 'Protocollo', 'Descrizione', 'Documento', 'Controparte', 'Dare', 'Avere', 'Saldo'], $rows];
        }
        if ($type === 'JOURNAL') {
            [$columns, $rows] = $this->query(
                ['Data', 'Protocollo', 'Conto', 'Descrizione', 'Documento', 'Controparte', 'Tipo', 'Dare', 'Avere'],
                "SELECT e.entry_date, e.protocol_number, CONCAT(a.code, ' · ', a.name), COALESCE(NULLIF(l.description,''), e.description),
                        e.document_number, e.counterparty, e.entry_type,
                        l.debit, l.credit
                 FROM journal_entries e
                 INNER JOIN journal_entry_lines l ON l.journal_entry_id = e.id AND l.organization_id = e.organization_id
                 INNER JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = e.organization_id
                  WHERE e.organization_id = ? AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
                  ORDER BY e.entry_date, e.id, l.line_number",
                [$this->organizationId, $from, $to],
            );
            $debit = round(array_sum(array_column($rows, 7)), 2);
            $credit = round(array_sum(array_column($rows, 8)), 2);
            $carry = $this->db->prepare("SELECT COALESCE(SUM(l.debit),0), COALESCE(SUM(l.credit),0)
                FROM journal_entries e JOIN journal_entry_lines l ON l.journal_entry_id = e.id AND l.organization_id = e.organization_id
                WHERE e.organization_id = ? AND e.status = 'POSTED' AND e.entry_date >= ? AND e.entry_date < ?");
            $carry->execute([$this->organizationId, substr($from, 0, 4) . '-01-01', $from]);
            [$carryDebit, $carryCredit] = array_map('floatval', $carry->fetch(PDO::FETCH_NUM));
            $progressDebit = (int) round($carryDebit * 100); $progressCredit = (int) round($carryCredit * 100);
            foreach ($rows as &$row) {
                $progressDebit += (int) round((float) $row[7] * 100);
                $progressCredit += (int) round((float) $row[8] * 100);
                $row[] = $progressDebit / 100; $row[] = $progressCredit / 100;
            }
            unset($row);
            $columns[] = 'Progressivo Dare'; $columns[] = 'Progressivo Avere';
            array_unshift($rows, ['', '', '', 'RIPORTO DALL’INIZIO ANNO', '', '', '', '', '', $carryDebit, $carryCredit]);
            if (abs($debit - $credit) > 0.005) { $this->issues[] = 'Libro giornale: Dare e Avere non quadrano.'; }
            $rows[] = ['', '', '', 'TOTALI PERIODO', '', '', '', $debit, $credit, $progressDebit / 100, $progressCredit / 100];
            $rows[] = ['', '', '', 'DIFFERENZA DARE / AVERE', '', '', '', max(0, $debit - $credit), max(0, $credit - $debit), '', ''];
            return [$columns, $rows];
        }
        if ($type === 'TRIAL_BALANCE') {
            [$columns, $rows] = $this->query(
                ['Codice', 'Conto', 'Saldo Dare', 'Saldo Avere'],
                "SELECT a.code, a.name,
                        GREATEST(COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.debit-l.credit ELSE 0 END),0), 0),
                        GREATEST(COALESCE(SUM(CASE WHEN e.id IS NOT NULL THEN l.credit-l.debit ELSE 0 END),0), 0)
                 FROM chart_of_accounts a
                 LEFT JOIN journal_entry_lines l ON l.account_id = a.id AND l.organization_id = a.organization_id
                 LEFT JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id AND e.status = 'POSTED' AND e.entry_date BETWEEN ? AND ?
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
            $columns = ['Periodo', 'Registro', 'Codice IVA', 'Articolo IVA', 'Aliquota %', 'Natura', 'Riferimento normativo', 'Imponibile', 'Imposta a debito', 'Imposta a credito', 'Esigibile / detraibile', 'Indetraibile', 'Sospesa', 'Credito precedente', 'Interessi', 'Saldo', 'Stato / pagamento'];
            $rows = [];
            $settlements = $this->db->prepare(
                "SELECT * FROM vat_settlements
                 WHERE organization_id = ? AND period_year BETWEEN ? AND ? AND status IN ('CALCULATED','SUBMITTED','PAID','OVERDUE')
                 ORDER BY period_year, period_type, period_number"
            );
            $settlements->execute([$this->organizationId, (int) substr($from, 0, 4), (int) substr($to, 0, 4)]);
            $details = $this->db->prepare(
                "SELECT d.register_type, d.vat_code, d.vat_description, d.vat_rate,
                        d.vat_nature, d.vat_legal_reference, d.taxable_amount,
                        CASE WHEN d.register_type = 'PURCHASES' THEN d.vat_due_amount ELSE d.vat_amount END,
                        CASE WHEN d.register_type = 'PURCHASES' THEN d.vat_amount ELSE 0 END,
                        CASE WHEN d.register_type = 'PURCHASES' THEN d.deductible_vat ELSE d.vat_due_amount END,
                        d.non_deductible_vat, d.suspended_vat
                 FROM vat_settlement_details d
                 WHERE d.organization_id = ? AND d.settlement_id = ? ORDER BY d.register_type, d.vat_code, d.vat_rate, d.vat_nature"
            );
            foreach ($settlements->fetchAll() as $settlement) {
                [$periodStart, $periodEnd] = $this->settlementPeriod($settlement);
                if ($periodEnd < $from || $periodStart > $to) {
                    continue;
                }
                if ($periodStart < $from || $periodEnd > $to) {
                    $this->issues[] = 'Liquidazione selezionata solo parzialmente: scegliere mesi/trimestri completi.';
                }
                $periodLabel = ($settlement['period_type'] === 'MONTHLY' ? 'M' : 'T')
                    . $settlement['period_number'] . '/' . $settlement['period_year'];
                $details->execute([$this->organizationId, $settlement['id']]);
                $detailRows = $details->fetchAll(PDO::FETCH_NUM);
                foreach ($detailRows as $detail) {
                    $rows[] = [$periodLabel, ...$detail, '', '', '', ''];
                }
                $deltaDebit = round((float) $settlement['vat_debit'] - array_sum(array_column($detailRows, 7)), 2);
                $deltaCredit = round((float) $settlement['vat_credit'] - array_sum(array_column($detailRows, 9)), 2);
                if ($deltaDebit != 0 || $deltaCredit != 0) {
                    $adjustments = $this->db->prepare("SELECT id, adjustment_type, description, vat_debit_delta, vat_credit_delta, updated_at
                        FROM vat_adjustments WHERE organization_id = ? AND fiscal_year = ? AND period_month BETWEEN ? AND ?
                        AND status IN ('APPROVED','POSTED') ORDER BY period_month, id");
                    $adjustments->execute([$this->organizationId, $settlement['period_year'], (int) substr($periodStart, 5, 2), (int) substr($periodEnd, 5, 2)]);
                    $adjustmentRows = $adjustments->fetchAll();
                    if (abs(array_sum(array_column($adjustmentRows, 'vat_debit_delta')) - $deltaDebit) > 0.005
                        || abs(array_sum(array_column($adjustmentRows, 'vat_credit_delta')) - $deltaCredit) > 0.005) {
                        $this->issues[] = $periodLabel . ': rettifiche non riconciliate; ricalcolare la liquidazione.';
                    }
                    foreach ($adjustmentRows as $adjustment) {
                        $rows[] = [$periodLabel, 'RETTIFICA #' . $adjustment['id'], '', $adjustment['adjustment_type'], '', '', '', '',
                            $adjustment['vat_debit_delta'], '', $adjustment['vat_credit_delta'], '', '', '', '', '', $adjustment['description']];
                    }
                }
                $expected = round((float) $settlement['vat_debit'] - (float) $settlement['vat_credit'] - (float) $settlement['previous_credit'] + (float) $settlement['interest_amount'], 2);
                if (abs($expected - (float) $settlement['balance']) > 0.005) { $this->issues[] = $periodLabel . ': saldo liquidazione non coerente.'; }
                if ($settlement['status'] === 'PAID' && (empty($settlement['payment_date']) || empty($settlement['payment_reference']))) {
                    $this->issues[] = $periodLabel . ': pagamento privo di data o riferimento F24.';
                }
                $rows[] = [
                    $periodLabel . ' · TOTALE LIQUIDAZIONE', '', '', '', '', '', '', 0,
                    $settlement['vat_debit'], '', $settlement['vat_credit'], '', '', $settlement['previous_credit'],
                    $settlement['interest_amount'], $settlement['balance'],
                    $settlement['status'] . "\nScadenza: " . ($settlement['payment_due_date'] ?: 'non indicata')
                        . "\nPagamento: " . ($settlement['payment_date'] ?: '-') . "\nF24: " . ($settlement['payment_reference'] ?? '-'),
                ];
                $lipe = $this->db->prepare('SELECT id, quarter_number, status, export_checksum FROM lipe_communications WHERE organization_id = ? AND fiscal_year = ? AND quarter_number = ?');
                $lipe->execute([$this->organizationId, $settlement['period_year'], (int) ceil((int) substr($periodEnd, 5, 2) / 3)]);
                $lipeRow = $lipe->fetch();
                $rows[] = [$periodLabel, 'RIFERIMENTI', '', '', '', '', '', '', '', '', '', '', '', '', '', '',
                    'Calcolo: ' . ($settlement['calculated_at'] ?: '-') . "\nNote: " . ($settlement['notes'] ?: '-')
                    . "\nLIPE: " . ($lipeRow ? '#' . $lipeRow['id'] . ' T' . $lipeRow['quarter_number'] . ' ' . $lipeRow['status'] . ' (prospetto di raccordo)' : 'nessun prospetto collegato')];
            }
            return [$columns, $rows];
        }
        if (str_starts_with($type, 'VAT_')) {
            $filter = match ($type) {
                'VAT_SALES' => "m.register_type = 'SALES'",
                'VAT_PURCHASES' => "m.register_type = 'PURCHASES'",
                'VAT_REVERSE_CHARGE' => "m.operation_type = 'REVERSE_CHARGE'",
                'VAT_SELF_INVOICES' => "(m.operation_type = 'SELF_INVOICE' OR d.fatturapa_type IN ('TD16','TD17','TD18','TD19','TD20','TD21','TD27','TD28'))",
                default => "m.register_type = 'CORRISPETTIVI'",
            };
            [$columns, $rows] = $this->query(
                ['Data registrazione', 'Protocollo', 'Controparte', 'Codice IVA', 'Articolo IVA', 'Aliquota %', 'Natura', 'Riferimento normativo', 'Imponibile', 'IVA', 'IVA dovuta', 'IVA detraibile', 'Registro / sezione', 'Documento / data', 'Competenza IVA', 'Origine / integrazione'],
                "SELECT m.movement_date, m.protocol_number, m.counterparty_name, COALESCE(m.vat_code, 'N/D'),
                        COALESCE(m.vat_description, m.vat_code, 'N/D'), m.vat_rate, m.vat_nature, m.vat_legal_reference,
                        m.taxable_amount, m.vat_amount, m.vat_due_amount, m.deductible_vat,
                        CONCAT(m.register_type, ' / ', COALESCE(r.code, 'UNICO')),
                        CONCAT(COALESCE(d.number, m.document_reference, ''), ' / ', COALESCE(d.document_date, m.document_reference_date, '')),
                        CONCAT(m.period_year, '-', LPAD(m.period_month, 2, '0')),
                        CONCAT('ID ', m.id, ' ', COALESCE(d.fatturapa_type,''), ' ', m.operation_type, ' Orig.: ', COALESCE(src.number,''))
                 FROM vat_movements m
                 LEFT JOIN documents d ON d.id = m.document_id AND d.organization_id = m.organization_id
                 LEFT JOIN documents src ON src.id = d.source_document_id AND src.organization_id = m.organization_id
                 LEFT JOIN vat_registers r ON r.id = m.vat_register_id AND r.organization_id = m.organization_id
                 WHERE m.organization_id = ? AND {$filter} AND m.movement_date BETWEEN ? AND ?
                 ORDER BY m.movement_date, m.protocol_number, m.id",
                [$this->organizationId, $from, $to],
            );
            $summary = [];
            foreach ($rows as $row) {
                if (($row[13] === '' || str_starts_with($row[13], ' / ') || str_ends_with($row[13], ' / ')) && $type !== 'VAT_CORRISPETTIVI') { $this->issues[] = 'Estremi documento mancanti per movimento ' . $row[15]; }
                if (trim((string) $row[2]) === '' && $type !== 'VAT_CORRISPETTIVI') { $this->issues[] = 'Controparte mancante: ' . $row[15]; }
                if ((float) $row[5] == 0 && (float) $row[8] != 0 && empty($row[6]) && empty($row[7])) {
                    $this->issues[] = 'Operazione senza aliquota: specificare natura o riferimento normativo (' . $row[15] . ').';
                }
                $key = $row[12] . '|' . ($row[3] ?? 'N/D') . '|' . ($row[4] ?? '') . '|' . ($row[5] ?? 0) . '|' . ($row[6] ?? '') . '|' . ($row[7] ?? '');
                if (!isset($summary[$key])) {
                    $summary[$key] = ['', 'RIEPILOGO', '', $row[3], $row[4], $row[5], $row[6], $row[7], 0.0, 0.0, 0.0, 0.0, $row[12], '', '', ''];
                }
                foreach ([8, 9, 10, 11] as $index) {
                    $summary[$key][$index] += (float) ($row[$index] ?? 0);
                }
            }
            foreach ($summary as $total) {
                $rows[] = $total;
            }
            return [$columns, $rows];
        }
        return $this->assetRegister($from, $to);
    }

    private function assetRegister(string $from, string $to): array
    {
        $year = (int) substr($from, 0, 4);
        if ($from !== $year . '-01-01' || $to !== $year . '-12-31') {
            $this->issues[] = 'Registro cespiti: selezionare l’intero esercizio annuale.';
        }
        $statement = $this->db->prepare('SELECT a.*, y.id AS schedule_id, y.original_cost, y.revaluations, y.writedowns,
            y.civil_opening_fund, y.tax_opening_fund, y.civil_rate, y.tax_rate, y.civil_quota, y.tax_quota,
            y.disposal_date AS year_disposal_date, y.disposal_proceeds AS year_disposal_proceeds, y.evidence_reference
            FROM fixed_assets a LEFT JOIN fixed_asset_register_years y
              ON y.fixed_asset_id = a.id AND y.organization_id = a.organization_id AND y.fiscal_year = ?
            WHERE a.organization_id = ? AND a.purchase_date <= ? ORDER BY a.asset_code');
        $statement->execute([$year, $this->organizationId, $to]);
        $rows = [];
        foreach ($statement->fetchAll() as $asset) {
            if (!$asset['schedule_id']) {
                $this->issues[] = 'Cespite ' . $asset['asset_code'] . ': completare la scheda annuale ' . $year . '.';
                $rows[] = [$asset['asset_code'], $asset['description'], $asset['purchase_date'], 'DA COMPLETARE', '', '', '', '', '', '', '', '', '', '', '', ''];
                continue;
            }
            $base = (float) $asset['original_cost'] + (float) $asset['revaluations'] - (float) $asset['writedowns'];
            foreach (['civil' => 'Civilistico', 'tax' => 'Fiscale'] as $prefix => $label) {
                $fund = round((float) $asset[$prefix . '_opening_fund'] + (float) $asset[$prefix . '_quota'], 2);
                $rows[] = [$asset['asset_code'], $asset['description'], $asset['purchase_date'], $label,
                    $asset['original_cost'], $asset['revaluations'], $asset['writedowns'], $asset[$prefix . '_opening_fund'],
                    $asset[$prefix . '_rate'], $asset[$prefix . '_quota'], $fund, round($base - $fund, 2),
                    $asset['year_disposal_date'], $asset['year_disposal_proceeds'], $asset['category'], $asset['evidence_reference']];
            }
        }
        return [['Codice', 'Descrizione', 'Data acquisto', 'Valori', 'Costo storico', 'Rivalutazioni', 'Svalutazioni', 'Fondo iniziale',
            'Coefficiente %', 'Quota annuale', 'Fondo finale', 'Valore netto', 'Eliminazione', 'Corrispettivo', 'Categoria', 'Riferimento verifica'], $rows];
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
        $numericColumns = ['Dare', 'Avere', 'Progressivo Dare', 'Progressivo Avere', 'Saldo', 'Saldo Dare', 'Saldo Avere', 'Aliquota %', 'Imponibile', 'IVA', 'IVA a debito', 'IVA dovuta', 'IVA detraibile', 'Credito precedente', 'Interessi', 'Costo storico', 'Fondo civilistico', 'Valore netto', 'Rivalutazioni', 'Svalutazioni', 'Fondo iniziale', 'Coefficiente %', 'Quota annuale', 'Fondo finale', 'Corrispettivo'];
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
                $display = preg_replace_callback('/\b(\d{4})-(\d{2})-(\d{2})\b/', static fn ($m): string => $m[3] . '/' . $m[2] . '/' . $m[1], $display);
                $body .= '<td' . (in_array($columns[$index] ?? '', $numericColumns, true) ? ' class="num"' : '') . '>' . nl2br($escape($display)) . '</td>';
            }
            $body .= '</tr>';
        }
        if ($body === '') {
            $body = '<tr><td colspan="' . count($columns) . '">Nessun movimento nel periodo selezionato.</td></tr>';
        }
        $issueHtml = $this->issues === [] ? '' : '<h2>Da completare prima della validazione</h2><ul><li>'
            . implode('</li><li>', array_map($escape, array_unique($this->issues))) . '</li></ul>';
        $address = implode(' ', array_filter([$company['address'] ?? '', $company['postal_code'] ?? '', $company['city'] ?? '', $company['province'] ?? '', $company['country_code'] ?? '']));
        return '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>
            @page { margin: 30mm 10mm 18mm; }
            body { font: 6.8pt DejaVu Sans, sans-serif; color:#172033; }
            header { position:fixed; top:-24mm; left:0; right:0; border-bottom:1px solid #163b68; padding-bottom:5px; }
            h1 { margin:0 0 3px; font-size:12pt; color:#102a4c; } p { margin:2px 0; }
            .meta { color:#536277; } table { width:100%; border-collapse:collapse; }
            th { background:#edf3fa; color:#193b64; text-align:left; font-size:6.5pt; }
            th, td { border:1px solid #ccd6e3; padding:3px; vertical-align:top; overflow-wrap:break-word; }
            .num { text-align:right; } thead { display:table-header-group; } tr { page-break-inside:avoid; }
            tr:nth-child(even) td { background:#f8fafc; }
            .summary-row td { background:#edf3fa; font-weight:bold; border-top:1.5px solid #163b68; }
            footer { position:fixed; bottom:-12mm; left:0; right:0; border-top:1px solid #ccd6e3; padding-top:5px; font-size:7pt; color:#66758a; }
        </style></head><body><header><h1>' . $escape(self::TYPES[$type]) . '</h1>
        <p><strong>' . $escape($company['business_name']) . '</strong> · P.IVA ' . $escape($company['vat_number'] ?? '—') . ' · CF ' . $escape($company['tax_code'] ?? '—') . ' · ' . $escape($address) . '</p>
        <p class="meta">Periodo ' . $escape((new DateTimeImmutable($from))->format('d/m/Y')) . ' – ' . $escape((new DateTimeImmutable($to))->format('d/m/Y'))
        . ' · Elaborazione ' . str_pad((string) $sequence, 6, '0', STR_PAD_LEFT) . ' · Importi in ' . $escape($company['currency'] ?? 'EUR') . '</p></header>'
        . $issueHtml . '
        <table><thead><tr>' . $headers . '</tr></thead><tbody>' . $body . '</tbody></table>
        <footer>Generato da Luna2 il ' . date('d/m/Y H:i') . ' · Verifica professionale e stato disponibili nel fascicolo allegato.</footer></body></html>';
    }

    private function nextSequence(string $type): int
    {
        $key = 'OFFICIAL-' . $type;
        $this->db->prepare("INSERT INTO document_sequences (organization_id, sequence_key, prefix, next_value, padding, created_at, updated_at)
            VALUES (?, ?, '', 1, 6, NOW(), NOW()) ON DUPLICATE KEY UPDATE sequence_key = VALUES(sequence_key)")
            ->execute([$this->organizationId, $key]);
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
        if (!$start || !$end || $start->format('Y-m-d') !== $from || $end->format('Y-m-d') !== $to || $start > $end
            || $start->format('Y') !== $end->format('Y')) {
            throw new InvalidArgumentException('Periodo non valido: generare un registro per ciascun anno.');
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
