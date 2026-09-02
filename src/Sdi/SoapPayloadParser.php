<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use DOMDocument;
use DOMElement;
use DOMXPath;
use InvalidArgumentException;

final class SoapPayloadParser
{
    public function parse(string $body, string $contentType, ?string $soapAction = null): SoapPayload
    {
        if (stripos($contentType, 'multipart/related') !== false) {
            [$body, $attachments] = $this->decodeMultipart($body, $contentType);
        } else {
            $attachments = [];
        }

        $dom = new DOMDocument();
        $previous = libxml_use_internal_errors(true);
        try {
            if (!$dom->loadXML($body, LIBXML_NONET | LIBXML_COMPACT)) {
                throw new InvalidArgumentException('Messaggio SOAP non valido.');
            }
        } finally {
            libxml_clear_errors();
            libxml_use_internal_errors($previous);
        }
        $xpath = new DOMXPath($dom);
        $operationNode = $xpath->query('/*[local-name()="Envelope"]/*[local-name()="Body"]/*[1]')?->item(0);
        if (!$operationNode instanceof DOMElement) {
            throw new InvalidArgumentException('Operazione SOAP mancante.');
        }
        $value = static function (string $name) use ($xpath, $operationNode): ?string {
            $node = $xpath->query('.//*[local-name()="' . $name . '"]', $operationNode)?->item(0);
            $text = trim((string) ($node?->textContent ?? ''));
            return $text !== '' ? $text : null;
        };
        $binary = function (string $name) use ($xpath, $operationNode, $attachments, $value): string {
            $node = $xpath->query('.//*[local-name()="' . $name . '"]', $operationNode)?->item(0);
            if ($node instanceof DOMElement) {
                $include = $xpath->query('.//*[local-name()="Include"]', $node)?->item(0);
                if ($include instanceof DOMElement) {
                    $cid = trim($include->getAttribute('href'));
                    $cid = trim(preg_replace('/^cid:/i', '', $cid) ?? '', '<>');
                    if (isset($attachments[$cid])) {
                        return $attachments[$cid];
                    }
                }
            }
            $encoded = $value($name);
            $decoded = $encoded !== null ? base64_decode(preg_replace('/\s+/', '', $encoded) ?? '', true) : false;
            return $decoded === false ? '' : $decoded;
        };

        $filename = $value('NomeFile') ?? '';
        if ($filename === '' || !preg_match('/^[A-Za-z0-9_.]{9,50}$/', $filename)) {
            throw new InvalidArgumentException('NomeFile SOAP non valido.');
        }
        $content = $binary('File');
        if ($content === '') {
            throw new InvalidArgumentException('File SOAP vuoto o non decodificabile.');
        }

        $operation = $operationNode->localName;
        if (in_array($operation, ['fileSdI', 'fileSdIConMetadati'], true) && $soapAction !== null && preg_match('~/([A-Za-z]+)"?$~', $soapAction, $action)) {
            $operation = $action[1];
        } elseif ($operation === 'fileSdIConMetadati') {
            $operation = 'RiceviFatture';
        }

        return new SoapPayload(
            $operation,
            $value('IdentificativoSdI'),
            $filename,
            $content,
            $value('NomeFileMetadati'),
            $binary('Metadati') ?: null,
        );
    }

    /** @return array{string, array<string,string>} */
    private function decodeMultipart(string $body, string $contentType): array
    {
        if (!preg_match('/boundary=(?:"([^"]+)"|([^;\s]+))/i', $contentType, $matches)) {
            throw new InvalidArgumentException('Boundary MTOM mancante.');
        }
        $boundary = $matches[1] !== '' ? $matches[1] : $matches[2];
        $parts = preg_split('/\r?\n--' . preg_quote($boundary, '/') . '(?:--)?\r?\n/', "\r\n" . $body) ?: [];
        $soap = null;
        $attachments = [];
        foreach ($parts as $part) {
            $part = ltrim($part, "\r\n");
            if ($part === '' || !preg_match('/\A(.*?)\r?\n\r?\n(.*)\z/s', $part, $match)) {
                continue;
            }
            $headers = $match[1];
            $content = preg_replace('/\r?\n\z/', '', $match[2]) ?? $match[2];
            if (stripos($headers, 'application/xop+xml') !== false || stripos($headers, 'text/xml') !== false) {
                $soap = $content;
            }
            if (preg_match('/Content-ID:\s*<?([^>\r\n]+)>?/i', $headers, $cid)) {
                $attachments[trim($cid[1])] = $content;
            }
        }
        if ($soap === null) {
            throw new InvalidArgumentException('Parte SOAP MTOM mancante.');
        }
        return [$soap, $attachments];
    }
}
