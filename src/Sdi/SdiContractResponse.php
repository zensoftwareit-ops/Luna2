<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use LunaApi\Config;
use RuntimeException;

final class SdiContractResponse
{
    public static function serve(string $root, SdiEnvironment $environment, string $service, bool $xsd): never
    {
        $base = rtrim(Config::string('APP_BASE_URL'), '/');
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
}

