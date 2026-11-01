<?php

declare(strict_types=1);

namespace Luna\Service;

use GuzzleHttp\Client;
use InvalidArgumentException;
use PDO;
use RuntimeException;

final class EinvoiceService
{
    public function __construct(
        private readonly PDO $db,
        private readonly int $organizationId,
        private readonly int $userId,
    ) {
    }

    /** @return array<string,mixed> */
    public function send(int $documentId): array
    {
        $document = $this->document($documentId);
        if (!in_array((string) $document['document_type'], ['SALES_INVOICE', 'CREDIT_NOTE'], true)) {
            throw new InvalidArgumentException('Solo fatture e note di credito attive possono essere inviate allo SdI.');
        }
        if (!in_array((string) $document['status'], ['ISSUED', 'REJECTED'], true)) {
            throw new InvalidArgumentException('Prima rendi emesso il documento; allo SdI non possono essere inviate bozze.');
        }
        $customer = $this->row('SELECT * FROM customers WHERE id = ? AND organization_id = ?', [(int) $document['counterparty_id'], $this->organizationId]);
        if (!$customer) {
            throw new InvalidArgumentException('Cliente della fattura non disponibile.');
        }
        $recipientCode = strtoupper(trim((string) ($customer['sdi_code'] ?? '')));
        if (strlen($recipientCode) === 6) {
            throw new InvalidArgumentException('Invio verso PA non ancora attivo: il codice destinatario indicato è un codice ufficio di 6 caratteri.');
        }
        if ($recipientCode !== '' && !preg_match('/^[A-Z0-9]{7}$/', $recipientCode)) {
            throw new InvalidArgumentException('Per i privati il codice destinatario deve contenere esattamente 7 caratteri alfanumerici.');
        }

        $latest = $this->row(
            'SELECT * FROM sdi_transmissions WHERE organization_id=? AND document_id=? ORDER BY id DESC LIMIT 1',
            [$this->organizationId, $documentId],
        );
        $retryable = ['REJECTED_BY_SDI', 'SEND_FAILED', 'SDI_ERROR', 'ERROR'];
        if ($latest && !in_array((string) $latest['status'], $retryable, true)) {
            return $latest;
        }
        $attemptStatement = $this->db->prepare('SELECT COUNT(*) FROM sdi_transmissions WHERE organization_id=? AND document_id=?');
        $attemptStatement->execute([$this->organizationId, $documentId]);
        $previousAttempts = (int) $attemptStatement->fetchColumn();
        $attempt = $latest && (string) $latest['status'] === 'REJECTED_BY_SDI' ? $previousAttempts + 1 : max(1, $previousAttempts);

        $generator = new FatturaPaService($this->db, $this->organizationId);
        $xml = $generator->generate($documentId, $attempt);
        $filename = $generator->filename($documentId, $attempt);
        $checksum = hash('sha256', $xml);
        $existing = $this->row(
            'SELECT * FROM sdi_transmissions WHERE organization_id = ? AND document_id = ? AND checksum_sha256 = ? LIMIT 1',
            [$this->organizationId, $documentId, $checksum],
        );
        if ($existing && !in_array((string) $existing['status'], ['SEND_FAILED', 'SDI_ERROR', 'ERROR'], true)) {
            return $existing;
        }

        $endpoint = $this->endpoint(true);
        $payload = $this->request($endpoint, 'POST', (string) $endpoint['send_path'], [
            'json' => [
                'filename' => $filename,
                'content_base64' => base64_encode($xml),
                'recipient_type' => 'PRIVATE',
            ],
        ]);
        $remote = $payload['transmission'] ?? null;
        if (!is_array($remote) || empty($remote['id'])) {
            throw new RuntimeException('Il servizio SdI non ha restituito una trasmissione valida.');
        }
        $status = $this->status((string) ($remote['status'] ?? 'ERROR'));
        if ($existing) {
            $this->db->prepare(
                'UPDATE sdi_transmissions SET external_id=?,sdi_id=?,provider_reference=?,status=?,sent_at=NOW(),
                 sdi_received_at=?,last_synced_at=NOW(),error_message=?,updated_at=NOW() WHERE id=? AND organization_id=?'
            )->execute([
                $remote['id'], $remote['sdi_id'] ?? null, $remote['sdi_id'] ?? null, $status,
                $this->dateTime($remote['sdi_received_at'] ?? null), $remote['sdi_error'] ?? $remote['last_error'] ?? null,
                $existing['id'], $this->organizationId,
            ]);
            $this->applyDocumentStatus($documentId, $status);
            return $this->row('SELECT * FROM sdi_transmissions WHERE id = ?', [(int) $existing['id']]) ?: [];
        }
        $statement = $this->db->prepare(
            'INSERT INTO sdi_transmissions
             (organization_id,document_id,provider,filename,checksum_sha256,external_id,sdi_id,provider_reference,status,
              sent_at,sdi_received_at,last_synced_at,error_message,created_at,updated_at)
             VALUES (?,?,?,?,?,?,?,?,?,NOW(),?,NOW(),?,NOW(),NOW())'
        );
        $statement->execute([
            $this->organizationId, $documentId, 'LUNA2_API', $filename, $checksum,
            $remote['id'], $remote['sdi_id'] ?? null, $remote['sdi_id'] ?? null, $status,
            $this->dateTime($remote['sdi_received_at'] ?? null), $remote['sdi_error'] ?? $remote['last_error'] ?? null,
        ]);
        $id = (int) $this->db->lastInsertId();
        $this->applyDocumentStatus($documentId, $status);
        return $this->row('SELECT * FROM sdi_transmissions WHERE id = ?', [$id]) ?: [];
    }

    /** @return array<string,mixed> */
    public function syncTransmission(int $documentId): array
    {
        $endpoint = $this->endpoint(true);
        $transmission = $this->row(
            'SELECT * FROM sdi_transmissions WHERE organization_id = ? AND document_id = ? ORDER BY id DESC LIMIT 1',
            [$this->organizationId, $documentId],
        );
        if (!$transmission || empty($transmission['external_id'])) {
            throw new InvalidArgumentException('Nessuna trasmissione SdI da aggiornare.');
        }
        $path = (string) ($endpoint['status_path'] ?: '/v1/einvoices/outbound/{id}');
        $path = str_contains($path, '{id}') ? str_replace('{id}', rawurlencode((string) $transmission['external_id']), $path)
            : rtrim($path, '/') . '/outbound/' . rawurlencode((string) $transmission['external_id']);
        $payload = $this->request($endpoint, 'GET', $path);
        $remote = $payload['document'] ?? null;
        if (!is_array($remote)) {
            throw new RuntimeException('Stato SdI non disponibile.');
        }
        $status = $this->status((string) ($remote['status'] ?? 'ERROR'));
        $this->db->prepare(
            'UPDATE sdi_transmissions SET sdi_id=?,provider_reference=?,status=?,sdi_received_at=?,last_synced_at=NOW(),
             error_message=?,completed_at=IF(? IN (\'DELIVERED\',\'REJECTED_BY_SDI\',\'DELIVERY_IMPOSSIBLE\'),NOW(),completed_at),updated_at=NOW()
             WHERE id=? AND organization_id=?'
        )->execute([
            $remote['sdi_id'] ?? $transmission['sdi_id'], $remote['sdi_id'] ?? $transmission['provider_reference'], $status,
            $this->dateTime($remote['sdi_received_at'] ?? null), $remote['sdi_error'] ?? $remote['last_error'] ?? null,
            $status, $transmission['id'], $this->organizationId,
        ]);
        $this->applyDocumentStatus($documentId, $status);
        return $this->row('SELECT * FROM sdi_transmissions WHERE id = ?', [(int) $transmission['id']]) ?: [];
    }

    /** @return array{received:int,updated:int} */
    public function syncNotifications(): array
    {
        $endpoint = $this->endpoint(true);
        $pending = $this->db->prepare(
            "SELECT document_id FROM sdi_transmissions WHERE organization_id=? AND external_id IS NOT NULL
             AND status NOT IN ('DELIVERED','NOT_DELIVERED','REJECTED_BY_SDI','TERMS_EXPIRED','DELIVERY_IMPOSSIBLE')
             ORDER BY id DESC LIMIT 200"
        );
        $pending->execute([$this->organizationId]);
        foreach ($pending->fetchAll(PDO::FETCH_COLUMN) as $documentId) {
            try { $this->syncTransmission((int) $documentId); } catch (RuntimeException) { /* Una trasmissione non blocca le altre. */ }
        }
        $root = (string) ($endpoint['receive_path'] ?: '/v1/einvoices');
        $payload = $this->request($endpoint, 'GET', $root, ['query' => ['direction' => 'notifications', 'pending_only' => 'true', 'limit' => 200]]);
        $notifications = is_array($payload['notifications'] ?? null) ? $payload['notifications'] : [];
        $received = $updated = 0;
        foreach ($notifications as $remote) {
            if (!is_array($remote) || empty($remote['id'])) continue;
            $exists = $this->row('SELECT id FROM sdi_notifications WHERE organization_id=? AND external_id=?', [$this->organizationId, $remote['id']]);
            if (!$exists) {
                $contentPayload = $this->request($endpoint, 'GET', rtrim($root, '/') . '/notifications/' . rawurlencode((string) $remote['id']) . '/content');
                $xml = base64_decode((string) ($contentPayload['content_base64'] ?? ''), true);
                if (!is_string($xml) || $xml === '') {
                    throw new RuntimeException('Contenuto della notifica SdI non disponibile.');
                }
                $transmission = $this->row('SELECT * FROM sdi_transmissions WHERE organization_id=? AND sdi_id=? ORDER BY id DESC LIMIT 1', [$this->organizationId, $remote['sdi_id'] ?? '']);
                $this->db->prepare(
                    'INSERT INTO sdi_notifications
                     (organization_id,sdi_transmission_id,external_id,notification_type,resulting_status,filename,checksum_sha256,payload_xml,received_at,processed_at,created_at)
                     VALUES (?,?,?,?,?,?,?,?,?,NOW(),NOW())'
                )->execute([
                    $this->organizationId, $transmission['id'] ?? null, $remote['id'], $remote['notification_type'] ?? 'NOTIFICATION',
                    $remote['resulting_status'] ?? null, $remote['filename'] ?? null, hash('sha256', $xml), $xml,
                    $this->dateTime($remote['created_at'] ?? null) ?: date('Y-m-d H:i:s'),
                ]);
                $received++;
                if ($transmission && !empty($remote['resulting_status'])) {
                    $status = $this->status((string) $remote['resulting_status']);
                    $this->db->prepare('UPDATE sdi_transmissions SET status=?,last_synced_at=NOW(),updated_at=NOW() WHERE id=?')
                        ->execute([$status, $transmission['id']]);
                    $this->applyDocumentStatus((int) $transmission['document_id'], $status);
                    $updated++;
                }
            }
            $this->request($endpoint, 'POST', rtrim($root, '/') . '/notifications/' . rawurlencode((string) $remote['id']) . '/ack');
        }
        return compact('received', 'updated');
    }

    /** @return array<string,mixed> */
    public function readiness(): array
    {
        $endpoint = $this->endpoint(false);
        return $this->request($endpoint, 'GET', '/v1/einvoices/readiness', [], true);
    }

    /** @return array<string,mixed> */
    private function endpoint(bool $requirePaths): array
    {
        $statement = $this->db->prepare(
            "SELECT * FROM api_endpoint_configs WHERE organization_id=? AND service_key IN ('EINVOICE_SDI','EINVOICE') AND enabled=1
             ORDER BY environment='PRODUCTION' DESC,id DESC LIMIT 1"
        );
        $statement->execute([$this->organizationId]);
        $endpoint = $statement->fetch();
        if (!$endpoint) {
            throw new InvalidArgumentException('Configura e abilita il canale EINVOICE_SDI.');
        }
        if ($requirePaths && (empty($endpoint['send_path']) || empty($endpoint['receive_path']))) {
            throw new InvalidArgumentException('Il canale deve indicare i percorsi di invio e ricezione.');
        }
        return $endpoint;
    }

    /** @return array<string,mixed> */
    private function request(array $endpoint, string $method, string $path, array $options = [], bool $allowUnavailable = false): array
    {
        $headers = json_decode((string) ($endpoint['header_json'] ?? '{}'), true) ?: [];
        $secret = SecretResolver::resolve($endpoint['secret_reference'] ?? null);
        if ($endpoint['auth_type'] === 'BEARER') $headers['Authorization'] = 'Bearer ' . $secret;
        if ($endpoint['auth_type'] === 'API_KEY') $headers['X-API-Key'] = $secret;
        $options['headers'] = ['Accept' => 'application/json', 'X-Request-Id' => bin2hex(random_bytes(12))] + $headers + ($options['headers'] ?? []);
        $options['http_errors'] = false;
        $response = (new Client([
            'timeout' => (int) $endpoint['timeout_seconds'],
            'connect_timeout' => min(10, (int) $endpoint['timeout_seconds']),
            'verify' => (bool) $endpoint['verify_tls'],
        ]))->request($method, rtrim((string) $endpoint['base_url'], '/') . '/' . ltrim($path, '/'), $options);
        $payload = json_decode((string) $response->getBody(), true);
        $status = $response->getStatusCode();
        if (!is_array($payload)) {
            throw new RuntimeException('Risposta non valida dal servizio di fatturazione elettronica.');
        }
        if (($status < 200 || $status >= 300) && !($allowUnavailable && $status === 503)) {
            throw new RuntimeException((string) ($payload['message'] ?? ('Servizio SdI HTTP ' . $status)));
        }
        return $payload;
    }

    /** @return array<string,mixed> */
    private function document(int $id): array
    {
        $row = $this->row('SELECT * FROM documents WHERE id=? AND organization_id=?', [$id, $this->organizationId]);
        if (!$row) throw new InvalidArgumentException('Documento non trovato.');
        return $row;
    }

    private function applyDocumentStatus(int $documentId, string $status): void
    {
        $documentStatus = match ($status) {
            'ACCEPTED_BY_SDI', 'SENT' => 'SENT',
            'DELIVERED', 'NOT_DELIVERED', 'DELIVERY_IMPOSSIBLE' => 'ACCEPTED',
            'REJECTED_BY_SDI', 'SDI_ERROR' => 'REJECTED',
            default => null,
        };
        if ($documentStatus !== null) {
            $this->db->prepare('UPDATE documents SET status=?,updated_by=?,updated_at=NOW() WHERE id=? AND organization_id=?')
                ->execute([$documentStatus, $this->userId ?: null, $documentId, $this->organizationId]);
        }
    }

    private function status(string $status): string
    {
        return in_array($status, [
            'QUEUED','SENDING','SENT','ACCEPTED_BY_SDI','DELIVERED','NOT_DELIVERED','REJECTED_BY_SDI',
            'TERMS_EXPIRED','DELIVERY_IMPOSSIBLE','SEND_FAILED','SDI_ERROR','ERROR',
        ], true) ? $status : 'ERROR';
    }

    private function dateTime(mixed $value): ?string
    {
        $time = $value ? strtotime((string) $value) : false;
        return $time ? date('Y-m-d H:i:s', $time) : null;
    }

    /** @return array<string,mixed>|false */
    private function row(string $sql, array $params): array|false
    {
        $statement = $this->db->prepare($sql);
        $statement->execute($params);
        return $statement->fetch();
    }
}
