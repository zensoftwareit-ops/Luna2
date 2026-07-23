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
