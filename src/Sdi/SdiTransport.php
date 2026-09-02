<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use DOMDocument;
use DOMXPath;
use LunaApi\Config;
use RuntimeException;

final class SdiTransport
{
    /** @return array{sdi_id:string,received_at:string,error:?string} */
    public function send(SdiEnvironment $environment, string $filename, string $content): array
    {
        $prefix = $environment->configPrefix();
        $endpoint = Config::string($prefix . 'ENDPOINT');
        if (!str_starts_with(strtolower($endpoint), 'https://')) {
            throw new RuntimeException('L’endpoint SdI deve usare HTTPS.');
        }
        $certificate = Config::string($prefix . 'CERT_PATH');
        $certificateType = strtoupper(Config::string($prefix . 'CERT_TYPE', 'PEM'));
        $key = (string) (getenv($prefix . 'KEY_PATH') ?: '');
        $password = (string) (getenv($prefix . 'CERT_PASSWORD') ?: '');
        $ca = Config::string($prefix . 'CA_PATH');
        foreach ([$certificate, $ca] as $file) {
            if (!is_file($file)) {
                throw new RuntimeException('Certificato SdI non trovato: ' . basename($file));
            }
        }
        if ($certificateType === 'PEM' && ($key === '' || !is_file($key))) {
            throw new RuntimeException('Chiave privata PEM SdI non trovata.');
        }
        if (!in_array($certificateType, ['PEM', 'P12'], true)) {
            throw new RuntimeException('Tipo certificato SdI non supportato.');
        }

        [$body, $contentType] = $this->mtom($filename, $content);
        $curl = curl_init($endpoint);
        if ($curl === false) {
            throw new RuntimeException('Impossibile inizializzare il collegamento SdI.');
        }
        $options = [
            CURLOPT_POST => true,
            CURLOPT_POSTFIELDS => $body,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_TIMEOUT => 60,
            CURLOPT_HTTPHEADER => [
                'Content-Type: ' . $contentType,
                'SOAPAction: "http://www.fatturapa.it/SdIRiceviFile/RiceviFile"',
                'Accept: multipart/related, text/xml, application/soap+xml',
                'User-Agent: Luna2-SDICoop/1.0',
            ],
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_CAINFO => $ca,
            CURLOPT_SSLCERT => $certificate,
            CURLOPT_SSLCERTTYPE => $certificateType,
        ];
        if ($password !== '') {
            $options[CURLOPT_SSLCERTPASSWD] = $password;
        }
        if ($certificateType === 'PEM') {
            $options[CURLOPT_SSLKEY] = $key;
            if ($password !== '') {
                $options[CURLOPT_SSLKEYPASSWD] = $password;
            }
        }
        curl_setopt_array($curl, $options);
        $response = curl_exec($curl);
        $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
        $responseType = (string) curl_getinfo($curl, CURLINFO_CONTENT_TYPE);
        $error = curl_error($curl);
        curl_close($curl);
        if ($response === false) {
            throw new RuntimeException('Collegamento SdI non riuscito: ' . $error);
        }
        if ($status < 200 || $status >= 300) {
            throw new RuntimeException('SdI ha restituito HTTP ' . $status . '.');
        }
        return $this->parseResponse($response, $responseType);
    }

    /** @return array{string,string} */
    private function mtom(string $filename, string $content): array
    {
        $boundary = '----=_Luna2_' . bin2hex(random_bytes(16));
        $rootId = 'root.' . bin2hex(random_bytes(8)) . '@luna2';
        $fileId = 'invoice.' . bin2hex(random_bytes(8)) . '@luna2';
        $escape = static fn (string $value): string => htmlspecialchars($value, ENT_XML1 | ENT_QUOTES, 'UTF-8');
        $soap = '<?xml version="1.0" encoding="UTF-8"?>'
            . '<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:typ="http://www.fatturapa.gov.it/sdi/ws/trasmissione/v1.0/types" xmlns:xop="http://www.w3.org/2004/08/xop/include">'
            . '<soapenv:Header/><soapenv:Body><typ:fileSdIAccoglienza><NomeFile>' . $escape($filename) . '</NomeFile><File><xop:Include href="cid:' . $fileId . '"/></File></typ:fileSdIAccoglienza></soapenv:Body></soapenv:Envelope>';
        $eol = "\r\n";
        $body = '--' . $boundary . $eol
            . 'Content-Type: application/xop+xml; charset=UTF-8; type="text/xml"' . $eol
            . 'Content-Transfer-Encoding: 8bit' . $eol
            . 'Content-ID: <' . $rootId . '>' . $eol . $eol
            . $soap . $eol
            . '--' . $boundary . $eol
            . 'Content-Type: application/octet-stream' . $eol
            . 'Content-Transfer-Encoding: binary' . $eol
            . 'Content-ID: <' . $fileId . '>' . $eol . $eol
            . $content . $eol
            . '--' . $boundary . '--' . $eol;
        $type = 'multipart/related; type="application/xop+xml"; start="<' . $rootId . '>"; start-info="text/xml"; boundary="' . $boundary . '"';
        return [$body, $type];
    }

    /** @return array{sdi_id:string,received_at:string,error:?string} */
    private function parseResponse(string $response, string $contentType): array
    {
        if (stripos($contentType, 'multipart/related') !== false && preg_match('/boundary=(?:"([^"]+)"|([^;\s]+))/i', $contentType, $boundary)) {
            $token = $boundary[1] !== '' ? $boundary[1] : $boundary[2];
            foreach (preg_split('/--' . preg_quote($token, '/') . '/', $response) ?: [] as $part) {
                if (stripos($part, 'application/xop+xml') !== false && preg_match('/\r?\n\r?\n(.*)\r?\n/s', $part, $match)) {
                    $response = $match[1];
                    break;
                }
            }
        }
        $dom = new DOMDocument();
        if (!@$dom->loadXML($response, LIBXML_NONET | LIBXML_COMPACT)) {
            throw new RuntimeException('Risposta SOAP SdI non valida.');
        }
        $xpath = new DOMXPath($dom);
        $value = static function (string $name) use ($xpath): ?string {
            $text = trim((string) ($xpath->query('//*[local-name()="' . $name . '"]')?->item(0)?->textContent ?? ''));
            return $text !== '' ? $text : null;
        };
        $sdiId = $value('IdentificativoSdI');
        $receivedAt = $value('DataOraRicezione');
        if ($sdiId === null || $receivedAt === null) {
            throw new RuntimeException('La risposta SdI non contiene identificativo e data di ricezione.');
        }
        return ['sdi_id' => $sdiId, 'received_at' => $receivedAt, 'error' => $value('Errore')];
    }
}

