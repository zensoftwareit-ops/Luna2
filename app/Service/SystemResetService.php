<?php

declare(strict_types=1);

namespace Luna\Service;

use PDO;
use RuntimeException;
use Throwable;

final class SystemResetService
{
    private const PROTECTED_TABLES = [
        'migrations',
        'instance_identity',
        'licenses',
        'license_modules',
        'license_module_history',
        'license_sync_logs',
        'license_overrides',
        'custom_domains',
        'custom_domain_events',
    ];

    private const RESET_DIRECTORIES = ['cache', 'imports', 'private', 'exports'];

    public function __construct(
        private readonly PDO $db,
        private readonly string $basePath,
    ) {
    }

    /**
     * @return array{tables_cleared:int,rows_deleted:int,organizations_deleted:int,users_deleted:int,files_deleted:int,file_errors:array<int,string>}
     */
    public function reset(int $superuserId, int $platformOrganizationId, bool $keepUsers): array
    {
        $this->assertSuperuser($superuserId, $platformOrganizationId);
        $tables = $this->applicationTables();
        $tablesCleared = 0;
        $rowsDeleted = 0;
        $organizationsDeleted = 0;
        $usersDeleted = 0;

        $this->db->exec('SET FOREIGN_KEY_CHECKS = 0');
        try {
            $this->db->beginTransaction();
            foreach ($tables as $table) {
                if (in_array($table, self::PROTECTED_TABLES, true)
                    || in_array($table, ['organizations', 'users'], true)) {
                    continue;
                }
                $rowsDeleted += $this->db->exec('DELETE FROM `' . $table . '`');
                $tablesCleared++;
            }

            if (!$keepUsers) {
                if (in_array('licenses', $tables, true)) {
                    $statement = $this->db->prepare(
                        'UPDATE licenses SET licensed_organization_id = NULL
                         WHERE licensed_organization_id IS NOT NULL AND licensed_organization_id <> ?'
                    );
                    $statement->execute([$platformOrganizationId]);
                }

                if (in_array('license_overrides', $tables, true)) {
                    $statement = $this->db->prepare(
                        'UPDATE license_overrides
                         SET created_by = ?, revoked_by = CASE WHEN revoked_by = ? THEN ? ELSE NULL END'
                    );
                    $statement->execute([$superuserId, $superuserId, $superuserId]);
                }

                if (in_array('custom_domains', $tables, true)) {
                    $statement = $this->db->prepare(
                        'UPDATE custom_domains SET created_by = ?, updated_by = ?'
                    );
                    $statement->execute([$superuserId, $superuserId]);
                }

                $statement = $this->db->prepare('DELETE FROM users WHERE id <> ?');
                $statement->execute([$superuserId]);
                $usersDeleted = $statement->rowCount();

                $statement = $this->db->prepare('DELETE FROM organizations WHERE id <> ?');
                $statement->execute([$platformOrganizationId]);
                $organizationsDeleted = $statement->rowCount();
            }

            $statement = $this->db->prepare(
                "INSERT INTO audit_logs
                    (organization_id, user_id, action, entity_type, entity_id, ip_address, user_agent, payload_json, created_at)
                 VALUES (?, ?, 'RESET_SYSTEM', 'system', NULL, ?, ?, ?, NOW())"
            );
            $statement->execute([
                $platformOrganizationId,
                $superuserId,
                substr((string) ($_SERVER['REMOTE_ADDR'] ?? ''), 0, 45),
                substr((string) ($_SERVER['HTTP_USER_AGENT'] ?? ''), 0, 500),
                json_encode([
                    'keep_users' => $keepUsers,
                    'tables_cleared' => $tablesCleared,
                    'rows_deleted' => $rowsDeleted,
                    'users_deleted' => $usersDeleted,
                    'organizations_deleted' => $organizationsDeleted,
                ], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES),
            ]);
            $this->db->commit();
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) {
                $this->db->rollBack();
            }
            throw $exception;
        } finally {
            $this->db->exec('SET FOREIGN_KEY_CHECKS = 1');
        }

        [$filesDeleted, $fileErrors] = $this->clearStoredData();

        return [
            'tables_cleared' => $tablesCleared,
            'rows_deleted' => $rowsDeleted,
            'organizations_deleted' => $organizationsDeleted,
            'users_deleted' => $usersDeleted,
            'files_deleted' => $filesDeleted,
            'file_errors' => $fileErrors,
        ];
    }

    private function assertSuperuser(int $superuserId, int $platformOrganizationId): void
    {
        $statement = $this->db->prepare(
            "SELECT COUNT(*) FROM users
             WHERE id = ? AND organization_id = ? AND role = 'SUPERUSER' AND active = 1"
        );
        $statement->execute([$superuserId, $platformOrganizationId]);
        if ((int) $statement->fetchColumn() !== 1) {
            throw new RuntimeException('Il superuser corrente non è valido: ripristino annullato.');
        }
    }

    /** @return array<int,string> */
    private function applicationTables(): array
    {
        $statement = $this->db->query(
            "SELECT TABLE_NAME
             FROM information_schema.TABLES
             WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'
             ORDER BY TABLE_NAME"
        );
        $tables = array_map('strval', $statement->fetchAll(PDO::FETCH_COLUMN));
        foreach ($tables as $table) {
            if (!preg_match('/^[A-Za-z0-9_]+$/', $table)) {
                throw new RuntimeException('Nome tabella non valido durante il ripristino.');
            }
        }
        return $tables;
    }

    /** @return array{0:int,1:array<int,string>} */
    private function clearStoredData(): array
    {
        $storage = realpath($this->basePath . '/storage');
        if ($storage === false || !is_dir($storage)) {
            return [0, ['Directory storage non disponibile.']];
        }

        $deleted = 0;
        $errors = [];
        foreach (self::RESET_DIRECTORIES as $directory) {
            $path = $storage . DIRECTORY_SEPARATOR . $directory;
            if (!is_dir($path)) {
                continue;
            }
            $this->removeChildren($path, $storage, $deleted, $errors);
        }
        return [$deleted, $errors];
    }

    /** @param array<int,string> $errors */
    private function removeChildren(string $directory, string $storageRoot, int &$deleted, array &$errors): void
    {
        $resolved = realpath($directory);
        if ($resolved === false || !str_starts_with($resolved . DIRECTORY_SEPARATOR, $storageRoot . DIRECTORY_SEPARATOR)) {
            $errors[] = 'Percorso non sicuro ignorato: ' . $directory;
            return;
        }

        $iterator = new \FilesystemIterator($resolved, \FilesystemIterator::SKIP_DOTS);
        foreach ($iterator as $item) {
            $path = $item->getPathname();
            if ($item->isLink() || $item->isFile()) {
                if (@unlink($path)) {
                    $deleted++;
                } else {
                    $errors[] = 'Impossibile eliminare ' . $path;
                }
                continue;
            }
            if ($item->isDir()) {
                $this->removeChildren($path, $storageRoot, $deleted, $errors);
                if (!@rmdir($path)) {
                    $errors[] = 'Impossibile eliminare la directory ' . $path;
                }
            }
        }
    }
}
