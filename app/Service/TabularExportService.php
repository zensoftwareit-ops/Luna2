<?php

declare(strict_types=1);

namespace Luna\Service;

use Dompdf\Dompdf;
use Dompdf\Options;
use PhpOffice\PhpSpreadsheet\Cell\Coordinate;
use PhpOffice\PhpSpreadsheet\Spreadsheet;
use PhpOffice\PhpSpreadsheet\Style\Alignment;
use PhpOffice\PhpSpreadsheet\Style\Border;
use PhpOffice\PhpSpreadsheet\Style\Fill;
use PhpOffice\PhpSpreadsheet\Writer\Xlsx;

final class TabularExportService
{
    public function __construct(private readonly array $company = []) {}

    /** @param array<string,mixed> $statement @param array<string,mixed> $options */
    public function streamTrialBalancePdf(array $statement, string $from, string $to, array $options = []): never
    {
        $escape = static fn (mixed $value): string => htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $money = static fn (mixed $value): string => number_format((float) $value, 2, ',', '.');
        $dateValue = static fn (string $value): string => ($time = strtotime($value)) ? date('d/m/Y', $time) : $value;
        $includeParties = !empty($options['include_parties']);
        $includeClosing = !empty($options['include_closing']);
        $sections = (array) ($statement['sections'] ?? []);
        $totals = (array) ($statement['totals'] ?? []);
        foreach ($sections as $type => $rows) {
            $sections[$type] = array_values(array_filter(
                (array) $rows,
                static fn (array $row): bool => (string) ($row['code'] ?? '') !== 'APERTURA'
            ));
        }

        $renderRows = static function (array $rows) use ($escape, $money): string {
            if ($rows === []) return '<tr><td colspan="3" class="empty">Nessun saldo nella sezione</td></tr>';
            $html = '';
            foreach ($rows as $row) {
                $level = max(0, (int) ($row['level'] ?? 0));
                $classes = !empty($row['is_party']) ? 'party' : (empty($row['is_postable']) ? 'group' : 'account');
                if (!empty($row['is_unassigned'])) $classes .= ' unassigned';
                $type = (string) ($row['party_type'] ?? '');
                $partyLabel = $type === 'SUPPLIER' ? 'fornitore' : ($type === 'CUSTOMER' ? 'cliente' : '');
                $html .= '<tr class="' . $classes . '"><td class="code">' . $escape($row['code'] ?? '') . '</td>'
                    . '<td class="description">' . $escape($row['name'] ?? '')
                    . ($partyLabel !== '' ? '<small>' . $partyLabel . '</small>' : '')
                    . '</td><td class="num">' . $money($row['amount'] ?? 0) . '</td></tr>';
            }
            return $html;
        };

        $chunkRows = static function (array $rows, int $budget = 42): array {
            if ($rows === []) return [[]];
            $chunks = $current = [];
            $used = 0;
            foreach ($rows as $row) {
                $length = mb_strlen((string) ($row['name'] ?? '')) + max(0, (int) ($row['level'] ?? 0)) * 3;
                $weight = 1 + ($length > 38 ? 1 : 0) + ($length > 78 ? 1 : 0);
                if ($current !== [] && $used + $weight > $budget) {
                    $chunks[] = $current;
                    $current = [];
                    $used = 0;
                }
                $current[] = $row;
                $used += $weight;
            }
            if ($current !== []) $chunks[] = $current;
            return $chunks;
        };

        $profit = (float) ($totals['profit_loss'] ?? 0);
        $openingDifference = -(float) ($statement['opening']['adjustment'] ?? 0);
        $liabilitySubtotal = (float) ($totals['liabilities'] ?? 0) - $openingDifference;
        $assetsFooter = [['TOTALE ATTIVITA\'', (float) ($totals['assets'] ?? 0), false]];
        if ($profit < 0) $assetsFooter[] = ['PERDITA D\'ESERCIZIO', abs($profit), false];
        $assetsFooter[] = ['TOTALE A PAREGGIO', (float) ($totals['balance_assets'] ?? 0), true];
        $liabilityFooter = [['TOTALE PASSIVITA\' E PATRIMONIO NETTO', $liabilitySubtotal, false]];
        if (abs($openingDifference) >= .005) $liabilityFooter[] = ['DIFFERENZA DA ESERCIZI PRECEDENTI', $openingDifference, false];
        if ($profit >= 0) $liabilityFooter[] = ['UTILE D\'ESERCIZIO', $profit, false];
        $liabilityFooter[] = ['TOTALE A PAREGGIO', (float) ($totals['balance_liabilities'] ?? 0), true];
        $economicBalance = max((float) ($totals['expenses'] ?? 0), (float) ($totals['revenues'] ?? 0));
        $expenseFooter = [['TOTALE COSTI', (float) ($totals['expenses'] ?? 0), false]];
        if ($profit >= 0) $expenseFooter[] = ['UTILE D\'ESERCIZIO', $profit, false];
        $expenseFooter[] = ['TOTALE A PAREGGIO', $economicBalance, true];
        $revenueFooter = [['TOTALE RICAVI', (float) ($totals['revenues'] ?? 0), false]];
        if ($profit < 0) $revenueFooter[] = ['PERDITA D\'ESERCIZIO', abs($profit), false];
        $revenueFooter[] = ['TOTALE A PAREGGIO', $economicBalance, true];

        $companyName = (string) ($this->company['business_name'] ?? 'Luna2');
        $companyIdentity = implode(' - ', array_filter([
            !empty($this->company['vat_number']) ? 'P.IVA ' . $this->company['vat_number'] : '',
            !empty($this->company['tax_code']) ? 'C.F. ' . $this->company['tax_code'] : '',
        ]));
        $openingLabel = !empty($statement['opening']['posted']) ? 'Apertura contabilizzata'
            : (!empty($statement['opening']['carried']) ? 'Ripresa saldo' : 'Solo movimenti del periodo');
        $parameters = [$openingLabel, $includeClosing ? 'Includi movimenti di chiusura' : 'Escludi movimenti di chiusura', 'Solo conti movimentabili'];
        $parameters[] = $includeParties ? 'Con saldi clienti e fornitori' : 'Senza saldi clienti e fornitori';
        if (trim((string) ($options['search'] ?? '')) !== '') $parameters[] = 'Ricerca: ' . trim((string) $options['search']);
        if (trim((string) ($options['account_type'] ?? '')) !== '') $parameters[] = 'Tipo conto: ' . trim((string) $options['account_type']);

        $renderFooter = static function (array $lines) use ($escape, $money): string {
            if ($lines === []) return '';
            $html = '<tfoot>';
            foreach ($lines as [$label, $value, $grand]) {
                $html .= '<tr class="' . ($grand ? 'grand' : '') . '"><th colspan="2">' . $escape($label) . '</th><th class="num">' . $money($value) . '</th></tr>';
            }
            return $html . '</tfoot>';
        };
        $renderColumn = static function (string $title, array $rows, array $footer, bool $continued) use ($escape, $renderRows, $renderFooter): string {
            return '<td class="report-column"><table class="section"><thead><tr><th colspan="3">' . $escape($title)
                . ($continued ? ' <small>- segue</small>' : '') . '</th></tr></thead><tbody>'
                . $renderRows($rows) . '</tbody>' . $renderFooter($footer) . '</table></td>';
        };
        $pageHeader = static function (string $reportTitle) use ($escape, $dateValue, $companyName, $companyIdentity, $from, $to, $parameters): string {
            return '<header><div class="print-head"><span>LUNA2 - Contabilita generale</span><span>' . date('d/m/Y') . '</span></div>'
                . '<div class="company">' . $escape($companyName) . ($companyIdentity !== '' ? '<small>' . $escape($companyIdentity) . '</small>' : '') . '</div>'
                . '<h1>SITUAZIONE CONTABILE A SEZIONI PER COMPETENZA</h1><div class="period">dal ' . $escape($dateValue($from)) . ' al ' . $escape($dateValue($to)) . '</div>'
                . '<div class="parameters"><b>Altri parametri:</b> ' . $escape(implode('; ', $parameters)) . '</div><h2>' . $escape($reportTitle) . '</h2></header>';
        };
        $pages = [];
        $appendPages = static function (string $reportTitle, string $leftTitle, array $leftRows, array $leftFooter,
            string $rightTitle, array $rightRows, array $rightFooter) use (&$pages, $chunkRows, $pageHeader, $renderColumn): void {
            $leftChunks = $chunkRows($leftRows);
            $rightChunks = $chunkRows($rightRows);
            $pageCount = max(count($leftChunks), count($rightChunks));
            for ($index = 0; $index < $pageCount; $index++) {
                $leftChunk = $leftChunks[$index] ?? [];
                $rightChunk = $rightChunks[$index] ?? [];
                $pages[] = '<div class="report-page">' . $pageHeader($reportTitle) . '<table class="opposed"><tr>'
                    . $renderColumn($leftTitle, $leftChunk, $index === $pageCount - 1 ? $leftFooter : [], $index > 0)
                    . $renderColumn($rightTitle, $rightChunk, $index === $pageCount - 1 ? $rightFooter : [], $index > 0)
                    . '</tr></table></div>';
            }
        };
        $appendPages('STATO PATRIMONIALE', 'ATTIVITA\'', (array) ($sections['ASSET'] ?? []), $assetsFooter,
            'PASSIVITA\' E PATRIMONIO NETTO', array_merge((array) ($sections['EQUITY'] ?? []), (array) ($sections['LIABILITY'] ?? [])), $liabilityFooter);
        $appendPages('CONTO ECONOMICO', 'COSTI', (array) ($sections['EXPENSE'] ?? []), $expenseFooter,
            'RICAVI', (array) ($sections['REVENUE'] ?? []), $revenueFooter);
        $difference = abs((float) ($totals['debit'] ?? 0) - (float) ($totals['credit'] ?? 0));
        $lastPage = array_key_last($pages);
        $pages[$lastPage] = substr($pages[$lastPage], 0, -6) . '<div class="balance-check">CONTROLLO SALDI - Dare '
            . $money($totals['debit'] ?? 0) . ' - Avere ' . $money($totals['credit'] ?? 0)
            . ' - Differenza ' . $money($difference) . '</div></div>';

        $html = '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>
            @page{margin:8mm 10mm 11mm}body{font-family:DejaVu Sans,sans-serif;color:#111;font-size:7px;margin:0}.report-page{width:100%}.page-break{page-break-after:always}.print-head{display:table;width:100%;font-size:6.5px}.print-head span{display:table-cell}.print-head span:last-child{text-align:right}.company{margin-top:2px;font-size:7px}.company small{display:block;font-size:6px}h1{margin:8px 0 1px;text-align:center;font-size:12px;font-weight:500}.period{text-align:center;font-size:10px}.parameters{margin-top:8px;font-size:6.5px}.parameters b{margin-right:8px}header h2{margin:9px 0 5px;padding-bottom:4px;border-bottom:.8px solid #111;text-align:center;font-size:9px}.opposed{width:100%;border-collapse:collapse;table-layout:fixed;page-break-inside:avoid}.report-column{width:50%;padding:0 4px;vertical-align:top}.report-column:first-child{padding-left:2px;border-right:.65px solid #111}.report-column:last-child{padding-right:2px}.section{width:100%;border-collapse:collapse;table-layout:fixed}.section thead th{padding:2px;border-top:.65px solid #111;border-bottom:.65px solid #111;text-align:center;font-size:7px}.section thead small{font-size:5.7px;font-weight:400}.section td{padding:.65px 2px;line-height:1.15;vertical-align:top}.section .code{width:23%;white-space:nowrap}.section .description{width:57%;white-space:normal;overflow-wrap:anywhere}.section .num{width:20%;text-align:right;white-space:nowrap}.section .group td{padding-top:2.2px;font-weight:bold}.section .party td{font-size:6.2px}.section .party small{display:block;font-size:5.3px;font-style:italic}.section .unassigned td{font-style:italic}.section .empty{padding:7px;text-align:center}.section tfoot th{padding:2px;border-top:.65px solid #111;text-align:left;font-size:6.6px}.section tfoot th.num{text-align:right}.section tfoot tr+tr th{border-top:0}.section tfoot .grand th{padding-top:5px;border-top:1.2px solid #111;border-bottom:1.8px double #111}.balance-check{margin-top:7px;padding-top:3px;border-top:.65px solid #111;font-size:6px;text-align:right}
            </style></head><body>' . implode('<div class="page-break"></div>', $pages) . '</body></html>';

        $pdfOptions = new Options();
        $pdfOptions->set('isRemoteEnabled', false);
        $pdfOptions->set('isHtml5ParserEnabled', true);
        $pdf = new Dompdf($pdfOptions);
        $pdf->loadHtml($html, 'UTF-8');
        $pdf->setPaper('A4', 'portrait');
        $pdf->render();
        $canvas = $pdf->getCanvas();
        $canvas->page_text(28, $canvas->get_height() - 20, 'Luna2', $pdf->getFontMetrics()->getFont('DejaVu Sans'), 6, [0, 0, 0]);
        $canvas->page_text($canvas->get_width() - 74, $canvas->get_height() - 20, 'Pagina {PAGE_NUM} / {PAGE_COUNT}',
            $pdf->getFontMetrics()->getFont('DejaVu Sans'), 6, [0, 0, 0]);
        $pdf->stream($this->filename('situazione-contabile-' . ($includeParties ? 'con-clienti-fornitori' : 'sintetica')) . '.pdf', ['Attachment' => true]);
        exit;
    }

    /** @param array<string,mixed> $detail */
    public function streamPartyLedgerPdf(array $detail, string $from, string $to, string $type): never
    {
        $content = $this->partyLedgerPdfContent($detail, $from, $to, $type);
        $partyCode = (string) ($detail['party']['code'] ?? 'sottoconto');
        header('Content-Type: application/pdf');
        header('Content-Disposition: attachment; filename="' . $this->filename('scheda-contabile-' . $partyCode) . '.pdf"');
        header('Content-Length: ' . strlen($content));
        header('Cache-Control: private, no-store, max-age=0');
        echo $content;
        exit;
    }

    /** @param array<string,mixed> $account @param array<int,array<string,mixed>> $rows */
    public function streamLedgerPdf(array $account, array $rows, string $from, string $to): never
    {
        $content = $this->ledgerPdfContent($account, $rows, $from, $to);
        header('Content-Type: application/pdf');
        header('Content-Disposition: attachment; filename="' . $this->filename('mastrino-' . ($account['code'] ?? 'conto')) . '.pdf"');
        header('Content-Length: ' . strlen($content));
        header('Cache-Control: private, no-store, max-age=0');
        echo $content;
        exit;
    }

    /**
     * Mastrino nel medesimo formato professionale della scheda cliente/fornitore.
     * @param array<string,mixed> $account
     * @param array<int,array<string,mixed>> $rows
     */
    public function ledgerPdfContent(array $account, array $rows, string $from, string $to): string
    {
        $escape = static fn (mixed $value): string => htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $money = static fn (mixed $value): string => number_format((float) $value, 2, ',', '.');
        $dateValue = static fn (mixed $value): string => ($time = strtotime((string) $value)) ? date('d/m/Y', $time) : '';
        $body = '';
        foreach ($rows as $row) {
            $cause = trim(implode(' · ', array_filter([$row['cause_code'] ?? '', $row['cause_name'] ?? ''])));
            if ($cause === '') $cause = str_replace('_', ' ', (string) ($row['entry_type'] ?? 'Movimento'));
            $description = trim((string) ($row['entry_description'] ?? ''));
            $lineDescription = trim((string) ($row['description'] ?? ''));
            $body .= '<tr><td>' . $escape($dateValue($row['entry_date'] ?? null)) . '</td>'
                . '<td><b>' . $escape($row['protocol_number'] ?? '') . '</b><small>' . $escape($row['source_protocol'] ?? '') . '</small></td>'
                . '<td>' . $escape($dateValue($row['document_date'] ?? null)) . '</td><td>' . $escape($row['document_number'] ?? '') . '</td>'
                . '<td><b>' . $escape($cause) . '</b><small>' . $escape(trim($description . ($lineDescription !== '' && $lineDescription !== $description ? ' · ' . $lineDescription : ''))) . '</small></td>'
                . '<td>' . $escape($row['counterparty'] ?? '') . '</td><td class="num">' . $money($row['debit'] ?? 0) . '</td>'
                . '<td class="num">' . $money($row['credit'] ?? 0) . '</td><td class="num"><b>' . $money($row['running_balance'] ?? 0) . '</b></td></tr>';
        }
        if ($body === '') $body = '<tr><td colspan="9" class="empty">Nessun movimento nel periodo selezionato.</td></tr>';
        $company = implode(' - ', array_filter([$this->company['business_name'] ?? 'Luna2', !empty($this->company['vat_number']) ? 'P.IVA ' . $this->company['vat_number'] : '']));
        $html = '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>@page{margin:15mm 9mm 14mm}body{font-family:DejaVu Sans,sans-serif;color:#111827;font-size:7.2px}header{margin-bottom:12px}.company{color:#475569}.generated{float:right}.title{text-align:center;font-size:15px;font-weight:bold;margin:24px 0 18px}.identity{width:100%;border-collapse:collapse;margin-bottom:9px}.identity th{text-align:left;width:10%;padding:2px}.identity td{padding:2px}.movements{width:100%;border-collapse:collapse;table-layout:fixed}.movements th{padding:4px 3px;border-top:1.4px solid #111;border-bottom:1px solid #111;text-align:left;font-size:6.5px}.movements td{padding:4px 3px;border-bottom:.45px solid #64748b;vertical-align:top}.movements thead{display:table-header-group}.movements tr{page-break-inside:avoid}.movements th:nth-child(1){width:7%}.movements th:nth-child(2){width:11%}.movements th:nth-child(3){width:7%}.movements th:nth-child(4){width:9%}.movements th:nth-child(5){width:27%}.movements th:nth-child(6){width:14%}.movements th:nth-child(7),.movements th:nth-child(8),.movements th:nth-child(9){width:8.3%}.num{text-align:right;white-space:nowrap}small{display:block;color:#475569;font-size:6px;margin-top:2px}.progressive td{font-weight:bold;border-top:1px solid #111;border-bottom:1.4px solid #111;background:#f8fafc}.empty{text-align:center;padding:18px!important;color:#64748b}footer{position:fixed;bottom:-8mm;left:0;right:0;border-top:1px solid #334155;padding-top:4px;color:#475569;font-size:6.5px}</style></head><body><header><span class="generated">Generato il ' . date('d/m/Y H:i') . '</span><div class="company">' . $escape($company) . '</div></header><div class="title">SCHEDA CONTABILE - dal ' . $escape($dateValue($from)) . ' al ' . $escape($dateValue($to)) . '</div><table class="identity"><tr><th>Conto:</th><td><b>' . $escape($account['code'] ?? '') . '</b> - ' . $escape($account['name'] ?? '') . '</td><th>Tipo:</th><td>' . $escape($account['account_type'] ?? '') . '</td></tr></table><table class="movements"><thead><tr><th>Data reg.</th><th>N. mov.<br>Protocollo</th><th>Data doc.</th><th>N. documento</th><th>Causale e descrizione</th><th>Controparte</th><th class="num">Dare</th><th class="num">Avere</th><th class="num">Saldo</th></tr></thead><tbody><tr class="progressive"><td colspan="6">Progressivi antecedenti al ' . $escape($dateValue($from)) . '</td><td></td><td></td><td class="num">' . $money($account['opening_balance'] ?? 0) . '</td></tr>' . $body . '<tr class="progressive"><td colspan="6">Progressivi al ' . $escape($dateValue($to)) . '</td><td class="num">' . $money($account['period_debit'] ?? 0) . '</td><td class="num">' . $money($account['period_credit'] ?? 0) . '</td><td class="num">' . $money($account['closing_balance'] ?? 0) . '</td></tr></tbody></table><footer>Luna2 - Scheda contabile</footer></body></html>';
        $options = new Options(); $options->set('isRemoteEnabled', false); $options->set('isHtml5ParserEnabled', true);
        $pdf = new Dompdf($options); $pdf->loadHtml($html, 'UTF-8'); $pdf->setPaper('A4', 'portrait'); $pdf->render();
        $canvas = $pdf->getCanvas(); $canvas->page_text($canvas->get_width() - 72, $canvas->get_height() - 24, '{PAGE_NUM} / {PAGE_COUNT}', $pdf->getFontMetrics()->getFont('DejaVu Sans'), 6.5, [0.28, 0.33, 0.41]);
        return $pdf->output();
    }

    /**
     * Produce la scheda contabile del singolo cliente/fornitore nel formato
     * professionale DATEV: progressivi, documento, protocollo, Dare/Avere e saldo.
     *
     * @param array<string,mixed> $detail
     */
    public function partyLedgerPdfContent(array $detail, string $from, string $to, string $type): string
    {
        $escape = static fn (mixed $value): string => htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $money = static fn (mixed $value): string => number_format((float) $value, 2, ',', '.');
        $dateValue = static fn (mixed $value): string => ($time = strtotime((string) $value)) ? date('d/m/Y', $time) : '';
        $party = (array) ($detail['party'] ?? []);
        $account = (array) ($detail['collective_account'] ?? []);
        $totals = (array) ($detail['totals'] ?? []);
        $rows = (array) ($detail['rows'] ?? []);
        $isSupplier = strtoupper($type) === 'SUPPLIER';
        $partyLabel = $isSupplier ? 'Fornitore' : 'Cliente';
        $accountType = match ((string) ($account['account_type'] ?? '')) {
            'ASSET' => 'Attivita', 'LIABILITY' => 'Passivita', 'EQUITY' => 'Patrimonio netto',
            'REVENUE' => 'Ricavo', 'EXPENSE' => 'Costo', default => (string) ($account['account_type'] ?? ''),
        };
        $entryLabels = [
            'PURCHASE_INVOICE' => 'Fattura acquisti', 'SALES_INVOICE' => 'Fattura vendite',
            'CREDIT_NOTE' => 'Nota di credito', 'PAYMENT' => 'Pagamento/incasso',
            'PAYMENT_REVERSAL' => 'Storno pagamento', 'OPENING' => 'Apertura contabile',
            'CLOSING' => 'Chiusura contabile', 'MANUAL' => 'Registrazione manuale',
        ];
        $body = '';
        foreach ($rows as $row) {
            $cause = $entryLabels[(string) ($row['entry_type'] ?? '')] ?? str_replace('_', ' ', (string) ($row['entry_type'] ?? 'Movimento'));
            $entryDescription = trim((string) ($row['entry_description'] ?? ''));
            $lineDescription = trim((string) ($row['description'] ?? ''));
            $description = $cause . ($entryDescription !== '' ? ' - ' . $entryDescription : '');
            $body .= '<tr>'
                . '<td>' . $escape($dateValue($row['entry_date'] ?? null)) . '</td>'
                . '<td><b>' . $escape($row['protocol_number'] ?? '') . '</b></td>'
                . '<td>' . $escape($dateValue($row['document_date'] ?? null)) . '<small>' . $escape($row['source_protocol'] ?? '') . '</small></td>'
                . '<td>' . $escape($row['document_number'] ?? '') . '</td>'
                . '<td><b>' . $escape($description) . '</b>' . ($lineDescription !== '' && $lineDescription !== $entryDescription ? '<small>' . $escape($lineDescription) . '</small>' : '') . '</td>'
                . '<td class="num">' . $money($row['debit'] ?? 0) . '</td>'
                . '<td class="num">' . $money($row['credit'] ?? 0) . '</td>'
                . '<td class="num"><b>' . $money($row['accounting_running_balance'] ?? 0) . '</b></td>'
                . '</tr>';
        }
        if ($body === '') {
            $body = '<tr><td colspan="8" class="empty">Nessun movimento contabile nel periodo selezionato.</td></tr>';
        }
        $company = implode(' - ', array_filter([
            $this->company['business_name'] ?? 'Luna2',
            !empty($this->company['vat_number']) ? 'P.IVA ' . $this->company['vat_number'] : '',
            !empty($this->company['tax_code']) ? 'C.F. ' . $this->company['tax_code'] : '',
        ]));
        $partyAddress = trim(implode(' ', array_filter([
            $party['address'] ?? '', $party['postal_code'] ?? '', $party['city'] ?? '', $party['province'] ?? '',
        ])));
        $html = '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>
            @page{margin:15mm 10mm 14mm}body{font-family:DejaVu Sans,sans-serif;color:#111827;font-size:7.4px}
            header{margin-bottom:12px}.company{font-size:7px;color:#475569}.generated{float:right}.title{text-align:center;font-size:15px;font-weight:bold;margin:24px 0 18px}
            .identity{width:100%;border-collapse:collapse;margin-bottom:9px}.identity th{width:11%;text-align:left;padding:2px 4px 2px 0}.identity td{padding:2px 8px 2px 0}
            table.movements{width:100%;border-collapse:collapse;table-layout:fixed}.movements th{padding:4px 3px;border-top:1.4px solid #111;border-bottom:1px solid #111;text-align:left;font-size:6.7px}
            .movements td{padding:4px 3px;border-bottom:.45px solid #64748b;vertical-align:top}.movements thead{display:table-header-group}.movements tr{page-break-inside:avoid}
            .movements th:nth-child(1){width:8%}.movements th:nth-child(2){width:13%}.movements th:nth-child(3){width:9%}.movements th:nth-child(4){width:10%}.movements th:nth-child(5){width:31%}.movements th:nth-child(6),.movements th:nth-child(7),.movements th:nth-child(8){width:9.66%}
            .num{text-align:right;white-space:nowrap}small{display:block;color:#475569;font-size:6.3px;margin-top:2px}.progressive td{font-weight:bold;border-top:1px solid #111;border-bottom:1.4px solid #111;background:#f8fafc}.empty{text-align:center;padding:18px!important;color:#64748b}
            footer{position:fixed;bottom:-8mm;left:0;right:0;border-top:1px solid #334155;padding-top:4px;color:#475569;font-size:6.5px}
            </style></head><body><header><span class="generated">Generata il ' . date('d/m/Y H:i') . '</span><div class="company">' . $escape($company) . '</div></header>'
            . '<div class="title">SCHEDA CONTABILE - dal ' . $escape($dateValue($from)) . ' al ' . $escape($dateValue($to)) . '</div>'
            . '<table class="identity"><tr><th>Conto:</th><td><b>' . $escape($account['code'] ?? '') . '</b> - ' . $escape($account['name'] ?? '') . '</td><th>Tipo:</th><td>' . $escape($accountType) . '</td></tr>'
            . '<tr><th>' . $partyLabel . ':</th><td><b>' . $escape($party['code'] ?? '') . '</b> - ' . $escape($party['business_name'] ?? '') . '</td><th>P.IVA:</th><td>' . $escape($party['vat_number'] ?? '') . '</td></tr>'
            . '<tr><th>Indirizzo:</th><td>' . $escape($partyAddress) . '</td><th>C.F.:</th><td>' . $escape($party['tax_code'] ?? '') . '</td></tr></table>'
            . '<table class="movements"><thead><tr><th>Data reg.</th><th>N. mov.<br>Provv.</th><th>Data doc.<br>N. prot.</th><th>N. documento</th><th>Causale e descrizione movimento<br>Descrizione riga</th><th class="num">Dare</th><th class="num">Avere</th><th class="num">Saldo</th></tr></thead><tbody>'
            . '<tr class="progressive"><td colspan="5">Progressivi antecedenti al ' . $escape($dateValue($from)) . '</td><td class="num">' . $money($totals['opening_debit'] ?? 0) . '</td><td class="num">' . $money($totals['opening_credit'] ?? 0) . '</td><td class="num">' . $money($totals['accounting_opening'] ?? 0) . '</td></tr>'
            . $body
            . '<tr class="progressive"><td colspan="5">Progressivi al ' . $escape($dateValue($to)) . '</td><td class="num">' . $money($totals['progressive_debit'] ?? 0) . '</td><td class="num">' . $money($totals['progressive_credit'] ?? 0) . '</td><td class="num">' . $money($totals['accounting_closing'] ?? 0) . '</td></tr>'
            . '</tbody></table><footer>Luna2 - Scheda contabile ' . strtolower($partyLabel) . '</footer></body></html>';
        $options = new Options();
        $options->set('isRemoteEnabled', false);
        $options->set('isHtml5ParserEnabled', true);
        $pdf = new Dompdf($options);
        $pdf->loadHtml($html, 'UTF-8');
        $pdf->setPaper('A4', 'portrait');
        $pdf->render();
        $canvas = $pdf->getCanvas();
        $canvas->page_text($canvas->get_width() - 72, $canvas->get_height() - 24, '{PAGE_NUM} / {PAGE_COUNT}',
            $pdf->getFontMetrics()->getFont('DejaVu Sans'), 6.5, [0.28, 0.33, 0.41]);
        return $pdf->output();
    }

    /** @param array<int,array<string,mixed>> $rows */
    public function streamVatSettlementPdf(array $settlement, array $rows, string $filename): never
    {
        $escape = static fn (mixed $value): string => htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $money = static fn (mixed $value): string => number_format((float) $value, 2, ',', '.') . ' €';
        $balance = (float) $settlement['balance'];
        $resultLabel = $balance > .005 ? 'IVA DA VERSARE' : ($balance < -.005 ? 'CREDITO IVA DA RIPORTARE' : 'LIQUIDAZIONE A ZERO');
        $company = implode(' · ', array_filter([
            $this->company['business_name'] ?? 'Luna2',
            !empty($this->company['vat_number']) ? 'P.IVA ' . $this->company['vat_number'] : '',
            !empty($this->company['tax_code']) ? 'CF ' . $this->company['tax_code'] : '',
        ]));
        $labels = ['SALES' => 'REGISTRO VENDITE', 'PURCHASES' => 'REGISTRO ACQUISTI', 'CORRISPETTIVI' => 'REGISTRO CORRISPETTIVI'];
        $grouped = [];
        foreach ($rows as $row) { $grouped[(string) $row['register_type']][] = $row; }
        $tables = '';
        $operationTotals = ['ACTIVE' => ['taxable' => 0.0, 'debit' => 0.0], 'PASSIVE' => ['taxable' => 0.0, 'credit' => 0.0, 'effective' => 0.0, 'non_deductible' => 0.0]];
        foreach ($grouped as $register => $items) {
            $body = '';
            $taxable = $debit = $credit = $effective = $nonDeductible = $suspended = 0.0;
            foreach ($items as $row) {
                $isPurchase = $register === 'PURCHASES';
                $rowDebit = $isPurchase ? (float) $row['vat_due_amount'] : (float) $row['vat_amount'];
                $rowCredit = $isPurchase ? (float) $row['vat_amount'] : 0.0;
                $rowEffective = $isPurchase ? (float) $row['deductible_vat'] : (float) $row['vat_due_amount'];
                $taxable += (float) $row['taxable_amount']; $debit += $rowDebit; $credit += $rowCredit; $effective += $rowEffective;
                $nonDeductible += (float) $row['non_deductible_vat']; $suspended += (float) $row['suspended_vat'];
                $body .= '<tr><td><b>' . $escape($row['vat_code']) . '</b><small>' . $escape($row['vat_description']) . '</small></td><td class="num">' . number_format((float) $row['vat_rate'], 2, ',', '.') . '%</td><td>' . $escape($row['vat_nature'] ?: '—') . '<small>' . $escape($row['vat_legal_reference']) . '</small></td><td class="num">' . $money($row['taxable_amount']) . '</td><td class="num">' . $money($rowDebit) . '</td><td class="num">' . $money($isPurchase ? -$rowCredit : $rowCredit) . '</td><td class="num">' . $money($isPurchase ? -$rowEffective : $rowEffective) . '</td><td class="num">' . $money($row['non_deductible_vat']) . '</td><td class="num">' . $money($row['suspended_vat']) . '</td></tr>';
            }
            if ($register === 'PURCHASES') {
                $operationTotals['PASSIVE']['taxable'] += $taxable; $operationTotals['PASSIVE']['credit'] += $credit;
                $operationTotals['PASSIVE']['effective'] += $effective; $operationTotals['PASSIVE']['non_deductible'] += $nonDeductible;
            } else {
                $operationTotals['ACTIVE']['taxable'] += $taxable; $operationTotals['ACTIVE']['debit'] += $debit;
            }
            $tables .= '<section><h2>' . $escape($labels[$register] ?? $register) . '</h2><table><thead><tr><th>Articolo IVA</th><th>Aliquota</th><th>Natura / riferimento</th><th>Imponibile</th><th>Debito</th><th>IVA acquisti (lordo)</th><th>IVA detraibile (da sottrarre)</th><th>Indetr.</th><th>Sospesa</th></tr></thead><tbody>' . $body . '</tbody><tfoot><tr><th colspan="3">Totale registro</th><th class="num">' . $money($taxable) . '</th><th class="num">' . $money($debit) . '</th><th class="num">' . $money($register === 'PURCHASES' ? -$credit : $credit) . '</th><th class="num">' . $money($register === 'PURCHASES' ? -$effective : $effective) . '</th><th class="num">' . $money($nonDeductible) . '</th><th class="num">' . $money($suspended) . '</th></tr></tfoot></table></section>';
        }
        $tables .= '<section><h2>RIEPILOGO OPERAZIONI</h2><table><thead><tr><th>Operazioni</th><th>Imponibile</th><th>Imposta</th><th>Esigibile / detraibile</th><th>Indetraibile</th></tr></thead><tbody><tr><td><b>TOTALI OPERAZIONI ATTIVE</b></td><td class="num">' . $money($operationTotals['ACTIVE']['taxable']) . '</td><td class="num">' . $money($operationTotals['ACTIVE']['debit']) . '</td><td class="num">' . $money($settlement['vat_debit']) . '</td><td class="num">' . $money(0) . '</td></tr><tr><td><b>TOTALI OPERAZIONI PASSIVE</b></td><td class="num">' . $money($operationTotals['PASSIVE']['taxable']) . '</td><td class="num">' . $money($operationTotals['PASSIVE']['credit']) . '</td><td class="num">' . $money($operationTotals['PASSIVE']['effective']) . '</td><td class="num">' . $money($operationTotals['PASSIVE']['non_deductible']) . '</td></tr></tbody></table></section>';
        $period = ((string) $settlement['period_type'] === 'MONTHLY' ? 'Mese ' : 'Trimestre ') . $settlement['period_number'] . ' / ' . $settlement['period_year'];
        $reconciliation = (($settlement['source_reconciliation_mode'] ?? 'STANDARD') === 'DATEV_PRIORITY')
            ? '<p class="reconciliation"><b>Riconciliazione fonti:</b> registro DATEV prioritario; ' . (int) ($settlement['excluded_document_rows'] ?? 0) . ' righe XML escluse dal secondo conteggio (IVA acquisti lorda ' . $money($settlement['excluded_document_purchase_vat'] ?? 0) . ', IVA detraibile ' . $money($settlement['excluded_document_vat_credit'] ?? 0) . ', IVA a debito ' . $money($settlement['excluded_document_vat_debit'] ?? 0) . ').</p>'
            : '';
        $html = '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>@page{margin:12mm 10mm}body{font-family:DejaVu Sans,sans-serif;color:#172033;font-size:8px}header{border-bottom:2px solid #17365d;padding-bottom:7px;margin-bottom:9px}h1{font-size:16px;margin:0 0 3px}.meta{color:#64748b}.result{margin:10px 0 12px;padding:12px 15px;border:2px solid ' . ($balance > .005 ? '#c43d49' : '#21835e') . ';background:#f8fafc}.result span{font-size:11px;font-weight:bold}.result strong{float:right;font-size:20px;color:' . ($balance > .005 ? '#a92330' : '#087a50') . '}.formula{clear:both;padding-top:8px;color:#475569;font-size:8px}.formula b{color:#172033}.reconciliation{padding:7px 9px;background:#eef6ff;border:1px solid #b9d6f5}section{margin-top:12px;page-break-inside:avoid}h2{font-size:10px;margin:0;padding:6px 7px;background:#17365d;color:#fff}table{width:100%;border-collapse:collapse}th,td{padding:4px;border:1px solid #d7dee8;vertical-align:top}thead th{background:#eaf0f7;text-align:left}tfoot th{background:#f2f5f9;border-top:2px solid #8291a5}.num{text-align:right;white-space:nowrap}small{display:block;color:#64748b;margin-top:2px}footer{position:fixed;bottom:-6mm;right:0;color:#64748b}</style></head><body><header><h1>Liquidazione IVA</h1><div class="meta">' . $escape($company) . ' · ' . $escape($period) . ' · elaborata il ' . date('d/m/Y H:i') . '</div></header><div class="result"><span>' . $resultLabel . '</span><strong>' . $money(abs($balance)) . '</strong><div class="formula"><b>' . $money($settlement['vat_debit']) . '</b> IVA a debito − <b>' . $money($settlement['vat_credit']) . '</b> IVA detraibile − <b>' . $money($settlement['previous_credit']) . '</b> credito precedente + <b>' . $money($settlement['interest_amount']) . '</b> interessi</div></div>' . $reconciliation . '<p>Le note di credito sono esposte con imponibile e imposta negativi. Nel registro acquisti IVA lorda e detraibile sono esposte col segno di sottrazione.</p>' . $tables . '<footer>Luna2 · prospetto di liquidazione IVA</footer></body></html>';
        $options = new Options(); $options->set('isRemoteEnabled', false); $options->set('isHtml5ParserEnabled', true);
        $pdf = new Dompdf($options); $pdf->loadHtml($html, 'UTF-8'); $pdf->setPaper('A4', 'landscape'); $pdf->render();
        $canvas = $pdf->getCanvas(); $canvas->page_text(28, $canvas->get_height() - 20, 'Pagina {PAGE_NUM} / {PAGE_COUNT}', $pdf->getFontMetrics()->getFont('DejaVu Sans'), 7);
        $pdf->stream($this->filename($filename) . '.pdf', ['Attachment' => true]); exit;
    }
    /**
     * @param array<int,array{key:string,label:string,type?:string}> $columns
     * @param array<int,array<string,mixed>> $rows
     * @param array<string,string> $filters
     */
    public function stream(
        string $format,
        string $title,
        array $columns,
        array $rows,
        array $filters = [],
        string $organization = 'Luna2',
        ?string $filename = null,
    ): never {
        $format = strtolower($format);
        if ($this->company !== []) {
            $organization = implode(' · ', array_filter([
                $this->company['business_name'] ?? $organization,
                'P.IVA ' . ($this->company['vat_number'] ?? 'non indicata'),
                'CF ' . ($this->company['tax_code'] ?? 'non indicato'),
                implode(' ', array_filter([$this->company['address'] ?? '', $this->company['postal_code'] ?? '',
                    $this->company['city'] ?? '', $this->company['province'] ?? ''])),
            ]));
        }
        if (!in_array($format, ['pdf', 'xlsx', 'csv'], true)) {
            throw new \InvalidArgumentException('Formato di esportazione non supportato.');
        }
        $base = $this->filename($filename ?: $title);
        match ($format) {
            'pdf' => $this->pdf($base, $title, $columns, $rows, $filters, $organization),
            'xlsx' => $this->xlsx($base, $title, $columns, $rows, $filters, $organization),
            default => $this->csv($base, $columns, $rows),
        };
    }

    private function csv(string $filename, array $columns, array $rows): never
    {
        header('Content-Type: text/csv; charset=UTF-8');
        header('Content-Disposition: attachment; filename="' . $filename . '.csv"');
        header('Cache-Control: private, no-store, max-age=0');
        $output = fopen('php://output', 'wb');
        fwrite($output, "\xEF\xBB\xBF");
        fputcsv($output, array_column($columns, 'label'), ';');
        foreach ($rows as $row) {
            fputcsv($output, array_map(
                fn (array $column): mixed => $this->plain($row[$column['key']] ?? null, $column['type'] ?? 'text'),
                $columns,
            ), ';');
        }
        fclose($output);
        exit;
    }

    private function xlsx(
        string $filename,
        string $title,
        array $columns,
        array $rows,
        array $filters,
        string $organization,
    ): never {
        $book = new Spreadsheet();
        $book->getProperties()->setCreator('Luna2')->setCompany($organization)->setTitle($title);
        $sheet = $book->getActiveSheet();
        $sheet->setTitle(mb_substr(preg_replace('/[\\\\\\/?*\\[\\]:]/', '', $title) ?: 'Esportazione', 0, 31));
        $lastColumn = Coordinate::stringFromColumnIndex(max(1, count($columns)));
        $sheet->mergeCells("A1:{$lastColumn}1");
        $sheet->setCellValue('A1', $title);
        $sheet->mergeCells("A2:{$lastColumn}2");
        $sheet->setCellValue('A2', $organization . ' · Generato il ' . date('d/m/Y H:i'));
        $filterText = $filters === [] ? 'Nessun filtro applicato' : implode(' · ', array_map(
            static fn (string $key, string $value): string => $key . ': ' . $value,
            array_keys($filters),
            array_values($filters),
        ));
        $sheet->mergeCells("A3:{$lastColumn}3");
        $sheet->setCellValue('A3', $filterText);
        foreach ($columns as $index => $column) {
            $sheet->setCellValue(Coordinate::stringFromColumnIndex($index + 1) . '5', $column['label']);
        }
        foreach ($rows as $rowIndex => $row) {
            foreach ($columns as $columnIndex => $column) {
                $value = $row[$column['key']] ?? null;
                $type = $column['type'] ?? 'text';
                $cell = Coordinate::stringFromColumnIndex($columnIndex + 1) . ($rowIndex + 6);
                $sheet->setCellValue($cell, $this->spreadsheetValue($value, $type));
                if (in_array($type, ['money', 'decimal', 'number'], true)) {
                    $sheet->getStyle($cell)->getNumberFormat()
                        ->setFormatCode($type === 'money' ? '#,##0.00 [$€-it-IT]' : '#,##0.00');
                }
            }
        }
        $sheet->getStyle("A1:{$lastColumn}1")->getFont()->setBold(true)->setSize(16)->getColor()->setRGB('FFFFFF');
        $sheet->getStyle("A1:{$lastColumn}1")->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB('17365D');
        $sheet->getStyle("A5:{$lastColumn}5")->getFont()->setBold(true)->getColor()->setRGB('FFFFFF');
        $sheet->getStyle("A5:{$lastColumn}5")->getFill()->setFillType(Fill::FILL_SOLID)->getStartColor()->setRGB('2563EB');
        $sheet->getStyle("A5:{$lastColumn}" . max(5, count($rows) + 5))->getBorders()->getAllBorders()
            ->setBorderStyle(Border::BORDER_HAIR)->getColor()->setRGB('D8E0EA');
        $sheet->getStyle("A2:{$lastColumn}3")->getFont()->setSize(9)->getColor()->setRGB('64748B');
        $sheet->getStyle("A1:{$lastColumn}" . max(5, count($rows) + 5))->getAlignment()
            ->setVertical(Alignment::VERTICAL_CENTER);
        $sheet->freezePane('A6');
        $sheet->setAutoFilter("A5:{$lastColumn}5");
        foreach ($columns as $index => $column) {
            $width = max(12, min(38, mb_strlen($column['label']) + 5));
            foreach (array_slice($rows, 0, 250) as $row) {
                $width = max($width, min(38, mb_strlen((string) ($row[$column['key']] ?? '')) + 3));
            }
            $sheet->getColumnDimension(Coordinate::stringFromColumnIndex($index + 1))->setWidth($width);
        }
        header('Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        header('Content-Disposition: attachment; filename="' . $filename . '.xlsx"');
        header('Cache-Control: private, no-store, max-age=0');
        (new Xlsx($book))->save('php://output');
        $book->disconnectWorksheets();
        exit;
    }

    private function pdf(
        string $filename,
        string $title,
        array $columns,
        array $rows,
        array $filters,
        string $organization,
    ): never {
        $escape = static fn (mixed $value): string => htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
        $head = '';
        foreach ($columns as $column) {
            $head .= '<th>' . $escape($column['label']) . '</th>';
        }
        $body = '';
        foreach ($rows as $row) {
            $body .= '<tr>';
            foreach ($columns as $column) {
                $type = $column['type'] ?? 'text';
                $class = in_array($type, ['money', 'decimal', 'number'], true) ? ' class="num"' : '';
                $body .= '<td' . $class . '>' . $escape($this->display($row[$column['key']] ?? null, $type)) . '</td>';
            }
            $body .= '</tr>';
        }
        if ($body === '') {
            $body = '<tr><td colspan="' . count($columns) . '">Nessun dato corrispondente ai filtri.</td></tr>';
        }
        $filterText = $filters === [] ? 'Nessun filtro applicato' : implode(' · ', array_map(
            static fn (string $key, string $value): string => $key . ': ' . $value,
            array_keys($filters),
            array_values($filters),
        ));
        $html = '<!doctype html><html lang="it"><head><meta charset="utf-8"><style>
            @page{margin:8mm 7mm}body{font-family:DejaVu Sans,sans-serif;color:#172033;font-size:7px}
            h1{font-size:13px;margin:0 0 2px}.meta{color:#64748b;font-size:6.5px;margin-bottom:7px}
            table{width:100%;border-collapse:collapse;table-layout:auto}th{background:#17365d;color:#fff;text-transform:uppercase;
            letter-spacing:.025em;font-size:6px;padding:4px 3px;border:1px solid #17365d}td{padding:3px;border:1px solid #d7dee8;
            vertical-align:top;word-break:break-word}tr:nth-child(even) td{background:#f7f9fc}.num{text-align:right;white-space:nowrap}
            footer{position:fixed;bottom:-5mm;left:0;right:0;text-align:right;color:#64748b;font-size:6px}
            </style></head><body><h1>' . $escape($title) . '</h1><div class="meta">' . $escape($organization)
            . ' · ' . $escape($filterText) . ' · Generato il ' . date('d/m/Y H:i') . ' · ' . count($rows)
            . ' righe</div><table><thead><tr>' . $head . '</tr></thead><tbody>' . $body
            . '</tbody></table><footer>Luna2 · stampa tecnica</footer></body></html>';
        $options = new Options();
        $options->set('isRemoteEnabled', false);
        $options->set('isHtml5ParserEnabled', true);
        $pdf = new Dompdf($options);
        $pdf->loadHtml($html, 'UTF-8');
        $pdf->setPaper('A4', 'landscape');
        $pdf->render();
        $canvas = $pdf->getCanvas();
        $canvas->page_text(22, $canvas->get_height() - 18, 'Pagina {PAGE_NUM} / {PAGE_COUNT}',
            $pdf->getFontMetrics()->getFont('DejaVu Sans'), 7);
        $pdf->stream($filename . '.pdf', ['Attachment' => true]);
        exit;
    }

    private function display(mixed $value, string $type): string
    {
        if ($value === null || $value === '') {
            return '—';
        }
        return match ($type) {
            'money' => number_format((float) $value, 2, ',', '.') . ' €',
            'decimal' => number_format((float) $value, 2, ',', '.'),
            'date' => ($time = strtotime((string) $value)) ? date('d/m/Y', $time) : (string) $value,
            'datetime' => ($time = strtotime((string) $value)) ? date('d/m/Y H:i', $time) : (string) $value,
            'boolean' => $value ? 'Sì' : 'No',
            default => (string) $value,
        };
    }

    private function plain(mixed $value, string $type): mixed
    {
        if ($value === null) {
            return '';
        }
        return in_array($type, ['money', 'decimal', 'number'], true) ? (float) $value : $this->display($value, $type);
    }

    private function spreadsheetValue(mixed $value, string $type): mixed
    {
        if ($value === null) {
            return '';
        }
        return in_array($type, ['money', 'decimal', 'number'], true) ? (float) $value : $this->display($value, $type);
    }

    private function filename(string $value): string
    {
        $value = mb_strtolower(trim($value));
        $value = preg_replace('/[^a-z0-9]+/i', '-', $value) ?: 'esportazione';
        return trim($value, '-') . '-' . date('Ymd-His');
    }
}
