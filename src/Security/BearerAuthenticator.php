<?php

declare(strict_types=1);

namespace LunaApi\Security;

final class BearerAuthenticator
{
    /** @param array<string, string> $tokens Chiave client, valore token segreto. */
    public function __construct(private readonly array $tokens)
    {
    }

    public function authenticate(?string $authorization): ?string
    {
        if ($authorization === null || !preg_match('/^Bearer\s+(.+)$/i', trim($authorization), $matches)) {
            return null;
        }
        $candidate = trim($matches[1]);
        foreach ($this->tokens as $clientId => $token) {
            if ($token !== '' && hash_equals($token, $candidate)) {
                return (string) $clientId;
            }
        }
        return null;
    }
}
