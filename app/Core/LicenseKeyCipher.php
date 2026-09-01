<?php

declare(strict_types=1);

namespace Luna\Core;

use RuntimeException;

final class LicenseKeyCipher
{
    private const CIPHER = 'aes-256-gcm';

    public function encrypt(string $plainText): string
    {
        $plainText = trim($plainText);
        if ($plainText === '') {
            throw new RuntimeException('Chiave licenza vuota.');
        }
        $iv = random_bytes(12);
        $tag = '';
        $cipherText = openssl_encrypt($plainText, self::CIPHER, $this->key(), OPENSSL_RAW_DATA, $iv, $tag, '', 16);
        if ($cipherText === false || strlen($tag) !== 16) {
            throw new RuntimeException('Impossibile cifrare la chiave licenza.');
        }
        return 'v1:' . base64_encode($iv . $tag . $cipherText);
    }

    public function decrypt(string $encoded): string
    {
        if (!str_starts_with($encoded, 'v1:')) {
            throw new RuntimeException('Formato chiave licenza non supportato.');
        }
        $payload = base64_decode(substr($encoded, 3), true);
        if ($payload === false || strlen($payload) < 29) {
            throw new RuntimeException('Chiave licenza cifrata non valida.');
        }
        $plainText = openssl_decrypt(substr($payload, 28), self::CIPHER, $this->key(), OPENSSL_RAW_DATA, substr($payload, 0, 12), substr($payload, 12, 16));
        if ($plainText === false || $plainText === '') {
            throw new RuntimeException('Impossibile decifrare la chiave licenza. Verifica APP_KEY.');
        }
        return $plainText;
    }

    public static function fingerprint(string $licenseKey): string
    {
        return strtoupper(implode('-', str_split(substr(hash('sha256', trim($licenseKey)), 0, 16), 4)));
    }

    public static function hash(string $licenseKey): string
    {
        return hash('sha256', trim($licenseKey));
    }

    private function key(): string
    {
        $configured = trim((string) Env::get('APP_KEY', ''));
        if ($configured === '' || str_starts_with($configured, 'change-')) {
            throw new RuntimeException('APP_KEY non configurata: genera e salva una chiave applicativa prima di attivare la licenza.');
        }
        if (str_starts_with($configured, 'base64:')) {
            $decoded = base64_decode(substr($configured, 7), true);
            if ($decoded !== false && strlen($decoded) >= 32) {
                return substr($decoded, 0, 32);
            }
        }
        return hash('sha256', $configured, true);
    }
}
