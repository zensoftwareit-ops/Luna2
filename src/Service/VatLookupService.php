<?php

declare(strict_types=1);

namespace LunaApi\Service;

use LunaApi\Cache\FileCache;
use LunaApi\Provider\CompanyProvider;

final class VatLookupService
{
    public function __construct(
        private readonly CompanyProvider $provider,
        private readonly FileCache $cache,
        private readonly int $ttlSeconds,
    ) {
    }

    /** @return array{data: array<string, mixed>|null, cache: string} */
    public function lookup(string $vatNumber): array
    {
        $key = 'vat:IT:' . $vatNumber;
        $cached = $this->cache->get($key);
        if ($cached !== null) {
            return ['data' => $cached, 'cache' => 'HIT'];
        }

        $company = $this->provider->findItalianCompany($vatNumber);
        if ($company !== null) {
            $this->cache->put($key, $company, $this->ttlSeconds);
        }
        return ['data' => $company, 'cache' => 'MISS'];
    }
}

