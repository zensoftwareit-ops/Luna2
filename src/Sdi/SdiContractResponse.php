<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use LunaApi\Config;
use RuntimeException;

final class SdiContractResponse
{
    public static function serve(string $root, SdiEnvironment $environment, string $service, bool $xsd): never
    {
        $base = rtrim(Config::string('APP_BASE_URL', self::requestBaseUrl()), '/');
        $prefix = $environment === SdiEnvironment::Test ? '/test' : '';
        $path = $service === 'ricezione' ? '/sdi/ricezione-fatture' : '/sdi/trasmissione-fatture';
        $resource = match ([$service, $xsd]) {
            ['ricezione', false] => 'RicezioneFatture_v1.0.wsdl',
            ['ricezione', true] => 'RicezioneTypes_v1.0.xsd',
            ['trasmissione', false] => 'TrasmissioneFatture_v1.1.wsdl',
            ['trasmissione', true] => 'TrasmissioneTypes_v1.1.xsd',
            default => throw new RuntimeException('Contratto SdI sconosciuto.'),
        };
        $content = file_get_contents($root . '/resources/sdi/' . $resource);
        if ($content === false) {
            throw new RuntimeException('Contratto SdI non disponibile.');
        }
        if (!$xsd) {
            $endpoint = $base . $prefix . $path;
            $content = str_replace(['{{ENDPOINT_URL}}', '{{XSD_URL}}'], [$endpoint, $endpoint . '?xsd=1'], $content);
        }
        header('Content-Type: application/xml; charset=utf-8');
        header('Cache-Control: public, max-age=3600');
        echo $content;
        exit;
    }

    private static function requestBaseUrl(): string
    {
        $host = strtolower(trim((string) ($_SERVER['HTTP_HOST'] ?? '')));
        if (!preg_match('/^[a-z0-9.-]+(?::[0-9]{2,5})?$/', $host)) {
            throw new RuntimeException('APP_BASE_URL non configurato e host richiesta non valido.');
        }
        $https = strtolower((string) ($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '')) === 'https'
            || strtolower((string) ($_SERVER['HTTPS'] ?? '')) === 'on';
        return ($https ? 'https://' : 'http://') . $host;
    }
}

