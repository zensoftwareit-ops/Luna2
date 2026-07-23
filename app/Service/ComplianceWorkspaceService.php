<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class ComplianceWorkspaceService
{
    private const TYPES = [
        'F24' => 'Deleghe F24',
        'INTRASTAT' => 'Elenchi Intrastat',
        'CU' => 'Certificazione Unica',
        'MODEL_770' => 'Modello 770',
        'IVA_ANNUAL' => 'Dichiarazione IVA annuale',
        'LIPE' => 'Comunicazioni LIPE',
        'XBRL' => 'Bilancio XBRL',
        'REDDITI' => 'Dichiarazione Redditi',
        'IRAP' => 'Dichiarazione IRAP',
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
            'SELECT f.*, u.name AS created_by_name, v.name AS validated_by_name, e.name AS endpoint_name
             FROM compliance_filing_runs f
             LEFT JOIN users u ON u.id = f.created_by
             LEFT JOIN users v ON v.id = f.validated_by
             LEFT JOIN api_endpoint_configs e ON e.id = f.submission_endpoint_id
             WHERE f.organization_id = ? ORDER BY f.period_year DESC, f.id DESC LIMIT 100'
        );
        $statement->execute([$this->organizationId]);
        return $statement->fetchAll();
    }

    public function create(string $type, int $year, ?string $periodCode, ?string $schemaVersion, ?string $notes): int
    {
        if (!isset(self::TYPES[$type]) || $year < 2000 || $year > (int) date('Y') + 1) {
            throw new InvalidArgumentException('Tipo o anno dell’adempimento non valido.');
        }
        $periodCode = $this->nullable($periodCode, 30);
        $payload = $this->snapshot($type, $year, $periodCode);
        $anomalies = $this->anomalies($type, $payload);

        $statement = $this->db->prepare(
            "INSERT INTO compliance_filing_runs
             (organization_id, filing_type, period_year, period_code, status, schema_version, data_json,
              anomaly_count, created_by, notes, created_at, updated_at)
             VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, ?, ?, ?, NOW(), NOW())"
        );
        $statement->execute([
            $this->organizationId, $type, $year, $periodCode, $this->nullable($schemaVersion, 50),
            json_encode($payload, JSON_THROW_ON_ERROR | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            count($anomalies), $this->userId, $this->nullable($notes, 5000),
        ]);
        $id = (int) $this->db->lastInsertId();
        try {
            $this->writeDossier($id, $type, $year, $payload, $anomalies);
        } catch (Throwable $exception) {
            $this->db->prepare(
                "UPDATE compliance_filing_runs
                 SET status = 'CANCELLED', notes = CONCAT(COALESCE(notes,''), ?), updated_at = NOW()
                 WHERE id = ? AND organization_id = ?"
            )->execute(["\nErrore generazione dossier: " . mb_substr($exception->getMessage(), 0, 300), $id, $this->organizationId]);
            throw $exception;
        }
        return $id;
    }

    public function transition(int $id, string $target, array $data): void
    {
        $statement = $this->db->prepare('SELECT * FROM compliance_filing_runs WHERE id = ? AND organization_id = ? FOR UPDATE');
        $this->db->beginTransaction();
        try {
            $statement->execute([$id, $this->organizationId]);
            $row = $statement->fetch();
            if (!$row) {
                throw new InvalidArgumentException('Fascicolo non trovato.');
            }
            $current = (string) $row['status'];
            $allowed = [
                'DRAFT' => ['REVIEW', 'CANCELLED'],
                'REVIEW' => ['VALIDATED', 'DRAFT', 'CANCELLED'],
                'VALIDATED' => ['READY', 'REVIEW', 'CANCELLED'],
                'READY' => ['SUBMITTED', 'CANCELLED'],
                'SUBMITTED' => ['ACCEPTED', 'REJECTED'],
                'REJECTED' => ['REVIEW', 'CANCELLED'],
            ];
            if (!in_array($target, $allowed[$current] ?? [], true)) {
                throw new InvalidArgumentException("Passaggio {$current} → {$target} non consentito.");
            }

            $reference = $this->nullable($data['professional_validation_reference'] ?? null, 190);
            $external = $this->nullable($data['external_reference'] ?? null, 190);
            $endpointId = !empty($data['submission_endpoint_id']) ? (int) $data['submission_endpoint_id'] : null;
            if ($target === 'VALIDATED' && ($reference === null || (int) $row['anomaly_count'] > 0)) {
                throw new InvalidArgumentException('Per validare servono zero anomalie e il riferimento del professionista.');
            }
            if ($target === 'SUBMITTED') {
                if ($external === null || $endpointId === null) {
                    throw new InvalidArgumentException('Registrare endpoint e riferimento esterno dell’invio.');
                }
                $endpoint = $this->db->prepare('SELECT COUNT(*) FROM api_endpoint_configs WHERE id = ? AND organization_id = ? AND enabled = 1');
                $endpoint->execute([$endpointId, $this->organizationId]);
                if (!(int) $endpoint->fetchColumn()) {
                    throw new InvalidArgumentException('Endpoint di trasmissione non valido.');
                }
            }
            if (in_array($target, ['ACCEPTED', 'REJECTED'], true) && $external === null && empty($row['external_reference'])) {
                throw new InvalidArgumentException('Indicare il riferimento dell’esito esterno.');
            }

            $sql = 'UPDATE compliance_filing_runs SET status = ?, updated_at = NOW()';
            $params = [$target];
            if ($target === 'VALIDATED') {
                $sql .= ', professional_validation_reference = ?, validated_by = ?, validated_at = NOW()';
                array_push($params, $reference, $this->userId);
            }
            if ($target === 'READY') {
                $sql .= ', locked_at = NOW()';
            }
            if ($target === 'SUBMITTED') {
                $sql .= ', submission_endpoint_id = ?, external_reference = ?, submitted_at = NOW()';
                array_push($params, $endpointId, $external);
            }
            if ($target === 'ACCEPTED') {
                $sql .= ', external_reference = COALESCE(?, external_reference), accepted_at = NOW(), locked_at = COALESCE(locked_at, NOW())';
                $params[] = $external;
            } elseif ($target === 'REJECTED') {
                $sql .= ', external_reference = COALESCE(?, external_reference)';
                $params[] = $external;
            }
            $sql .= ' WHERE id = ? AND organization_id = ?';
            array_push($params, $id, $this->organizationId);
            $this->db->prepare($sql)->execute($params);
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function dossier(int $id): array
    {
        $statement = $this->db->prepare('SELECT * FROM compliance_filing_runs WHERE id = ? AND organization_id = ?');
        $statement->execute([$id, $this->organizationId]);
        $row = $statement->fetch();
        if (!$row || empty($row['output_path'])) {
            throw new InvalidArgumentException('Dossier non disponibile.');
        }
        $path = $this->basePath . '/' . ltrim((string) $row['output_path'], '/');
        if (!is_file($path) || hash_file('sha256', $path) !== $row['output_sha256']) {
            throw new RuntimeException('Il dossier non supera il controllo di integrità.');
        }
        return [$path, $row];
    }

    private function snapshot(string $type, int $year, ?string $periodCode): array
    {
        $company = $this->db->prepare(
            'SELECT business_name, vat_number, tax_code, fiscal_regime, address, postal_code, city, province, country_code
             FROM organizations WHERE id = ?'
        );
        $company->execute([$this->organizationId]);
        $payload = [
            'generated_at' => date(DATE_ATOM),
            'filing_type' => $type,
            'period_year' => $year,
            'period_code' => $periodCode,
            'company' => $company->fetch() ?: [],
            'disclaimer' => 'Dossier di controllo Luna2. Non è un file telematico ministeriale e non produce invii.',
        ];

        if (in_array($type, ['CU', 'MODEL_770'], true)) {
            $payload['withholdings'] = $this->rows(
                'SELECT withholding_type, COUNT(*) AS records, SUM(gross_amount) AS gross, SUM(withholding_amount) AS withheld,
                        SUM(social_security_amount) AS social_security
                 FROM withholding_records WHERE organization_id = ? AND YEAR(record_date) = ?
                 GROUP BY withholding_type ORDER BY withholding_type',
                [$this->organizationId, $year],
            );
        } elseif ($type === 'F24') {
            $payload['deadlines'] = $this->rows(
                "SELECT due_date, deadline_type, description, reference_period, amount, status
                 FROM tax_deadlines WHERE organization_id = ? AND YEAR(due_date) = ?
                   AND deadline_type IN ('F24','VAT','INPS','WITHHOLDING','INCOME_TAX')
                 ORDER BY due_date",
                [$this->organizationId, $year],
            );
        } elseif ($type === 'LIPE') {
            $payload['communications'] = $this->rows(
                'SELECT * FROM lipe_communications WHERE organization_id = ? AND fiscal_year = ? ORDER BY quarter_number',
                [$this->organizationId, $year],
            );
        } elseif ($type === 'IVA_ANNUAL') {
            $payload['annual_summary'] = $this->rows(
                'SELECT * FROM vat_annual_summaries WHERE organization_id = ? AND fiscal_year = ? ORDER BY id DESC',
                [$this->organizationId, $year],
            );
            $payload['vat_totals'] = $this->rows(
                'SELECT register_type, SUM(taxable_amount) AS taxable, SUM(vat_amount) AS vat, SUM(deductible_vat) AS deductible
                 FROM vat_movements WHERE organization_id = ? AND period_year = ? GROUP BY register_type',
                [$this->organizationId, $year],
            );
        } elseif ($type === 'INTRASTAT') {
            $payload['eu_operations'] = $this->rows(
                "SELECT register_type, operation_type, vat_code, COUNT(*) AS movements,
                        SUM(taxable_amount) AS taxable, SUM(vat_amount) AS vat
                 FROM vat_movements WHERE organization_id = ? AND period_year = ?
                   AND operation_type IN ('REVERSE_CHARGE','INTRA_EU')
                 GROUP BY register_type, operation_type, vat_code",
                [$this->organizationId, $year],
            );
        } else {
            $payload['trial_balance'] = $this->rows(
                "SELECT a.code, a.name, a.account_type, a.classification_code, a.statement_section, a.tax_mapping_code,
                        SUM(l.debit) AS debit, SUM(l.credit) AS credit, SUM(l.debit-l.credit) AS balance
                 FROM journal_entry_lines l
                 INNER JOIN journal_entries e ON e.id = l.journal_entry_id AND e.organization_id = l.organization_id
                 INNER JOIN chart_of_accounts a ON a.id = l.account_id AND a.organization_id = l.organization_id
                 WHERE l.organization_id = ? AND e.status = 'POSTED' AND YEAR(e.entry_date) = ?
                 GROUP BY a.id, a.code, a.name, a.account_type, a.classification_code, a.statement_section, a.tax_mapping_code
                 ORDER BY a.code",
                [$this->organizationId, $year],
            );
        }
        return $payload;
    }

    private function anomalies(string $type, array $payload): array
    {
        $anomalies = [];
        $company = $payload['company'] ?? [];
        if (empty($company['vat_number']) && empty($company['tax_code'])) {
            $anomalies[] = 'Identificativo fiscale aziendale mancante.';
        }
        if (in_array($type, ['XBRL', 'REDDITI', 'IRAP'], true)) {
            foreach ($payload['trial_balance'] ?? [] as $row) {
                if (empty($row['classification_code']) || ($type !== 'XBRL' && empty($row['tax_mapping_code']))) {
                    $anomalies[] = 'Conto ' . ($row['code'] ?? '') . ' privo di classificazione richiesta.';
                }
            }
        }
        if ($type === 'IVA_ANNUAL' && empty($payload['annual_summary'])) {
            $anomalies[] = 'Prospetto IVA annuale non ancora generato.';
        }
        if ($type === 'LIPE' && empty($payload['communications'])) {
            $anomalies[] = 'Nessuna comunicazione LIPE presente per l’anno.';
        }
        if (in_array($type, ['CU', 'MODEL_770'], true) && empty($payload['withholdings'])) {
            $anomalies[] = 'Nessuna ritenuta presente per il periodo.';
        }
        return array_values(array_unique($anomalies));
    }

    private function writeDossier(int $id, string $type, int $year, array $payload, array $anomalies): void
    {
        $directory = $this->basePath . '/storage/private/compliance/' . $this->organizationId;
        if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
            throw new RuntimeException('Impossibile creare l’archivio adempimenti.');
        }
        $path = $directory . '/' . strtolower($type) . '-' . $year . '-' . $id . '.json';
        $content = json_encode(
            ['payload' => $payload, 'anomalies' => $anomalies],
            JSON_THROW_ON_ERROR | JSON_PRETTY_PRINT | JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES,
        );
        if (file_put_contents($path, $content, LOCK_EX) === false) {
            throw new RuntimeException('Impossibile salvare il dossier.');
        }
        $this->db->prepare(
            'UPDATE compliance_filing_runs SET output_path = ?, output_sha256 = ?, anomaly_count = ?, updated_at = NOW()
             WHERE id = ? AND organization_id = ?'
        )->execute([$this->relative($path), hash('sha256', $content), count($anomalies), $id, $this->organizationId]);
    }

    private function rows(string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetchAll();
    }

    private function nullable(mixed $value, int $length): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : mb_substr($value, 0, $length);
    }

    private function relative(string $path): string
    {
        return str_replace('\\', '/', ltrim(str_replace($this->basePath, '', $path), '/\\'));
    }
}
