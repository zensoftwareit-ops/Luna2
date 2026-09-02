<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

final class SoapPayload
{
    public function __construct(
        public readonly string $operation,
        public readonly ?string $sdiId,
        public readonly string $filename,
        public readonly string $content,
        public readonly ?string $metadataFilename = null,
        public readonly ?string $metadata = null,
    ) {
    }
}

