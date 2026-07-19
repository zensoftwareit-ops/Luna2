<?php

declare(strict_types=1);

namespace Luna\Service;

use DOMDocument;
use DOMElement;
use InvalidArgumentException;
use PDO;

final class FatturaPaService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId)
    {
    }

    public function generate(int $documentId): string
    {
        $statement = $this->db->prepare('SELECT * FROM documents WHERE id = ? AND organization_id = ?');
        $statement->execute([$documentId, $this->organizationId]);
        $document = $statement->fetch();
        if (!$document || !in_array($document['document_type'], ['SALES_INVOICE', 'CREDIT_NOTE'], true)) {
            throw new InvalidArgumentException('XML FatturaPA disponibile solo per fatture e note di credito attive.');
        }
        $statement = $this->db->prepare('SELECT * FROM document_lines WHERE document_id = ? AND organization_id = ? ORDER BY line_number');
        $statement->execute([$documentId, $this->organizationId]);
        $lines = $statement->fetchAll();
        $statement = $this->db->prepare('SELECT * FROM organizations WHERE id = ?');
        $statement->execute([$this->organizationId]);
        $organization = $statement->fetch();
        $statement = $this->db->prepare('SELECT * FROM customers WHERE id = ? AND organization_id = ?');
        $statement->execute([$document['counterparty_id'], $this->organizationId]);
        $customer = $statement->fetch();
        if (!$organization || !$customer) {
            throw new InvalidArgumentException('Dati fiscali azienda o cliente incompleti.');
        }

        $dom = new DOMDocument('1.0', 'UTF-8');
        $dom->formatOutput = true;
        $root = $dom->createElementNS('http://ivaservizi.agenziaentrate.gov.it/docs/xsd/fatture/v1.2', 'p:FatturaElettronica');
        $root->setAttribute('versione', 'FPR12');
        $root->setAttributeNS('http://www.w3.org/2000/xmlns/', 'xmlns:ds', 'http://www.w3.org/2000/09/xmldsig#');
        $root->setAttributeNS('http://www.w3.org/2000/xmlns/', 'xmlns:xsi', 'http://www.w3.org/2001/XMLSchema-instance');
        $dom->appendChild($root);

        $header = $this->append($dom, $root, 'FatturaElettronicaHeader');
        $transmission = $this->append($dom, $header, 'DatiTrasmissione');
        $sender = $this->append($dom, $transmission, 'IdTrasmittente');
        $this->value($dom, $sender, 'IdPaese', $organization['country_code'] ?: 'IT');
        $this->value($dom, $sender, 'IdCodice', preg_replace('/\D/', '', (string) $organization['vat_number']));
        $this->value($dom, $transmission, 'ProgressivoInvio', str_pad((string) $document['id'], 5, '0', STR_PAD_LEFT));
        $this->value($dom, $transmission, 'FormatoTrasmissione', 'FPR12');
        $this->value($dom, $transmission, 'CodiceDestinatario', $customer['sdi_code'] ?: '0000000');
        if (!empty($customer['pec'])) {
            $this->value($dom, $transmission, 'PECDestinatario', $customer['pec']);
        }

        $supplier = $this->append($dom, $header, 'CedentePrestatore');
        $supplierData = $this->append($dom, $supplier, 'DatiAnagrafici');
        $supplierVat = $this->append($dom, $supplierData, 'IdFiscaleIVA');
        $this->value($dom, $supplierVat, 'IdPaese', $organization['country_code'] ?: 'IT');
        $this->value($dom, $supplierVat, 'IdCodice', preg_replace('/\D/', '', (string) $organization['vat_number']));
        if (!empty($organization['tax_code'])) {
            $this->value($dom, $supplierData, 'CodiceFiscale', $organization['tax_code']);
        }
        $supplierName = $this->append($dom, $supplierData, 'Anagrafica');
        $this->value($dom, $supplierName, 'Denominazione', $organization['business_name']);
        $this->value($dom, $supplierData, 'RegimeFiscale', $organization['fiscal_regime'] ?: 'RF01');
        $this->address($dom, $supplier, $organization);

        $buyer = $this->append($dom, $header, 'CessionarioCommittente');
        $buyerData = $this->append($dom, $buyer, 'DatiAnagrafici');
        if (!empty($customer['vat_number'])) {
            $buyerVat = $this->append($dom, $buyerData, 'IdFiscaleIVA');
            $this->value($dom, $buyerVat, 'IdPaese', $customer['country_code'] ?: 'IT');
            $this->value($dom, $buyerVat, 'IdCodice', preg_replace('/\D/', '', (string) $customer['vat_number']));
        }
        if (!empty($customer['tax_code'])) {
            $this->value($dom, $buyerData, 'CodiceFiscale', $customer['tax_code']);
        }
        $buyerName = $this->append($dom, $buyerData, 'Anagrafica');
        $this->value($dom, $buyerName, 'Denominazione', $customer['business_name']);
        $this->address($dom, $buyer, $customer);

        $body = $this->append($dom, $root, 'FatturaElettronicaBody');
        $general = $this->append($dom, $body, 'DatiGenerali');
        $generalDocument = $this->append($dom, $general, 'DatiGeneraliDocumento');
        $this->value($dom, $generalDocument, 'TipoDocumento', $document['document_type'] === 'CREDIT_NOTE' ? 'TD04' : ($document['fatturapa_type'] ?: 'TD01'));
        $this->value($dom, $generalDocument, 'Divisa', $document['currency'] ?: 'EUR');
        $this->value($dom, $generalDocument, 'Data', $document['document_date']);
        $this->value($dom, $generalDocument, 'Numero', $document['number']);
        $this->value($dom, $generalDocument, 'ImportoTotaleDocumento', $this->amount($document['total']));

        $goods = $this->append($dom, $body, 'DatiBeniServizi');
        $vatSummary = [];
        foreach ($lines as $line) {
            $detail = $this->append($dom, $goods, 'DettaglioLinee');
            $this->value($dom, $detail, 'NumeroLinea', (string) $line['line_number']);
            $this->value($dom, $detail, 'Descrizione', mb_substr((string) $line['description'], 0, 1000));
            $this->value($dom, $detail, 'Quantita', number_format((float) $line['quantity'], 4, '.', ''));
            $this->value($dom, $detail, 'UnitaMisura', $line['unit'] ?: 'NR');
            $this->value($dom, $detail, 'PrezzoUnitario', number_format((float) $line['unit_price'], 4, '.', ''));
            if ((float) $line['discount_percent'] > 0) {
                $discount = $this->append($dom, $detail, 'ScontoMaggiorazione');
                $this->value($dom, $discount, 'Tipo', 'SC');
                $this->value($dom, $discount, 'Percentuale', number_format((float) $line['discount_percent'], 2, '.', ''));
            }
            $this->value($dom, $detail, 'PrezzoTotale', $this->amount($line['taxable_amount']));
            $this->value($dom, $detail, 'AliquotaIVA', number_format((float) $line['vat_rate'], 2, '.', ''));
            if ((float) $line['vat_rate'] === 0.0 && $line['vat_nature']) {
                $this->value($dom, $detail, 'Natura', $line['vat_nature']);
            }
            $key = number_format((float) $line['vat_rate'], 2, '.', '') . '|' . ($line['vat_nature'] ?: '');
            $vatSummary[$key] ??= ['rate' => (float) $line['vat_rate'], 'nature' => $line['vat_nature'], 'taxable' => 0.0, 'tax' => 0.0];
            $vatSummary[$key]['taxable'] += (float) $line['taxable_amount'];
            $vatSummary[$key]['tax'] += (float) $line['vat_amount'];
        }
        foreach ($vatSummary as $summary) {
            $recap = $this->append($dom, $goods, 'DatiRiepilogo');
            $this->value($dom, $recap, 'AliquotaIVA', number_format($summary['rate'], 2, '.', ''));
            if ($summary['rate'] === 0.0 && $summary['nature']) {
                $this->value($dom, $recap, 'Natura', $summary['nature']);
            }
            $this->value($dom, $recap, 'ImponibileImporto', $this->amount($summary['taxable']));
            $this->value($dom, $recap, 'Imposta', $this->amount($summary['tax']));
            $this->value($dom, $recap, 'EsigibilitaIVA', $document['vat_collectability'] ?: 'I');
        }

        if (!empty($document['due_date'])) {
            $payment = $this->append($dom, $body, 'DatiPagamento');
            $this->value($dom, $payment, 'CondizioniPagamento', 'TP02');
            $detail = $this->append($dom, $payment, 'DettaglioPagamento');
            $this->value($dom, $detail, 'ModalitaPagamento', $document['payment_method_code'] ?: 'MP05');
            $this->value($dom, $detail, 'DataScadenzaPagamento', $document['due_date']);
            $this->value($dom, $detail, 'ImportoPagamento', $this->amount($document['total']));
            if (!empty($organization['iban'])) {
                $this->value($dom, $detail, 'IBAN', $organization['iban']);
            }
        }

        return $dom->saveXML() ?: '';
    }

    private function address(DOMDocument $dom, DOMElement $parent, array $data): void
    {
        $address = $this->append($dom, $parent, 'Sede');
        $this->value($dom, $address, 'Indirizzo', $data['address'] ?: 'N/D');
        $this->value($dom, $address, 'CAP', $data['postal_code'] ?: '00000');
        $this->value($dom, $address, 'Comune', $data['city'] ?: 'N/D');
        if (!empty($data['province']) && ($data['country_code'] ?: 'IT') === 'IT') {
            $this->value($dom, $address, 'Provincia', $data['province']);
        }
        $this->value($dom, $address, 'Nazione', $data['country_code'] ?: 'IT');
    }

    private function append(DOMDocument $dom, DOMElement $parent, string $name): DOMElement
    {
        $element = $dom->createElement($name);
        $parent->appendChild($element);
        return $element;
    }

    private function value(DOMDocument $dom, DOMElement $parent, string $name, mixed $value): void
    {
        $element = $dom->createElement($name);
        $element->appendChild($dom->createTextNode((string) $value));
        $parent->appendChild($element);
    }

    private function amount(mixed $value): string
    {
        return number_format((float) $value, 2, '.', '');
    }
}
