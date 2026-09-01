<?php

declare(strict_types=1);

namespace Luna\Core;

use PDO;
use Throwable;

final class SystemHealth
{
    private static array $tableCache = [];

    public static function tableExists(PDO $db, string $table): bool
    {
        $cacheKey = spl_object_id($db) . ':' . $table;
        if (array_key_exists($cacheKey, self::$tableCache)) {
            return self::$tableCache[$cacheKey];
        }

        try {
            $statement = $db->prepare(
                'SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?'
            );
            $statement->execute([$table]);
            return self::$tableCache[$cacheKey] = (int) $statement->fetchColumn() > 0;
        } catch (Throwable) {
            return self::$tableCache[$cacheKey] = false;
        }
    }

    public static function migrationStatus(PDO $db, string $basePath): array
    {
        $available = array_map('basename', glob($basePath . '/database/migrations/*.sql') ?: []);
        sort($available);
        $applied = [];
        if (self::tableExists($db, 'migrations')) {
            try {
                $applied = array_map('strval', $db->query('SELECT migration FROM migrations ORDER BY migration')->fetchAll(PDO::FETCH_COLUMN));
            } catch (Throwable) {
                $applied = [];
            }
        }

        return [
            'available' => $available,
            'applied' => $applied,
            'pending' => array_values(array_diff($available, $applied)),
        ];
    }

    public static function featureTables(PDO $db, array $features): array
    {
        $result = [];
        foreach ($features as $key => $feature) {
            $required = $feature['required_tables'] ?? [];
            $missing = array_values(array_filter($required, static fn (string $table): bool => !self::tableExists($db, $table)));
            $result[$key] = ['required' => $required, 'missing' => $missing, 'ready' => $missing === []];
        }
        return $result;
    }

    public static function runtime(string $basePath): array
    {
        $requiredExtensions = ['ctype', 'dom', 'fileinfo', 'gd', 'json', 'libxml', 'mbstring', 'openssl', 'pdo', 'pdo_mysql', 'simplexml', 'sodium', 'zip'];
        $extensions = [];
        foreach ($requiredExtensions as $extension) {
            $extensions[$extension] = extension_loaded($extension);
        }
        $directories = [];
        foreach (['cache', 'imports', 'logs', 'private', 'sessions'] as $directory) {
            $path = $basePath . '/storage/' . $directory;
            $directories[$directory] = is_dir($path) && is_writable($path);
        }
        return ['php' => PHP_VERSION, 'extensions' => $extensions, 'directories' => $directories];
    }
}
