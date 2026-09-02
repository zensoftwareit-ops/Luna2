<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use InvalidArgumentException;

enum SdiEnvironment: string
{
    case Production = 'production';
    case Test = 'test';

    public static function fromPath(string $path): self
    {
        return str_starts_with($path, '/test/') || $path === '/test' ? self::Test : self::Production;
    }

    public function withoutPrefix(string $path): string
    {
        if ($this === self::Test) {
            $path = substr($path, 5) ?: '/';
        }
        return '/' . trim($path, '/');
    }

    public function configPrefix(): string
    {
        return $this === self::Test ? 'SDI_TEST_' : 'SDI_PRODUCTION_';
    }

    public static function assert(string $value): self
    {
        return self::tryFrom($value) ?? throw new InvalidArgumentException('Ambiente SdI non valido.');
    }
}

