<?php

declare(strict_types=1);

namespace Luna\Core;

use RuntimeException;

final class LicenseSignatureVerifier
{
    public function verify(array $payload, string $signature): void
    {
        if (!function_exists('sodium_crypto_sign_verify_detached')) {
            throw new RuntimeException('Estensione Sodium non disponibile: impossibile verificare la licenza.');
        }
        $publicKey = $this->decode((string) Env::get('LUNA_LICENSE_PUBLIC_KEY', ''), SODIUM_CRYPTO_SIGN_PUBLICKEYBYTES, 'chiave pubblica');
        $signatureBytes = $this->decode($signature, SODIUM_CRYPTO_SIGN_BYTES, 'firma');
        if (!sodium_crypto_sign_verify_detached($signatureBytes, self::canonicalJson($payload), $publicKey)) {
            throw new RuntimeException('Firma della licenza non valida.');
        }
    }

    public static function canonicalJson(array $payload): string
    {
        $normalize = static function (mixed $value) use (&$normalize): mixed {
            if (!is_array($value)) {
                return $value;
            }
            if (!array_is_list($value)) {
                ksort($value, SORT_STRING);
            }
            foreach ($value as $key => $item) {
                $value[$key] = $normalize($item);
            }
            return $value;
        };
        return json_encode($normalize($payload), JSON_THROW_ON_ERROR | JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
    }

    private function decode(string $encoded, int $expectedLength, string $label): string
    {
        $encoded = trim($encoded);
        if (str_starts_with($encoded, 'base64:')) {
            $encoded = substr($encoded, 7);
        }
        $decoded = base64_decode($encoded, true);
        if ($decoded === false || strlen($decoded) !== $expectedLength) {
            throw new RuntimeException(ucfirst($label) . ' Ed25519 non configurata o non valida.');
        }
        return $decoded;
    }
}
