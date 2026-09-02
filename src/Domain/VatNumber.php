<?php

declare(strict_types=1);

namespace LunaApi\Domain;

use InvalidArgumentException;

final class VatNumber
{
    public static function italian(string $value): string
    {
        $value = strtoupper(trim($value));
        if (str_starts_with($value, 'IT')) {
            $value = substr($value, 2);
        }
        $value = preg_replace('/[\s.\-]/', '', $value) ?? '';
        if (!preg_match('/^\d{11}$/', $value) || !self::hasValidItalianChecksum($value)) {
            throw new InvalidArgumentException('Partita IVA italiana non valida.');
        }
        return $value;
    }

    private static function hasValidItalianChecksum(string $vat): bool
    {
        $sum = 0;
        for ($i = 0; $i < 10; $i++) {
            $digit = (int) $vat[$i];
            if ($i % 2 === 1) {
                $digit *= 2;
                if ($digit > 9) {
                    $digit -= 9;
                }
            }
            $sum += $digit;
        }
        return ((10 - ($sum % 10)) % 10) === (int) $vat[10];
    }
}

