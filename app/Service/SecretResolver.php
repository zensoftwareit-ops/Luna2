<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;

final class SecretResolver
{
    public static function resolve(?string $reference): string
    {
        if (!$reference) {
            return '';
        }
        if (!str_starts_with($reference, 'ENV:')) {
            throw new InvalidArgumentException('Riferimento segreto non supportato. Usa ENV:NOME_VARIABILE.');
        }
        $name = substr($reference, 4);
        if (!preg_match('/^[A-Z][A-Z0-9_]{1,100}$/', $name)) {
            throw new InvalidArgumentException('Nome della variabile segreta non valido.');
        }
        $value = getenv($name);
        if ($value === false || $value === '') {
            throw new InvalidArgumentException('Segreto non disponibile nell’ambiente: ' . $name . '.');
        }
        return $value;
    }
}
