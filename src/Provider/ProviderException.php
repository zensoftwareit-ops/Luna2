<?php

declare(strict_types=1);

namespace LunaApi\Provider;

use RuntimeException;

final class ProviderException extends RuntimeException
{
    public function __construct(string $message, public readonly int $httpStatus = 502)
    {
        parent::__construct($message);
    }
}

