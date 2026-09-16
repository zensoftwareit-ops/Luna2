<?php
declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PhpOffice\PhpSpreadsheet\IOFactory;
use PhpOffice\PhpSpreadsheet\Shared\Date;

/** Reads original DK print layouts. Never infers missing journal lines or asset dates. */
final class DatevKoinosReader
{
    public const LABELS = [
        'accounts' => 'Piano dei conti', 'customers' => 'Clienti', 'suppliers' => 'Fornitori',
        'causes' => 'Causali contabili', 'vat_articles' => 'Articoli IVA', 'vat_rates' => 'Aliquote IVA',
        'tax_accounts' => 'Conti per tributo', 'asset_categories' => 'Categorie cespiti per gruppo e specie',
        'asset_accounts' => 'Conti per categoria cespite', 'asset_rates' => 'Aliquote per cespite',
        'asset_progressives' => 'Progressivi storici cespiti', 'journal_headers' => 'Testate di prima nota',
        'fixed_asset_master' => 'Anagrafica completa cespiti', 'asset_movements' => 'Movimenti cespiti',
        'invoice_history' => 'Fatture storiche XML', 'invoice_index' => 'Elenco fatture esportate',
        'vat_pdf' => 'Registro IVA originale PDF', 'journal_pdf' => 'Libro giornale originale PDF',
        'asset_register_pdf' => 'Registro beni ammortizzabili originale PDF',
    ];

    public function read(string $path, string $name): array
    {
        $extension = strtolower(pathinfo($name, PATHINFO_EXTENSION));
        if (in_array($extension, ['xls', 'xlsx'], true)) return $this->spreadsheet($path, $name);
        if ($extension !== 'csv') throw new InvalidArgumentException('Il tracciato DATEV richiede CSV, XLS o XLSX.');
        $raw = (string) file_get_contents($path);
        if (!mb_check_encoding($raw, 'UTF-8')) $raw = mb_convert_encoding($raw, 'UTF-8', 'Windows-1252');
        $raw = preg_replace('/^\xEF\xBB\xBF/', '', $raw) ?? $raw;
        $stream = fopen('php://temp', 'w+');
        fwrite($stream, $raw); rewind($stream); $rows = [];
        while (($row = fgetcsv($stream, 0, ';', '"', '')) !== false) {
            $rows[] = array_map(static fn ($v): string => trim((string) $v), $row);
            if (count($rows) > 200000) throw new InvalidArgumentException('Il file supera 200.000 righe.');
        }
        fclose($stream);
        $heading = mb_strtoupper(implode(' ', array_merge(...array_slice($rows, 0, 6))));
        $kind = match (true) {
            str_contains($heading, 'ELENCO CONTI PER TRIBUTO') => 'tax_accounts',
            str_contains($heading, 'LISTA CLIENTI/FORNITORI') => 'parties',
            str_contains($heading, 'ELENCO CAUSALI CONTABILI') => 'causes',
            str_contains($heading, 'STAMPA ARTICOLI IVA') => 'vat_articles',
            str_contains($heading, 'STAMPA ALIQUOTE IVA') => 'vat_rates',
            str_contains($heading, 'ELENCO ALIQUOTE AMMORTAMENTO PER CESPITE') => 'asset_rates',
            str_contains($heading, 'ELENCO CONTI PER CATEGORIA CESPITE') => 'asset_accounts',
            str_contains($heading, 'ELENCO CATEGORIE CESPITI') => 'asset_categories',
            str_contains($heading, 'PIANO DEI CONTI') => 'accounts',
            default => throw new InvalidArgumentException('Stampa Koinos non riconosciuta. Usare un tracciato standard oppure verificare le prime righe del file.'),
        };
        $records = []; $section = null; $group = ''; $species = ''; $category = ''; $current = null;
        foreach ($rows as $index => $r) {
            $v = static fn (int $i): string => $r[$i] ?? '';
            if ($kind === 'parties') {
                if (in_array($v(0), ['Clienti', 'Fornitori'], true)) { $section = $v(0) === 'Clienti' ? 'customers' : 'suppliers'; $current = null; }
                elseif ($section && preg_match('/^\d+$/D', $v(0)) && $v(3) !== '') {
                    $records[] = $this->record($section, $v(0), $index + 1, ['code' => $v(0), 'business_name' => $v(3), 'tax_code' => $v(4), 'vat_number' => $v(7)], $r);
                    $current = count($records) - 1;
                } elseif ($current !== null && $v(0) === '' && $v(3) !== '' && $v(3) !== 'Indirizzo') {
                    $address = $v(3);
                    $records[$current]['data']['address_original'] = $address;
                    if (preg_match('/^(.*?)\s+-\s+(\d{5})\s+(.+?)\s+\(([A-Z]{2})\)\s*$/u', $address, $m) && $m[4] !== 'EE') {
                        $records[$current]['data'] += ['address' => $m[1], 'postal_code' => $m[2], 'city' => $m[3], 'province' => $m[4], 'country_code' => 'IT'];
                    } else $records[$current]['data']['address'] = $address;
                    $records[$current]['raw']['address_row'] = $r;
                    $current = null;
                }
                continue;
            }
            if ($kind === 'asset_categories') {
                if ($v(0) === 'Gruppo:') { $group = $v(4); $species = ''; }
                if ($v(0) === 'Specie:') $species = $v(4);
                if (preg_match('/^[A-Z]\d{3}$/D', $v(1))) {
                    if ($group === '' || $species === '') throw new InvalidArgumentException('Categoria cespite senza gruppo/specie.');
                    $records[] = $this->record($kind, implode(':', [$group, $species, $v(1), $v(22)]), $index + 1, [
                        'group' => $group, 'species' => $species, 'code' => $v(1), 'name' => $v(3), 'asset_type' => $v(8),
                        'depreciation_type' => $v(10), 'print_mode' => $v(12), 'advance_allowed' => $v(15),
                        'ordinary_rate' => $this->number($v(17)), 'advance_rate' => $this->number($v(20)), 'valid_from' => $v(22),
                    ], $r);
                }
                continue;
            }
            if ($kind === 'asset_accounts') {
                if ($v(0) === 'Categoria:' && preg_match('/^([A-Z]\d{3})\s+-\s+(.*)$/u', $v(3), $m)) {
                    $records[] = $this->record($kind, $m[1], $index + 1, ['code' => $m[1], 'name' => $m[2], 'accounts' => []], $r);
                    $current = count($records) - 1;
                } elseif ($current !== null && ($v(0) === 'Conto cespite:' || $v(1) !== '')) {
                    $label = $v(0) === 'Conto cespite:' ? $v(0) : $v(1);
                    $records[$current]['data']['accounts'][$label] = ['civil' => $v(0) === 'Conto cespite:' ? $v(3) : $v(4), 'tax' => $v(5)];
                    $records[$current]['raw']['details'][] = $r;
                }
                continue;
            }
            if ($kind === 'asset_rates') {
                if ($v(0) === 'Categoria:') $category = $v(2);
                if (preg_match('/^\d+$/D', $v(0)) && $v(2) !== '') $records[] = $this->record($kind, $v(0), $index + 1, [
                    'asset_code' => $v(0), 'description' => $v(2), 'category' => $category,
                    'ministerial_rate' => $this->number($v(4)), 'ministerial_advance_rate' => $this->number($v(5)),
                    'civil_rate_override' => $this->number($v(7)), 'civil_advance_override' => $this->number($v(9)),
                    'tax_rate_override' => $this->number($v(12)), 'tax_advance_override' => $this->number($v(13)),
                ], $r);
                continue;
            }
            if ($kind === 'accounts' && preg_match('/^\d+$/D', $v(0)) && in_array($v(6), ['A','P','C','R','O'], true)) {
                $type = ['A' => 'ASSET', 'P' => 'LIABILITY', 'C' => 'EXPENSE', 'R' => 'REVENUE', 'O' => null][$v(6)];
                // The printed DK grouping explicitly places these equity headings before provisions.
                if ($v(6) === 'P' && (int) substr($v(0), 0, 3) >= 170 && (int) substr($v(0), 0, 3) <= 255) $type = 'EQUITY';
                $records[] = $this->record($kind, $v(0), $index + 1, ['code' => $v(0), 'name' => $v(3), 'source_type' => $v(6), 'account_type' => $type, 'subledger' => $v(7)], $r,
                    $type === null ? 'Conto d’ordine: conservato nel piano originale, classificazione operativa da concordare.' : null);
            } elseif ($kind === 'causes' && preg_match('/^[A-Z]\d{2,}$/D', $v(0))) {
                $records[] = $this->record($kind, $v(0), $index + 1, ['code' => $v(0), 'name' => $v(4), 'source_type' => $v(1), 'vat_type' => $v(5), 'register_type' => $v(6), 'account' => $v(7), 'payment_cause' => $v(8), 'next_cause' => $v(9)], $r);
            } elseif ($kind === 'vat_articles' && preg_match('/^[A-Za-z0-9]+$/D', $v(0)) && $v(1) !== '' && is_numeric(str_replace(',', '.', $v(4)))) {
                $records[] = $this->record($kind, $v(0), $index + 1, ['code' => $v(0), 'description' => $v(1), 'non_deductible_percent' => $this->number($v(4)), 'taxability' => $v(5), 'territory' => $v(6), 'operation_type' => $v(7), 'split_payment' => $v(8), 'nature' => $v(9), 'self_invoice' => $v(10), 'special_regime' => $v(11), 'margin_regime' => $v(12), 'compensation_rate' => $this->number($v(15)), 'alternate_rate' => $this->number($v(16))], $r);
            } elseif ($kind === 'vat_rates' && $v(1) !== '' && is_numeric(str_replace(',', '.', $v(9)))) {
                $records[] = $this->record($kind, $v(0), $index + 1, ['code' => $v(0), 'description' => $v(1), 'taxability' => $v(4), 'rate' => $this->number($v(9))], $r);
            } elseif ($kind === 'tax_accounts' && preg_match('/^\d{4}$/D', $v(0))) {
                $records[] = $this->record($kind, $v(0), $index + 1, ['code' => $v(0), 'description' => $v(1), 'type' => $v(4), 'debit_account' => $v(5), 'debit_description' => $v(6), 'credit_account' => $v(7), 'credit_description' => $v(8)], $r);
            }
        }
        if ($kind === 'accounts') {
            $codes = array_fill_keys(array_column($records, 'key'), true);
            $parents = [];
            foreach ($records as &$record) {
                $code = $record['key']; $parent = null;
                for ($length = strlen($code) - 1; $length > 0; $length--) {
                    $prefix = substr($code, 0, $length);
                    if (isset($codes[$prefix])) { $parent = $prefix; $parents[$prefix] = true; break; }
                }
                $record['data']['parent_code'] = $parent;
            }
            unset($record);
            foreach ($records as &$record) $record['data']['is_postable'] = !isset($parents[$record['key']]);
            unset($record);
        }
        if (!$records) throw new InvalidArgumentException('Nessun dato riconosciuto nella stampa Koinos.');
        return $records;
    }

    private function spreadsheet(string $path, string $name): array
    {
        $reader = IOFactory::createReaderForFile($path);
        $book = $reader->load($path); $result = [];
        try {
            foreach ($book->getWorksheetIterator() as $sheet) {
                $lastColumn = $sheet->getHighestDataColumn();
                $lastRow = $sheet->getHighestDataRow();
                if ($lastRow > 200001 || \PhpOffice\PhpSpreadsheet\Cell\Coordinate::columnIndexFromString($lastColumn) > 256) throw new InvalidArgumentException('Foglio troppo grande.');
                $headers = [];
                foreach ($sheet->getRowIterator(1, $lastRow) as $row) {
                    $values = [];
                    foreach ($row->getCellIterator('A', $lastColumn) as $cell) {
                        $value = $cell->getValue();
                        if (is_numeric($value) && Date::isDateTime($cell)) $value = Date::excelToDateTimeObject((float) $value)->format('Y-m-d');
                        $values[] = is_bool($value) ? ($value ? 'TRUE' : 'FALSE') : ($value === null ? '' : (string) $value);
                    }
                    if ($row->getRowIndex() === 1) { $headers = $values; continue; }
                    if (!array_filter($values, static fn ($v) => $v !== '')) continue;
                    $data = array_combine($headers, array_pad(array_slice($values, 0, count($headers)), count($headers), ''));
                    if (isset($data['IDMovimento'], $data['IDCespite'], $data['DataDocumento'])) {
                        $kind = 'asset_movements'; $key = (string) $data['IDMovimento'];
                        $data += [
                            'movement_id' => $data['IDMovimento'], 'asset_code' => $data['IDCespite'],
                            'cause_code' => $data['IDCausaleTestata'] ?? null, 'description' => $data['Descrizione'] ?? null,
                            'document_date' => $data['DataDocumento'] ?? null, 'document_number' => $data['NumeroDocumento'] ?? null,
                            'party_code' => $data['Codice'] ?? null, 'party_name' => $data['Nominativo'] ?? ($data['DenominazioneCognome'] ?? null),
                            'valid_from' => $data['DataInizioValidita'] ?? null, 'tax_valid_from' => $data['DataInizioValiditaFiscale'] ?? null,
                        ];
                        $issue = 'Movimento acquisito con causale e riferimenti. Gli importi sono ricostruiti dai progressivi e dai registri annuali, perché questo foglio non li espone.';
                    } elseif (isset($data['IDCespite'], $data['DataInizioUtilizzo'], $data['ValoreAcq'])) {
                        $kind = 'fixed_asset_master'; $key = (string) $data['IDCespite'];
                        $data += [
                            'asset_code' => $data['IDCespite'], 'description' => $data['Descrizione'] ?? null,
                            'category_code' => $data['IDCategoria'] ?? null, 'category_name' => $data['DescrizioneCategoria'] ?? null,
                            'purchase_date' => $data['DataDocAcq'] ?: ($data['DataInizioUtilizzo'] ?? null),
                            'in_service_date' => $data['DataInizioUtilizzo'] ?? null,
                            'purchase_cost' => $data['ValoreAcq'] ?? null, 'tax_purchase_cost' => $data['ValoreAcqFiscale'] ?? null,
                            'civil_depreciation_rate' => $data['PercAmmOrdinario'] ?? null,
                            'tax_depreciation_rate' => $data['PercAmmOrdinarioFiscale'] ?? null,
                            'supplier_source_id' => $data['IDNominativoAcq'] ?? null,
                            'disposal_date' => $data['DataAlienazioneEliminazione'] ?? null,
                            'source_status' => $data['TPStato'] ?? null,
                        ];
                        $issue = null;
                    } elseif (isset($data['IDPrimanota'], $data['DataRegistrazione'])) {
                        $kind = 'journal_headers'; $key = $data['Periodo'] . ':' . $data['IDPrimanota'];
                        $issue = 'Solo testata: mancano le righe con conto, Dare e Avere. Non genera scritture né saldi contabili.';
                    } elseif (isset($data['IDCespite'], $data['DataFinale'], $data['FondoAmmOrdinario'])) {
                        $kind = 'asset_progressives'; $key = implode(':', [$data['IDGruppo'], $data['IDSpecie'], $data['IDCespite'], $data['DataFinale']]);
                        $data += [
                            'asset_code' => $data['IDCespite'], 'as_of_date' => $data['DataFinale'],
                            'historical_cost' => $data['ValoreAcq'] ?? null, 'current_value' => $data['ValoreAttuale'] ?? null,
                            'ordinary_fund' => $data['FondoAmmOrdinario'] ?? null,
                            'accelerated_fund' => $data['FondoAmmAnticipato'] ?? null,
                            'depreciable_residual' => $data['ResiduoAmmortizzabile'] ?? null,
                            'alienation_amount' => $data['Alienazione'] ?? null, 'elimination_amount' => $data['Eliminazione'] ?? null,
                        ];
                        $issue = null;
                    } elseif (str_contains(mb_strtolower($name), 'fattur')) {
                        $kind = 'invoice_index'; $key = hash('sha256', json_encode($data, JSON_THROW_ON_ERROR));
                        $issue = null;
                    } else throw new InvalidArgumentException('Foglio Koinos non riconosciuto: ' . $sheet->getTitle());
                    $result[] = $this->record($kind, $key, $row->getRowIndex(), $data, ['sheet' => $sheet->getTitle(), 'cells' => $values], $issue);
                }
            }
        } finally { $book->disconnectWorksheets(); unset($book); gc_collect_cycles(); }
        if (!$result) throw new InvalidArgumentException('Il foglio non contiene record riconoscibili.');
        return $result;
    }

    private function number(string $value): ?string
    {
        $value = trim(str_replace('%', '', $value));
        if ($value === '') return null;
        $value = str_replace(',', '.', str_replace('.', '', $value));
        if (!is_numeric($value)) throw new InvalidArgumentException('Numero non valido nel tracciato Koinos.');
        return $value;
    }

    private function record(string $kind, string $key, int $line, array $data, array $raw, ?string $issue = null): array
    {
        return compact('kind', 'key', 'line', 'data', 'raw', 'issue');
    }
}
