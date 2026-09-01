<?php

declare(strict_types=1);

namespace Luna\Service;

use RuntimeException;
use Throwable;

final class LicenseTransportException extends RuntimeException
{
    public function __construct(
        string $message,
        public readonly string $requestId,
        public readonly int $httpStatus = 0,
        public readonly int $durationMs = 0,
        ?Throwable $previous = null,
    ) {
        parent::__construct($message, 0, $previous);
    }
}
