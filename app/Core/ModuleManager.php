<?php

declare(strict_types=1);

namespace Luna\Core;

use PDO;
use Throwable;

final class ModuleManager
{
    private ?array $states = null;

    public function __construct(
        private readonly PDO $db,
        private readonly array $catalog,
        private readonly int $organizationId,
    ) {
    }

    public function all(): array
    {
        if ($this->states !== null) {
            return $this->states;
        }

        $overrides = [];
        if ($this->organizationId > 0) {
            try {
                $statement = $this->db->prepare('SELECT module_key, enabled FROM module_settings WHERE organization_id = ?');
                $statement->execute([$this->organizationId]);
                foreach ($statement->fetchAll() as $row) {
                    $overrides[(string) $row['module_key']] = (bool) $row['enabled'];
                }
            } catch (Throwable) {
                // Durante il primo setup i default rendono comunque navigabile l'applicazione.
            }
        }

        $this->states = [];
        foreach ($this->catalog as $key => $feature) {
            $this->states[$key] = $feature + [
                'key' => $key,
                'enabled' => $overrides[$key] ?? (bool) ($feature['default'] ?? true),
            ];
        }
        return $this->states;
    }

    public function enabled(string $key): bool
    {
        return (bool) ($this->all()[$key]['enabled'] ?? false);
    }

    public function update(array $enabledKeys): void
    {
        $enabledLookup = array_fill_keys($enabledKeys, true);
        $statement = $this->db->prepare(
            'INSERT INTO module_settings (organization_id, module_key, enabled, created_at, updated_at)
             VALUES (?, ?, ?, NOW(), NOW())
             ON DUPLICATE KEY UPDATE enabled = VALUES(enabled), updated_at = NOW()'
        );
        foreach (array_keys($this->catalog) as $key) {
            $statement->execute([$this->organizationId, $key, isset($enabledLookup[$key]) ? 1 : 0]);
        }
        $this->states = null;
    }
}
