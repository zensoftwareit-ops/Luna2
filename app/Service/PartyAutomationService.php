<?php

declare(strict_types=1);

namespace Luna\Service;

use DateTimeImmutable;
use InvalidArgumentException;

final class PartyAutomationService
{
    public static function normalizeVat(mixed $value, string $country = 'IT'): ?string
    {
        $vat = strtoupper(preg_replace('/[^A-Z0-9]/', '', trim((string) $value)) ?? '');
        $country = strtoupper(trim($country)) ?: 'IT';
        if ($country === 'IT' && str_starts_with($vat, 'IT')) {
            $vat = substr($vat, 2);
        }
        return $vat === '' ? null : $vat;
    }

    public static function validateItalianVat(?string $vat): bool
    {
        if ($vat === null || !preg_match('/^\d{11}$/', $vat)) {
            return false;
        }
        $sum = 0;
        for ($i = 0; $i < 10; $i++) {
            $digit = (int) $vat[$i];
            if ($i % 2 === 1) {
                $digit *= 2;
                if ($digit > 9) { $digit -= 9; }
            }
            $sum += $digit;
        }
        return (10 - ($sum % 10)) % 10 === (int) $vat[10];
    }

    public static function dueDate(string $documentDate, array $party): string
    {
        $date = DateTimeImmutable::createFromFormat('!Y-m-d', $documentDate);
        if (!$date) {
            throw new InvalidArgumentException('Data documento non valida.');
        }
        $days = max(0, min(3650, (int) ($party['payment_days'] ?? self::daysFromLabel((string) ($party['payment_terms'] ?? '30')))));
        $monthEnd = !empty($party['payment_month_end']) || self::labelIsMonthEnd((string) ($party['payment_terms'] ?? ''));
        if ($monthEnd) {
            $date = $date->modify('last day of this month');
        }
        return $date->modify('+' . $days . ' days')->format('Y-m-d');
    }

    public static function paymentLabel(array $party): string
    {
        $configured = trim((string) ($party['payment_terms'] ?? ''));
        if ($configured !== '') { return $configured; }
        return max(0, (int) ($party['payment_days'] ?? 30)) . ' gg' . (!empty($party['payment_month_end']) ? ' FM' : ' data fattura');
    }

    private static function daysFromLabel(string $label): int
    {
        return preg_match('/(\d{1,4})/', $label, $match) ? (int) $match[1] : 30;
    }

    private static function labelIsMonthEnd(string $label): bool
    {
        $label = strtoupper(preg_replace('/\s+/', '', $label) ?? '');
        return str_contains($label, 'FM') || str_contains($label, 'FINEMESE');
    }
}
