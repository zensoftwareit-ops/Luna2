<?php

declare(strict_types=1);

namespace LunaApi\Provider;

interface CompanyProvider
{
    /** @return array<string, mixed>|null */
    public function findItalianCompany(string $vatNumber): ?array;
}

