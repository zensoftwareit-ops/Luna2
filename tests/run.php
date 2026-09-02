<?php

declare(strict_types=1);

use LunaApi\Cache\FileCache;
use LunaApi\Domain\VatNumber;
use LunaApi\Provider\CompanyProvider;
use LunaApi\Provider\OpenApiCompanyProvider;
use LunaApi\Security\BearerAuthenticator;
use LunaApi\Service\VatLookupService;
use LunaApi\Sdi\EncryptedFileStore;
use LunaApi\Sdi\FatturaPaInspector;
use LunaApi\Sdi\SdiEnvironment;
use LunaApi\Sdi\SdiService;
use LunaApi\Sdi\SdiTransport;
use LunaApi\Sdi\SoapPayloadParser;

require dirname(__DIR__) . '/bootstrap/autoload.php';

$passed = 0;
$failed = 0;

$test = static function (string $name, callable $callback) use (&$passed, &$failed): void {
    try {
        $callback();
        echo "[OK] {$name}\n";
        $passed++;
    } catch (Throwable $exception) {
        echo "[ERRORE] {$name}: {$exception->getMessage()}\n";
        $failed++;
    }
};

$assert = static function (bool $condition, string $message = 'Asserzione fallita.'): void {
    if (!$condition) {
        throw new RuntimeException($message);
    }
};

$invoiceXml = static function (string $format = 'FPR12', string $recipientCode = 'ABC1234', string $recipientFiscalId = '09876543210'): string {
    return '<?xml version="1.0" encoding="UTF-8"?>'
        . '<p:FatturaElettronica xmlns:p="http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.2" versione="' . $format . '">'
        . '<FatturaElettronicaHeader><DatiTrasmissione><IdTrasmittente><IdPaese>IT</IdPaese><IdCodice>12485671007</IdCodice></IdTrasmittente><ProgressivoInvio>00001</ProgressivoInvio><FormatoTrasmissione>' . $format . '</FormatoTrasmissione><CodiceDestinatario>' . $recipientCode . '</CodiceDestinatario></DatiTrasmissione>'
        . '<CedentePrestatore><DatiAnagrafici><IdFiscaleIVA><IdPaese>IT</IdPaese><IdCodice>12485671007</IdCodice></IdFiscaleIVA><Anagrafica><Denominazione>FORNITORE SRL</Denominazione></Anagrafica><RegimeFiscale>RF01</RegimeFiscale></DatiAnagrafici></CedentePrestatore>'
        . '<CessionarioCommittente><DatiAnagrafici><IdFiscaleIVA><IdPaese>IT</IdPaese><IdCodice>' . $recipientFiscalId . '</IdCodice></IdFiscaleIVA><Anagrafica><Denominazione>CLIENTE SRL</Denominazione></Anagrafica></DatiAnagrafici></CessionarioCommittente></FatturaElettronicaHeader>'
        . '<FatturaElettronicaBody><DatiGenerali><DatiGeneraliDocumento><TipoDocumento>TD01</TipoDocumento><Divisa>EUR</Divisa><Data>2026-09-01</Data><Numero>42</Numero></DatiGeneraliDocumento></DatiGenerali></FatturaElettronicaBody>'
        . '</p:FatturaElettronica>';
};

$test('normalizza partita IVA italiana', static function () use ($assert): void {
    $assert(VatNumber::italian('IT 12485671007') === '12485671007');
});

$test('rifiuta checksum errato', static function () use ($assert): void {
    try {
        VatNumber::italian('12485671008');
    } catch (InvalidArgumentException) {
        $assert(true);
        return;
    }
    throw new RuntimeException('La partita IVA non valida è stata accettata.');
});

$test('autentica token bearer senza confronto debole', static function () use ($assert): void {
    $auth = new BearerAuthenticator(['azienda-demo' => 'token-corretto']);
    $assert($auth->authenticate('Bearer token-corretto') === 'azienda-demo');
    $assert($auth->authenticate('Bearer token-errato') === null);
    $assert($auth->authenticate(null) === null);
});

$test('distingue correttamente fattura privata e fattura PA', static function () use ($assert, $invoiceXml): void {
    $inspector = new FatturaPaInspector();
    $private = $inspector->inspect('IT12485671007_00001.xml', $invoiceXml());
    $pa = $inspector->inspect('IT12485671007_00002.xml', $invoiceXml('FPA12', 'ABC123', '09876543210'), 'PA');
    $assert($private['recipient_type'] === 'PRIVATE');
    $assert($private['recipient_fiscal_id'] === '09876543210');
    $assert($pa['recipient_type'] === 'PA');
    $assert($pa['recipient_code'] === 'ABC123');
});

$test('cifra e separa lo storage SdI per ambiente e cliente', static function () use ($assert): void {
    if (!function_exists('openssl_encrypt')) {
        throw new RuntimeException('Estensione OpenSSL non caricata nel runtime di test.');
    }
    $directory = sys_get_temp_dir() . '/luna2-sdi-store-' . bin2hex(random_bytes(5));
    $store = new EncryptedFileStore($directory, str_repeat('k', 32));
    $row = $store->create(SdiEnvironment::Test, 'azienda-demo', 'inbound', 'IT12485671007_00001.xml', '<xml/>', ['sdi_id' => '123']);
    $assert($store->content(SdiEnvironment::Test, 'azienda-demo', 'inbound', $row['id']) === '<xml/>');
    $assert($store->get(SdiEnvironment::Production, 'azienda-demo', 'inbound', $row['id']) === null);
    $iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($directory, FilesystemIterator::SKIP_DOTS), RecursiveIteratorIterator::CHILD_FIRST);
    foreach ($iterator as $item) {
        $item->isDir() ? rmdir($item->getPathname()) : unlink($item->getPathname());
    }
    rmdir($directory);
});

$test('decodifica una consegna SOAP SdI base64', static function () use ($assert, $invoiceXml): void {
    $xml = $invoiceXml();
    $soap = '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://www.fatturapa.gov.it/sdi/ws/ricezione/v1.0/types"><soap:Body><typ:fileSdIConMetadati><IdentificativoSdI>987654</IdentificativoSdI><NomeFile>IT12485671007_00001.xml</NomeFile><File>' . base64_encode($xml) . '</File><NomeFileMetadati>IT12485671007_00001_MT.xml</NomeFileMetadati><Metadati>' . base64_encode('<Metadati/>') . '</Metadati></typ:fileSdIConMetadati></soap:Body></soap:Envelope>';
    $payload = (new SoapPayloadParser())->parse($soap, 'text/xml', '"http://www.fatturapa.it/RicezioneFatture/RiceviFattureSdI"');
    $assert($payload->operation === 'RiceviFattureSdI');
    $assert($payload->sdiId === '987654');
    $assert($payload->content === $xml);
});

$test('decodifica una consegna SOAP MTOM con allegati XOP', static function () use ($assert, $invoiceXml): void {
    $invoice = $invoiceXml();
    $boundary = '----Luna2TestBoundary';
    $soap = '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://www.fatturapa.gov.it/sdi/ws/ricezione/v1.0/types" xmlns:xop="http://www.w3.org/2004/08/xop/include"><soap:Body><typ:fileSdIConMetadati><IdentificativoSdI>987655</IdentificativoSdI><NomeFile>IT12485671007_00002.xml</NomeFile><File><xop:Include href="cid:invoice@test"/></File><NomeFileMetadati>IT12485671007_00002_MT.xml</NomeFileMetadati><Metadati>' . base64_encode('<Metadati/>') . '</Metadati></typ:fileSdIConMetadati></soap:Body></soap:Envelope>';
    $body = '--' . $boundary . "\r\nContent-Type: application/xop+xml; type=\"text/xml\"\r\nContent-ID: <root@test>\r\n\r\n" . $soap . "\r\n"
        . '--' . $boundary . "\r\nContent-Type: application/octet-stream\r\nContent-ID: <invoice@test>\r\n\r\n" . $invoice . "\r\n"
        . '--' . $boundary . "--\r\n";
    $payload = (new SoapPayloadParser())->parse($body, 'multipart/related; type="application/xop+xml"; boundary="' . $boundary . '"');
    $assert($payload->sdiId === '987655');
    $assert($payload->content === $invoice);
});

$test('instrada una fattura passiva al solo client destinatario', static function () use ($assert, $invoiceXml): void {
    if (!function_exists('openssl_encrypt')) {
        throw new RuntimeException('Estensione OpenSSL non caricata nel runtime di test.');
    }
    $directory = sys_get_temp_dir() . '/luna2-sdi-service-' . bin2hex(random_bytes(5));
    $store = new EncryptedFileStore($directory, str_repeat('s', 32));
    $service = new SdiService($store, new FatturaPaInspector(), new SdiTransport(), ['09876543210' => 'azienda-demo']);
    $payload = new \LunaApi\Sdi\SoapPayload('RiceviFatture', '123456', 'IT12485671007_00001.xml', $invoiceXml());
    $row = $service->receiveInvoice(SdiEnvironment::Test, $payload);
    $assert($row['client_id'] === 'azienda-demo');
    $assert($row['status'] === 'READY');
    $assert(count($store->list(SdiEnvironment::Test, 'azienda-demo', 'inbound')) === 1);
    $iterator = new RecursiveIteratorIterator(new RecursiveDirectoryIterator($directory, FilesystemIterator::SKIP_DOTS), RecursiveIteratorIterator::CHILD_FIRST);
    foreach ($iterator as $item) {
        $item->isDir() ? rmdir($item->getPathname()) : unlink($item->getPathname());
    }
    rmdir($directory);
});

$test('normalizza la risposta ufficiale OpenAPI Company', static function () use ($assert): void {
    $result = OpenApiCompanyProvider::normalizePayload([
        'success' => true,
        'data' => [[
            'taxCode' => '12485671007',
            'companyName' => 'OPENAPI S.P.A.',
            'vatCode' => '12485671007',
            'address' => ['registeredOffice' => [
                'streetName' => 'VIALE F TOMMASO MARINETTI 221',
                'town' => 'ROMA',
                'province' => 'RM',
                'zipCode' => '00143',
            ]],
            'activityStatus' => 'ATTIVA',
            'sdiCode' => 'USAL8PV',
            'lastUpdateTimestamp' => 1708705000,
        ]],
    ], '12485671007');
    $assert(is_array($result));
    $assert($result['business_name'] === 'OPENAPI S.P.A.');
    $assert($result['city'] === 'ROMA');
    $assert($result['status'] === 'ACTIVE');
    $assert($result['sdi_code'] === 'USAL8PV');
});

$test('usa cache dopo la prima ricerca', static function () use ($assert): void {
    $provider = new class implements CompanyProvider {
        public int $calls = 0;
        public function findItalianCompany(string $vatNumber): ?array
        {
            $this->calls++;
            return ['business_name' => 'OPENAPI S.P.A.', 'vat_number' => $vatNumber, 'country_code' => 'IT', 'status' => 'ACTIVE'];
        }
    };
    $directory = sys_get_temp_dir() . '/luna2-api-test-' . bin2hex(random_bytes(5));
    $service = new VatLookupService($provider, new FileCache($directory), 60);
    $first = $service->lookup('12485671007');
    $second = $service->lookup('12485671007');
    $assert($first['cache'] === 'MISS');
    $assert($second['cache'] === 'HIT');
    $assert($provider->calls === 1);
    foreach (glob($directory . '/*') ?: [] as $file) {
        unlink($file);
    }
    rmdir($directory);
});

echo "\nRisultato: {$passed} superati, {$failed} falliti.\n";
exit($failed === 0 ? 0 : 1);
