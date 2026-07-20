<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use RuntimeException;
use Throwable;

final class AccountingSetupService
{
    public const MAPPING_LABELS = [
        'TRADE_RECEIVABLES' => 'Crediti verso clienti',
        'TRADE_PAYABLES' => 'Debiti verso fornitori',
        'BANK' => 'Banca predefinita',
        'CASH' => 'Cassa',
        'VAT_RECEIVABLE' => 'IVA a credito',
        'VAT_PAYABLE' => 'IVA a debito',
        'VAT_CLEARING' => 'Erario conto IVA',
        'SALES_REVENUE' => 'Ricavi vendite',
        'PURCHASE_COSTS' => 'Costi acquisti',
        'PAYMENT_DIFFERENCES' => 'Abbuoni e differenze',
        'PROFIT_LOSS' => 'Risultato d’esercizio',
        'RETAINED_EARNINGS' => 'Utili/perdite portati a nuovo',
        'OPENING_BALANCE' => 'Bilancio di apertura',
        'DEPRECIATION_EXPENSE' => 'Ammortamenti',
        'ACCUMULATED_DEPRECIATION' => 'Fondo ammortamento',
        'WITHHOLDING_PAYABLE' => 'Erario conto ritenute',
        'SOCIAL_SECURITY_PAYABLE' => 'Enti previdenziali',
        'ACCRUED_EXPENSES' => 'Ratei passivi',
        'PREPAID_EXPENSES' => 'Risconti attivi',
        'ACCRUED_INCOME' => 'Ratei attivi',
        'DEFERRED_INCOME' => 'Risconti passivi',
    ];

    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    public function saveAccount(array $data, ?int $accountId = null): int
    {
        $code = trim((string) ($data['code'] ?? ''));
        $name = trim((string) ($data['name'] ?? ''));
        $type = strtoupper((string) ($data['account_type'] ?? ''));
        $normal = strtoupper((string) ($data['normal_balance'] ?? ''));
        $parentId = !empty($data['parent_id']) ? (int) $data['parent_id'] : null;
        if ($code === '' || $name === '' || !preg_match('/^[A-Za-z0-9._\/-]{1,50}$/', $code)) {
            throw new InvalidArgumentException('Codice e descrizione del conto non sono validi.');
        }
        if (!in_array($type, ['ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'], true)
            || !in_array($normal, ['DEBIT', 'CREDIT'], true)) {
            throw new InvalidArgumentException('Tipo o saldo naturale del conto non validi.');
        }
        if ($parentId !== null) {
            $parent = $this->db->prepare('SELECT id FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
            $parent->execute([$parentId, $this->organizationId]);
            if (!$parent->fetchColumn() || $parentId === $accountId) {
                throw new InvalidArgumentException('Conto padre non valido.');
            }
            if ($accountId !== null && $this->isDescendant($parentId, $accountId)) {
                throw new InvalidArgumentException('La gerarchia del piano dei conti creerebbe un ciclo.');
            }
        }

        $values = [
            $code, $name, $type, $normal, $parentId,
            $this->nullable($data['classification_code'] ?? null),
            $this->nullable($data['statement_section'] ?? null),
            $this->nullable($data['tax_mapping_code'] ?? null),
            isset($data['is_postable']) ? 1 : 0,
            isset($data['requires_party']) ? 1 : 0,
            isset($data['requires_cost_center']) ? 1 : 0,
            isset($data['active']) ? 1 : 0,
        ];
        if ($accountId !== null) {
            $current = $this->account($accountId);
            if ((int) $current['locked'] === 1 && ($current['code'] !== $code || $current['account_type'] !== $type)) {
                throw new RuntimeException('Codice e tipo di un conto di sistema non possono essere modificati.');
            }
            $used = $this->db->prepare('SELECT COUNT(*) FROM journal_entry_lines WHERE organization_id = ? AND account_id = ?');
            $used->execute([$this->organizationId, $accountId]);
            if ((int) $used->fetchColumn() > 0 && ($current['account_type'] !== $type || $current['normal_balance'] !== $normal)) {
                throw new RuntimeException('Tipo e saldo naturale non sono modificabili dopo la prima movimentazione.');
            }
            $statement = $this->db->prepare(
                'UPDATE chart_of_accounts SET code = ?, name = ?, account_type = ?, normal_balance = ?, parent_id = ?,
                 classification_code = ?, statement_section = ?, tax_mapping_code = ?, is_postable = ?, requires_party = ?,
                 requires_cost_center = ?, active = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            );
            $statement->execute(array_merge($values, [$this->userId, $accountId, $this->organizationId]));
            return $accountId;
        }

        $statement = $this->db->prepare(
            'INSERT INTO chart_of_accounts
             (organization_id, code, name, account_type, normal_balance, parent_id, classification_code,
              statement_section, tax_mapping_code, is_postable, requires_party, requires_cost_center,
              active, locked, created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?, NOW(), NOW())'
        );
        $statement->execute(array_merge([$this->organizationId], $values, [$this->userId, $this->userId]));
        return (int) $this->db->lastInsertId();
    }

    public function toggleAccount(int $accountId): bool
    {
        $account = $this->account($accountId);
        if ((int) $account['locked'] === 1 && (int) $account['active'] === 1) {
            throw new RuntimeException('Un conto di sistema non può essere disattivato finché è associato a un automatismo.');
        }
        $statement = $this->db->prepare(
            'UPDATE chart_of_accounts SET active = IF(active = 1, 0, 1), updated_by = ?, updated_at = NOW()
             WHERE id = ? AND organization_id = ?'
        );
        $statement->execute([$this->userId, $accountId, $this->organizationId]);
        return $statement->rowCount() > 0;
    }

    public function saveSettings(array $data): void
    {
        $basis = strtoupper((string) ($data['accounting_basis'] ?? 'ACCRUAL'));
        $periodicity = strtoupper((string) ($data['vat_periodicity'] ?? 'MONTHLY'));
        $month = (int) ($data['fiscal_year_start_month'] ?? 1);
        $lag = (int) ($data['third_party_accounting_lag'] ?? 0);
        $proRata = $this->decimal($data['pro_rata_percent'] ?? 100);
        $lipeSchema = trim((string) ($data['lipe_schema_version'] ?? '2026-draft'));
        $annualSchema = trim((string) ($data['vat_return_schema_version'] ?? 'IVA26-draft'));
        if (!in_array($basis, ['ACCRUAL', 'CASH'], true) || !in_array($periodicity, ['MONTHLY', 'QUARTERLY'], true)
            || $month < 1 || $month > 12 || $lag < 0 || $lag > 2 || $proRata < 0 || $proRata > 100
            || $lipeSchema === '' || strlen($lipeSchema) > 20 || $annualSchema === '' || strlen($annualSchema) > 20) {
            throw new InvalidArgumentException('Configurazione contabile non valida.');
        }
        $statement = $this->db->prepare(
            'INSERT INTO accounting_settings
             (organization_id, accounting_basis, vat_periodicity, fiscal_year_start_month, third_party_accounting_lag,
              pro_rata_percent, cash_vat_enabled, auto_post_documents, auto_create_open_items,
              lipe_schema_version, vat_return_schema_version, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE accounting_basis = VALUES(accounting_basis), vat_periodicity = VALUES(vat_periodicity),
              fiscal_year_start_month = VALUES(fiscal_year_start_month), third_party_accounting_lag = VALUES(third_party_accounting_lag),
              pro_rata_percent = VALUES(pro_rata_percent), cash_vat_enabled = VALUES(cash_vat_enabled),
              auto_post_documents = VALUES(auto_post_documents), auto_create_open_items = VALUES(auto_create_open_items),
              lipe_schema_version = VALUES(lipe_schema_version), vat_return_schema_version = VALUES(vat_return_schema_version), updated_at = NOW()'
        );
        $statement->execute([
            $this->organizationId, $basis, $periodicity, $month, $lag, round($proRata, 4),
            isset($data['cash_vat_enabled']) ? 1 : 0, isset($data['auto_post_documents']) ? 1 : 0,
            isset($data['auto_create_open_items']) ? 1 : 0, $lipeSchema, $annualSchema,
        ]);
    }

    public function saveRegister(array $data, ?int $registerId = null): int
    {
        $code = strtoupper(trim((string) ($data['code'] ?? '')));
        $name = trim((string) ($data['name'] ?? ''));
        $type = strtoupper((string) ($data['register_type'] ?? ''));
        if (!preg_match('/^[A-Z0-9._-]{1,20}$/', $code) || $name === ''
            || !in_array($type, ['SALES', 'PURCHASES', 'CORRISPETTIVI'], true)) {
            throw new InvalidArgumentException('Dati del sezionale IVA non validi.');
        }
        $values = [
            $code, $name, $type, trim((string) ($data['prefix'] ?? '')), trim((string) ($data['suffix'] ?? '')),
            max(1, (int) ($data['next_protocol'] ?? 1)), min(12, max(1, (int) ($data['padding'] ?? 6))),
            isset($data['is_default']) ? 1 : 0, isset($data['active']) ? 1 : 0,
        ];
        $ownsTransaction = !$this->db->inTransaction();
        if ($ownsTransaction) {
            $this->db->beginTransaction();
        }
        try {
            if ($registerId !== null) {
                $statement = $this->db->prepare(
                    'UPDATE vat_registers SET code = ?, name = ?, register_type = ?, prefix = ?, suffix = ?, next_protocol = ?,
                     padding = ?, is_default = ?, active = ?, updated_by = ?, updated_at = NOW()
                     WHERE id = ? AND organization_id = ?'
                );
                $statement->execute(array_merge($values, [$this->userId, $registerId, $this->organizationId]));
                if ($statement->rowCount() === 0) {
                    $exists = $this->db->prepare('SELECT 1 FROM vat_registers WHERE id = ? AND organization_id = ?');
                    $exists->execute([$registerId, $this->organizationId]);
                    if (!$exists->fetchColumn()) {
                        throw new InvalidArgumentException('Sezionale IVA non trovato.');
                    }
                }
                $savedId = $registerId;
            } else {
                $statement = $this->db->prepare(
                    'INSERT INTO vat_registers
                     (organization_id, code, name, register_type, prefix, suffix, next_protocol, padding, is_default, active,
                      created_by, updated_by, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
                );
                $statement->execute(array_merge([$this->organizationId], $values, [$this->userId, $this->userId]));
                $savedId = (int) $this->db->lastInsertId();
            }
            if (isset($data['is_default'])) {
                $this->db->prepare('UPDATE vat_registers SET is_default = (id = ?) WHERE organization_id = ? AND register_type = ?')
                    ->execute([$savedId, $this->organizationId, $type]);
            }
            if ($ownsTransaction) {
                $this->db->commit();
            }
            return $savedId;
        } catch (Throwable $exception) {
            if ($ownsTransaction && $this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        }
    }

    public function saveCause(array $data, ?int $causeId = null): int
    {
        $code = strtoupper(trim((string) ($data['code'] ?? '')));
        $name = trim((string) ($data['name'] ?? ''));
        $category = strtoupper((string) ($data['category'] ?? 'GENERAL'));
        $categories = ['GENERAL', 'SALES', 'PURCHASE', 'RECEIPT', 'PAYMENT', 'ADJUSTMENT', 'CLOSING', 'OPENING', 'DEPRECIATION', 'WITHHOLDING'];
        if (!preg_match('/^[A-Z0-9._-]{1,20}$/', $code) || $name === '' || !in_array($category, $categories, true)) {
            throw new InvalidArgumentException('Dati della causale non validi.');
        }
        $values = [
            $code, $name, $category, !empty($data['vat_register_id']) ? (int) $data['vat_register_id'] : null,
            $this->nullable($data['default_description'] ?? null), $this->nullable($data['document_type'] ?? null),
            isset($data['creates_open_item']) ? 1 : 0, isset($data['automatic']) ? 1 : 0,
            isset($data['active']) ? 1 : 0,
        ];
        if ($causeId !== null) {
            $statement = $this->db->prepare(
                'UPDATE accounting_causes SET code = ?, name = ?, category = ?, vat_register_id = ?, default_description = ?,
                 document_type = ?, creates_open_item = ?, automatic = ?, active = ?, updated_by = ?, updated_at = NOW()
                 WHERE id = ? AND organization_id = ?'
            );
            $statement->execute(array_merge($values, [$this->userId, $causeId, $this->organizationId]));
            return $causeId;
        }
        $statement = $this->db->prepare(
            'INSERT INTO accounting_causes
             (organization_id, code, name, category, vat_register_id, default_description, document_type,
              creates_open_item, automatic, active, created_by, updated_by, created_at, updated_at)
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())'
        );
        $statement->execute(array_merge([$this->organizationId], $values, [$this->userId, $this->userId]));
        return (int) $this->db->lastInsertId();
    }

    public function saveMapping(string $mappingKey, int $accountId): void
    {
        if (!isset(self::MAPPING_LABELS[$mappingKey])) {
            throw new InvalidArgumentException('Automatismo contabile non riconosciuto.');
        }
        $account = $this->account($accountId);
        if ((int) $account['is_postable'] !== 1 || (int) $account['active'] !== 1) {
            throw new InvalidArgumentException('Il conto scelto non è attivo o movimentabile.');
        }
        $statement = $this->db->prepare(
            'INSERT INTO accounting_account_mappings (organization_id, mapping_key, account_id, description, created_at, updated_at)
             VALUES (?, ?, ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE account_id = VALUES(account_id), description = VALUES(description), updated_at = NOW()'
        );
        $statement->execute([$this->organizationId, $mappingKey, $accountId, self::MAPPING_LABELS[$mappingKey]]);
    }

    private function account(int $accountId): array
    {
        $statement = $this->db->prepare('SELECT * FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
        $statement->execute([$accountId, $this->organizationId]);
        $account = $statement->fetch();
        return $account ?: throw new InvalidArgumentException('Conto non trovato.');
    }

    private function isDescendant(int $candidateParentId, int $accountId): bool
    {
        $seen = [];
        $current = $candidateParentId;
        while ($current > 0 && !isset($seen[$current])) {
            if ($current === $accountId) {
                return true;
            }
            $seen[$current] = true;
            $statement = $this->db->prepare('SELECT parent_id FROM chart_of_accounts WHERE id = ? AND organization_id = ?');
            $statement->execute([$current, $this->organizationId]);
            $current = (int) $statement->fetchColumn();
        }
        return false;
    }

    private function nullable(mixed $value): ?string
    {
        $value = trim((string) $value);
        return $value === '' ? null : $value;
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
}
