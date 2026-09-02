<?php

declare(strict_types=1);

namespace LunaApi;

use RuntimeException;

final class Config
{
    private static bool $loaded = false;

    public static function load(string $root): void
    {
        if (self::$loaded) {
            return;
        }
        self::$loaded = true;

        $file = $root . '/.env';
        if (!is_file($file)) {
            return;
        }

        foreach (file($file, FILE_IGNORE_NEW_LINES | FILE_SKIP_EMPTY_LINES) ?: [] as $line) {
            $line = trim($line);
            if ($line === '' || str_starts_with($line, '#') || !str_contains($line, '=')) {
                continue;
            }
            [$name, $value] = array_map('trim', explode('=', $line, 2));
            if (!preg_match('/^[A-Z][A-Z0-9_]*$/', $name) || getenv($name) !== false) {
                continue;
            }
            if (strlen($value) >= 2 && (($value[0] === '"' && str_ends_with($value, '"')) || ($value[0] === "'" && str_ends_with($value, "'")))) {
                $value = substr($value, 1, -1);
            }
            putenv($name . '=' . $value);
        }
    }

    public static function string(string $name, ?string $default = null): string
    {
        $value = getenv($name);
        if ($value === false || trim($value) === '') {
            if ($default !== null) {
                return $default;
            }
            throw new RuntimeException('Configurazione mancante: ' . $name);
        }
        return trim($value);
    }

    public static function integer(string $name, int $default, int $min = 1): int
    {
        $value = getenv($name);
        if ($value === false || $value === '') {
            return $default;
        }
        if (filter_var($value, FILTER_VALIDATE_INT) === false || (int) $value < $min) {
            throw new RuntimeException('Configurazione non valida: ' . $name);
        }
        return (int) $value;
    }

    public static function boolean(string $name, bool $default = false): bool
    {
        $value = getenv($name);
        if ($value === false || $value === '') {
            return $default;
        }
        return filter_var($value, FILTER_VALIDATE_BOOL, FILTER_NULL_ON_FAILURE) ?? $default;
    }
}

