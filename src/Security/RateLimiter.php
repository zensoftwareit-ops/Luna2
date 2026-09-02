<?php

declare(strict_types=1);

namespace LunaApi\Security;

final class RateLimiter
{
    public function __construct(private readonly string $directory, private readonly int $limit)
    {
        if (!is_dir($directory)) {
            mkdir($directory, 0770, true);
        }
    }

    public function consume(string $clientKey): bool
    {
        $window = intdiv(time(), 60);
        $file = $this->directory . '/rate-' . hash('sha256', $clientKey . ':' . $window) . '.txt';
        $handle = fopen($file, 'c+');
        if ($handle === false) {
            return false;
        }
        try {
            if (!flock($handle, LOCK_EX)) {
                return false;
            }
            $count = (int) trim((string) stream_get_contents($handle));
            if ($count >= $this->limit) {
                return false;
            }
            rewind($handle);
            ftruncate($handle, 0);
            fwrite($handle, (string) ($count + 1));
            fflush($handle);
            flock($handle, LOCK_UN);
            return true;
        } finally {
            fclose($handle);
        }
    }
}

