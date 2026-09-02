<?php

declare(strict_types=1);

namespace LunaApi\Cache;

final class FileCache
{
    public function __construct(private readonly string $directory)
    {
        if (!is_dir($directory)) {
            mkdir($directory, 0770, true);
        }
    }

    /** @return array<string, mixed>|null */
    public function get(string $key): ?array
    {
        $file = $this->path($key);
        if (!is_file($file)) {
            return null;
        }
        $payload = json_decode((string) file_get_contents($file), true);
        if (!is_array($payload) || (int) ($payload['expires_at'] ?? 0) < time() || !is_array($payload['value'] ?? null)) {
            @unlink($file);
            return null;
        }
        return $payload['value'];
    }

    /** @param array<string, mixed> $value */
    public function put(string $key, array $value, int $ttlSeconds): void
    {
        $file = $this->path($key);
        $temporary = $file . '.' . bin2hex(random_bytes(4)) . '.tmp';
        $json = json_encode(['expires_at' => time() + $ttlSeconds, 'value' => $value], JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        if ($json !== false && file_put_contents($temporary, $json, LOCK_EX) !== false) {
            rename($temporary, $file);
        }
        if (is_file($temporary)) {
            @unlink($temporary);
        }
    }

    private function path(string $key): string
    {
        return $this->directory . '/' . hash('sha256', $key) . '.json';
    }
}

