<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

use InvalidArgumentException;

final class SdiService
{
    /** @param array<string,string> $recipientMap */
    public function __construct(
        private readonly EncryptedFileStore $store,
        private readonly FatturaPaInspector $inspector,
        private readonly SdiTransport $transport,
        private readonly array $recipientMap,
    ) {
    }

    /** @return array<string,mixed> */
    public function send(SdiEnvironment $environment, string $clientId, string $filename, string $content, ?string $recipientType = null): array
    {
        $inspection = $this->inspector->inspect($filename, $content, $recipientType);
        $record = $this->store->create($environment, $clientId, 'outbound', $filename, $content, [
            'status' => 'SENDING',
            'format' => $inspection['format'],
            'recipient_type' => $inspection['recipient_type'],
            'recipient_code' => $inspection['recipient_code'],
            'sender_vat' => $inspection['sender_vat'],
            'recipient_fiscal_id' => $inspection['recipient_fiscal_id'],
            'document_type' => $inspection['document_type'],
            'document_number' => $inspection['document_number'],
            'document_date' => $inspection['document_date'],
        ], $inspection['normalized_xml']);
        try {
            $response = $this->transport->send($environment, $filename, $content);
            return $this->store->update($environment, $clientId, 'outbound', (string) $record['id'], [
                'status' => $response['error'] === null ? 'ACCEPTED_BY_SDI' : 'SDI_ERROR',
                'sdi_id' => $response['sdi_id'],
                'sdi_received_at' => $response['received_at'],
                'sdi_error' => $response['error'],
            ]);
        } catch (\Throwable $exception) {
            $this->store->update($environment, $clientId, 'outbound', (string) $record['id'], ['status' => 'SEND_FAILED', 'last_error' => $exception->getMessage()]);
            throw $exception;
        }
    }

    /** @return array<string,mixed> */
    public function receiveInvoice(SdiEnvironment $environment, SoapPayload $payload): array
    {
        if ($payload->sdiId === null || !preg_match('/^\d{1,12}$/', $payload->sdiId)) {
            throw new InvalidArgumentException('IdentificativoSdI mancante o non valido.');
        }
        $existing = $this->store->findBySdiId($environment, 'inbound', $payload->sdiId);
        if ($existing !== null) {
            return $existing['metadata'];
        }
        $inspection = $this->inspector->inspect($payload->filename, $payload->content);
        $recipient = strtoupper(preg_replace('/\s+/', '', (string) ($inspection['recipient_fiscal_id'] ?? '')) ?? '');
        $clientId = $this->recipientMap[$recipient] ?? 'unmatched';
        return $this->store->create($environment, $clientId, 'inbound', $payload->filename, $payload->content, [
            'status' => $clientId === 'unmatched' ? 'ROUTING_REQUIRED' : 'READY',
            'sdi_id' => $payload->sdiId,
            'metadata_filename' => $payload->metadataFilename,
            'metadata_sha256' => $payload->metadata !== null ? hash('sha256', $payload->metadata) : null,
            'format' => $inspection['format'],
            'sender_vat' => $inspection['sender_vat'],
            'recipient_fiscal_id' => $inspection['recipient_fiscal_id'],
            'document_type' => $inspection['document_type'],
            'document_number' => $inspection['document_number'],
            'document_date' => $inspection['document_date'],
        ], $inspection['normalized_xml']);
    }

    /** @return array<string,mixed> */
    public function receiveNotification(SdiEnvironment $environment, SoapPayload $payload): array
    {
        if ($payload->sdiId === null || !preg_match('/^\d{1,12}$/', $payload->sdiId)) {
            throw new InvalidArgumentException('IdentificativoSdI della notifica non valido.');
        }
        $outbound = $this->store->findOutboundBySdiId($environment, $payload->sdiId);
        $inbound = $outbound === null ? $this->store->findBySdiId($environment, 'inbound', $payload->sdiId) : null;
        $related = $outbound ?? $inbound;
        $clientId = $related['client_id'] ?? 'unmatched';
        $operation = ucfirst($payload->operation);
        $status = match (strtolower($operation)) {
            'ricevutaconsegna' => 'DELIVERED',
            'notificamancataconsegna' => 'NOT_DELIVERED',
            'notificascarto' => 'REJECTED_BY_SDI',
            'notificaesito' => $this->outcomeStatus($payload->content),
            'notificadecorrenzatermini' => 'TERMS_EXPIRED',
            'attestazionetrasmissionefattura' => 'DELIVERY_IMPOSSIBLE',
            default => 'NOTIFICATION_RECEIVED',
        };
        $record = $this->store->create($environment, $clientId, 'notifications', $payload->filename, $payload->content, [
            'status' => 'RECEIVED',
            'notification_type' => $operation,
            'sdi_id' => $payload->sdiId,
            'resulting_status' => $status,
        ], str_starts_with(ltrim($payload->content), '<') ? $payload->content : null);
        if ($outbound !== null) {
            $this->store->update($environment, $clientId, 'outbound', (string) $outbound['metadata']['id'], [
                'status' => $status,
                'last_notification_id' => $record['id'],
                'last_notification_at' => $record['created_at'],
            ]);
        } elseif ($inbound !== null) {
            $this->store->update($environment, $clientId, 'inbound', (string) $inbound['metadata']['id'], [
                'status' => $status,
                'last_notification_id' => $record['id'],
                'last_notification_at' => $record['created_at'],
            ]);
        }
        return $record;
    }

    private function outcomeStatus(string $xml): string
    {
        if (preg_match('/<(?:(?:\w+):)?Esito[^>]*>\s*(EC01|EC02)\s*</i', $xml, $matches)) {
            return strtoupper($matches[1]) === 'EC01' ? 'ACCEPTED_BY_PA' : 'REJECTED_BY_PA';
        }
        return 'PA_OUTCOME_RECEIVED';
    }
}
