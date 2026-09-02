<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use DOMDocument;
use DOMXPath;
use InvalidArgumentException;

final class FatturaPaInspector
{
    public function __construct(private readonly P7mExtractor $extractor = new P7mExtractor())
    {
    }

    /** @return array<string, mixed> */
    public function inspect(string $filename, string $content, ?string $requiredRecipientType = null): array
    {
        $filename = basename($filename);
        if (!preg_match('/^[A-Za-z0-9_.]{9,50}$/', $filename) || !preg_match('/\.(xml|p7m|xml\.p7m)$/i', $filename)) {
            throw new InvalidArgumentException('Nome file SdI non valido. Usare il nome FatturaPA previsto dal canale.');
        }
        if ($content === '') {
            throw new InvalidArgumentException('Il file fattura è vuoto.');
        }
        $xml = $this->extractor->extract($content);
        if ($xml === null) {
            throw new InvalidArgumentException('Impossibile estrarre o leggere il contenuto XML della fattura firmata.');
        }

        $dom = new DOMDocument();
        $previous = libxml_use_internal_errors(true);
        try {
            if (!$dom->loadXML($xml, LIBXML_NONET | LIBXML_COMPACT) || $dom->documentElement?->localName !== 'FatturaElettronica') {
                throw new InvalidArgumentException('Il file non contiene una FatturaElettronica valida.');
            }
        } finally {
            libxml_clear_errors();
            libxml_use_internal_errors($previous);
        }
        $xpath = new DOMXPath($dom);
        $value = static function (string $expression) use ($xpath): ?string {
            $node = $xpath->query($expression)?->item(0);
            $text = trim((string) ($node?->textContent ?? ''));
            return $text !== '' ? $text : null;
        };

        $format = strtoupper((string) $value('/*[local-name()="FatturaElettronica"]/*[local-name()="FatturaElettronicaHeader"]/*[local-name()="DatiTrasmissione"]/*[local-name()="FormatoTrasmissione"]'));
        $recipientCode = strtoupper((string) $value('/*[local-name()="FatturaElettronica"]/*[local-name()="FatturaElettronicaHeader"]/*[local-name()="DatiTrasmissione"]/*[local-name()="CodiceDestinatario"]'));
        $isPa = str_starts_with($format, 'FPA');
        if ($isPa && strlen($recipientCode) !== 6) {
            throw new InvalidArgumentException('Una fattura PA deve indicare FormatoTrasmissione FPA e un CodiceDestinatario di 6 caratteri.');
        }
        if (!$isPa && str_starts_with($format, 'FPR') && strlen($recipientCode) !== 7) {
            throw new InvalidArgumentException('Una fattura tra privati deve indicare un CodiceDestinatario di 7 caratteri.');
        }
        if (strtoupper((string) $requiredRecipientType) === 'PA' && !$isPa) {
            throw new InvalidArgumentException('La richiesta è marcata PA ma il tracciato non è FPA.');
        }

        return [
            'normalized_xml' => $xml,
            'format' => $format,
            'recipient_type' => $isPa ? 'PA' : 'PRIVATE',
            'recipient_code' => $recipientCode,
            'sender_vat' => $value('/*[local-name()="FatturaElettronica"]/*[local-name()="FatturaElettronicaHeader"]/*[local-name()="CedentePrestatore"]/*[local-name()="DatiAnagrafici"]/*[local-name()="IdFiscaleIVA"]/*[local-name()="IdCodice"]'),
            'recipient_fiscal_id' => $value('/*[local-name()="FatturaElettronica"]/*[local-name()="FatturaElettronicaHeader"]/*[local-name()="CessionarioCommittente"]/*[local-name()="DatiAnagrafici"]/*[local-name()="IdFiscaleIVA"]/*[local-name()="IdCodice"]')
                ?? $value('/*[local-name()="FatturaElettronica"]/*[local-name()="FatturaElettronicaHeader"]/*[local-name()="CessionarioCommittente"]/*[local-name()="DatiAnagrafici"]/*[local-name()="CodiceFiscale"]'),
            'document_type' => $value('(//*[local-name()="DatiGeneraliDocumento"]/*[local-name()="TipoDocumento"])[1]'),
            'document_number' => $value('(//*[local-name()="DatiGeneraliDocumento"]/*[local-name()="Numero"])[1]'),
            'document_date' => $value('(//*[local-name()="DatiGeneraliDocumento"]/*[local-name()="Data"])[1]'),
        ];
    }
}

