<?php

declare(strict_types=1);

namespace LunaApi\Security;

use InvalidArgumentException;
use LunaApi\Config;

final class ClientRegistry
{
    /** @return array<string, string> */
    public static function credentials(): array
    {
        $clients = self::pairs((string) (getenv('LUNA_API_CLIENTS') ?: ''));
        foreach (array_filter(array_map('trim', explode(',', (string) (getenv('LUNA_API_TOKENS') ?: '')))) as $token) {
            $clients['legacy-' . substr(hash('sha256', $token), 0, 16)] = $token;
        }
        if ($clients === []) {
            Config::string('LUNA_API_CLIENTS');
        }
        return $clients;
    }

    /** @return array<string, string> */
    public static function recipientMap(): array
    {
        $pairs = self::pairs((string) (getenv('SDI_RECIPIENT_MAP') ?: ''));
        $map = [];
        foreach ($pairs as $fiscalId => $clientId) {
            $normalized = strtoupper(preg_replace('/\s+/', '', $fiscalId) ?? '');
            if ($normalized !== '') {
                $map[$normalized] = $clientId;
            }
        }
        return $map;
    }

    /** @return array<string, string> */
    private static function pairs(string $value): array
    {
        $result = [];
        foreach (array_filter(array_map('trim', explode(',', $value))) as $item) {
            if (!str_contains($item, '=')) {
                throw new InvalidArgumentException('Configurazione client non valida: usare chiave=valore.');
            }
            [$key, $secret] = array_map('trim', explode('=', $item, 2));
            if (!preg_match('/^[A-Za-z0-9._-]{2,80}$/', $key) || $secret === '') {
                throw new InvalidArgumentException('Identificativo client o valore non valido.');
            }
            $result[$key] = $secret;
        }
        return $result;
    }
}

