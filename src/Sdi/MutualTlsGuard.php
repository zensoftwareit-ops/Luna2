<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use LunaApi\Config;

final class MutualTlsGuard
{
    /** @param array<string,mixed> $server */
    public static function verified(array $server): bool
    {
        if (strtoupper((string) ($server['SSL_CLIENT_VERIFY'] ?? '')) === 'SUCCESS') {
            return true;
        }
        if (!Config::boolean('SDI_TRUST_PROXY_MTLS', false)) {
            return false;
        }
        $trusted = array_filter(array_map('trim', explode(',', (string) (getenv('SDI_TRUSTED_PROXY_IPS') ?: '127.0.0.1,::1'))));
        $remote = (string) ($server['REMOTE_ADDR'] ?? '');
        if (!in_array($remote, $trusted, true)) {
            return false;
        }
        return strtoupper((string) ($server['HTTP_X_SDI_CLIENT_VERIFY'] ?? '')) === 'SUCCESS';
    }
}
