<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class AssetService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function saveCategory(array $data, ?int $categoryId = null): int
    {
        $code = strtoupper(trim((string) ($data['code'] ?? '')));
        $name = trim((string) ($data['name'] ?? ''));
        $civil = $this->rate($data['civil_rate'] ?? 0);
        $tax = $this->rate($data['tax_rate'] ?? 0);
        $first = $this->rate($data['first_year_percent'] ?? 50);
        if (!preg_match('/^[A-Z0-9._-]{1,30}$/', $code) || $name === '' || $civil <= 0 || $tax <= 0) {
            throw new InvalidArgumentException('Categoria cespite non valida.');
        }
        $values = [
            $code, $name, $civil, $tax, $first,
            !empty($data['asset_account_id']) ? (int) $data['asset_account_id'] : null,
            !empty($data['depreciation_expense_account_id']) ? (int) $data['depreciation_expense_account_id'] : null,
            !empty($data['accumulated_depreciation_account_id']) ? (int) $data['accumulated_depreciation_account_id'] : null,
            isset($data['active']) ? 1 : 0,
        ];
        if ($categoryId !== null) {
            $statement = $this->db->prepare(
                'UPDATE fixed_asset_categories SET code = ?, name = ?, civil_rate = ?, tax_rate = ?, first_year_percent = ?,
                 asset_account_id = ?, depreciation_expense_account_id = ?, accumulated_depreciation_account_id = ?,
                 active = ?, updated_at = NOW() WHERE id = ? AND organization_id = ?'
            );
            $statement->execute(array_merge($values, [$categoryId, $this->organizationId]));
            return $categoryId;
        }
        $statement = $this->db->prepare(
            'INSERT INTO fixed_asset_categories
             (organization_id, code, name, civil_rate, tax_rate, first_year_percent, asset_account_id,
              depreciation_expense_account_id, accumulated_depreciation_account_id, active, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
        );
        $statement->execute(array_merge([$this->organizationId], $values));
        return (int) $this->db->lastInsertId();
    }

    public function saveAsset(array $data, ?int $assetId = null): int
    {
        $code = strtoupper(trim((string) ($data['asset_code'] ?? '')));
        $description = trim((string) ($data['description'] ?? ''));
        $purchaseDate = $this->date((string) ($data['purchase_date'] ?? ''));
        $cost = round($this->amount($data['purchase_cost'] ?? 0), 2);
        $residual = round(max(0, $this->amount($data['residual_value'] ?? 0)), 2);
        $categoryId = !empty($data['category_id']) ? (int) $data['category_id'] : null;
        if ($code === '' || $description === '' || $cost <= 0 || $residual > $cost) {
            throw new InvalidArgumentException('Dati del cespite non validi.');
        }
        $category = null;
        if ($categoryId !== null) {
            $statement = $this->db->prepare('SELECT * FROM fixed_asset_categories WHERE id = ? AND organization_id = ? AND active = 1');
            $statement->execute([$categoryId, $this->organizationId]);
            $category = $statement->fetch() ?: throw new InvalidArgumentException('Categoria cespite non valida.');
        }
        $civilRate = $this->rate($data['civil_depreciation_rate'] ?? ($category['civil_rate'] ?? $data['depreciation_rate'] ?? 0));
        $taxRate = $this->rate($data['tax_depreciation_rate'] ?? ($category['tax_rate'] ?? $civilRate));
        $first = $this->rate($data['first_year_percent'] ?? ($category['first_year_percent'] ?? 50));
        if ($civilRate <= 0 || $taxRate <= 0) {
            throw new InvalidArgumentException('Le aliquote civilistica e fiscale devono essere maggiori di zero.');
        }
        $status = strtoupper((string) ($data['status'] ?? 'ACTIVE'));
        if (!in_array($status, ['ACTIVE','DISPOSED','SOLD'], true)) {
            throw new InvalidArgumentException('Stato cespite non valido.');
        }
        $values = [
            $code, $description, trim((string) ($category['name'] ?? $data['category'] ?? '')) ?: null, $categoryId,
            !empty($data['supplier_id']) ? (int) $data['supplier_id'] : null,
            !empty($data['document_id']) ? (int) $data['document_id'] : null,
            $purchaseDate, !empty($data['in_service_date']) ? $this->date((string) $data['in_service_date']) : $purchaseDate,
            $cost, $residual, $civilRate, $civilRate, $taxRate, $first, $status, $this->userId,
        ];
        if ($assetId !== null) {
            $this->asset($assetId, false);
            $statement = $this->db->prepare(
                'UPDATE fixed_assets SET asset_code = ?, description = ?, category = ?, category_id = ?, supplier_id = ?, document_id = ?,
                 purchase_date = ?, in_service_date = ?, purchase_cost = ?, residual_value = ?, depreciation_rate = ?,
                 civil_depreciation_rate = ?, tax_depreciation_rate = ?, first_year_percent = ?, status = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            );
            $statement->execute(array_merge($values, [$assetId, $this->organizationId]));
            return $assetId;
        }
        $statement = $this->db->prepare(
            'INSERT INTO fixed_assets
             (organization_id, asset_code, description, category, category_id, supplier_id, document_id, purchase_date,
              in_service_date, purchase_cost, residual_value, depreciation_rate, civil_depreciation_rate,
              tax_depreciation_rate, first_year_percent, accumulated_depreciation, net_book_value,
              tax_accumulated_depreciation, tax_net_value, status, created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, 0, ?, ?, ?, ?, NOW(), NOW())'
        );
        $statement->execute(array_merge(
            [$this->organizationId], array_slice($values, 0, 14), [$cost, $cost, $status, $this->userId, $this->userId]
        ));
        return (int) $this->db->lastInsertId();
    }

    public function calculate(int $assetId, int $year): int
    {
        if ($year < 2000 || $year > 2200) {
            throw new InvalidArgumentException('Esercizio ammortamento non valido.');
        }
        $asset = $this->asset($assetId, false);
        if ($asset['status'] !== 'ACTIVE') {
            throw new RuntimeException('Il cespite non è attivo.');
        }
        $start = new DateTimeImmutable((string) ($asset['in_service_date'] ?: $asset['purchase_date']));
        if ((int) $start->format('Y') > $year) {
            throw new RuntimeException('Il cespite non era ancora entrato in funzione nell’esercizio selezionato.');
        }
        $base = max(0, (float) $asset['purchase_cost'] - (float) $asset['residual_value']);
        $civilRate = (float) ($asset['civil_depreciation_rate'] ?: $asset['depreciation_rate']);
        $taxRate = (float) ($asset['tax_depreciation_rate'] ?: $asset['depreciation_rate']);
        $firstFactor = (int) $start->format('Y') === $year ? (float) $asset['first_year_percent'] / 100 : 1.0;
        $civil = min(max(0, (float) $asset['net_book_value'] - (float) $asset['residual_value']), round($base * $civilRate / 100 * $firstFactor, 2));
        $tax = min(max(0, (float) $asset['tax_net_value'] - (float) $asset['residual_value']), round($base * $taxRate / 100 * $firstFactor, 2));
        if ($civil <= .005 && $tax <= .005) {
            throw new RuntimeException('Il cespite è già completamente ammortizzato.');
        }
        $existing = $this->db->prepare('SELECT status FROM depreciation_entries WHERE fixed_asset_id = ? AND fiscal_year = ?');
        $existing->execute([$assetId, $year]);
        if ($existing->fetchColumn() === 'POSTED') {
            throw new RuntimeException('La quota dell’esercizio è già contabilizzata e non può essere ricalcolata.');
        }
        $statement = $this->db->prepare(
            "INSERT INTO depreciation_entries
             (organization_id, fixed_asset_id, fiscal_year, period_start, period_end, amount, civil_amount, tax_amount,
              status, created_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', NOW())
             ON DUPLICATE KEY UPDATE period_start = VALUES(period_start), period_end = VALUES(period_end),
              amount = VALUES(amount), civil_amount = VALUES(civil_amount), tax_amount = VALUES(tax_amount),
              status = IF(status = 'POSTED', status, 'DRAFT')"
        );
        $statement->execute([
            $this->organizationId, $assetId, $year, sprintf('%04d-01-01', $year), sprintf('%04d-12-31', $year),
            $civil, $civil, $tax,
        ]);
        $find = $this->db->prepare('SELECT id FROM depreciation_entries WHERE fixed_asset_id = ? AND fiscal_year = ?');
        $find->execute([$assetId, $year]);
        return (int) $find->fetchColumn();
    }

    public function postDepreciation(int $entryId): int
    {
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            $statement = $this->db->prepare(
                "SELECT d.*, a.description, a.net_book_value, a.tax_net_value, a.accumulated_depreciation,
                        a.tax_accumulated_depreciation, c.depreciation_expense_account_id,
                        c.accumulated_depreciation_account_id
                 FROM depreciation_entries d JOIN fixed_assets a ON a.id = d.fixed_asset_id
                 LEFT JOIN fixed_asset_categories c ON c.id = a.category_id
                 WHERE d.id = ? AND d.organization_id = ? AND d.status = 'DRAFT' FOR UPDATE"
            );
            $statement->execute([$entryId, $this->organizationId]);
            $row = $statement->fetch();
            if (!$row) {
                throw new InvalidArgumentException('Quota di ammortamento non trovata o già contabilizzata.');
            }
            $expense = (int) ($row['depreciation_expense_account_id'] ?: $this->mapping('DEPRECIATION_EXPENSE'));
            $fund = (int) ($row['accumulated_depreciation_account_id'] ?: $this->mapping('ACCUMULATED_DEPRECIATION'));
            $amount = round((float) $row['civil_amount'], 2);
            $journalId = (new AccountingService($this->db, $this->organizationId, $this->userId))->postAutomated([
                'entry_date' => $row['period_end'], 'competence_date' => $row['period_end'],
                'entry_type' => 'DEPRECIATION', 'description' => 'Ammortamento ' . $row['description'] . ' ' . $row['fiscal_year'],
            ], [
                ['account_id' => $expense, 'debit' => $amount, 'credit' => 0, 'description' => $row['description']],
                ['account_id' => $fund, 'debit' => 0, 'credit' => $amount, 'description' => $row['description']],
            ], 'DEPRECIATION_ENTRY', $entryId);
            $this->db->prepare(
                "UPDATE depreciation_entries SET status = 'POSTED', journal_entry_id = ?, posted_at = NOW() WHERE id = ?"
            )->execute([$journalId, $entryId]);
            $this->db->prepare(
                'UPDATE fixed_assets SET accumulated_depreciation = accumulated_depreciation + ?,
                 net_book_value = GREATEST(residual_value, net_book_value - ?),
                 tax_accumulated_depreciation = tax_accumulated_depreciation + ?,
                 tax_net_value = GREATEST(residual_value, tax_net_value - ?), updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            )->execute([
                $amount, $amount, $row['tax_amount'], $row['tax_amount'], $this->userId,
                $row['fixed_asset_id'], $this->organizationId,
            ]);
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $journalId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    private function asset(int $assetId, bool $forUpdate): array
    {
        $statement = $this->db->prepare(
            'SELECT * FROM fixed_assets WHERE id = ? AND organization_id = ?' . ($forUpdate ? ' FOR UPDATE' : '')
        );
        $statement->execute([$assetId, $this->organizationId]);
        $asset = $statement->fetch();
        return $asset ?: throw new InvalidArgumentException('Cespite non trovato.');
    }

    private function mapping(string $key): int
    {
        $statement = $this->db->prepare('SELECT account_id FROM accounting_account_mappings WHERE organization_id = ? AND mapping_key = ?');
        $statement->execute([$this->organizationId, $key]);
        $id = (int) $statement->fetchColumn();
        return $id > 0 ? $id : throw new RuntimeException('Manca il conto automatico “' . $key . '”.');
    }

    private function rate(mixed $value): float
    {
        $value = str_replace(',', '.', trim((string) $value));
        $rate = (float) $value;
        return min(100, max(0, $rate));
    }

    private function amount(mixed $value): float
    {
        $value = trim((string) $value);
        if (str_contains($value, ',')) {
            $value = str_replace('.', '', $value);
            $value = str_replace(',', '.', $value);
        }
        return (float) $value;
    }

    private function date(string $value): string
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $value);
        $errors = DateTimeImmutable::getLastErrors();
        if ($date === false || ($errors !== false && ($errors['warning_count'] > 0 || $errors['error_count'] > 0))) {
            throw new InvalidArgumentException('Data cespite non valida.');
        }
        return $date->format('Y-m-d');
    }
}
