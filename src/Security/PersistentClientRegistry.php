<?php

declare(strict_types=1);

namespace LunaApi\Security;

use InvalidArgumentException;
use RuntimeException;

final class PersistentClientRegistry
{
    private string $registryPath;
    private string $lockPath;

    public function __construct(string $directory)
    {
        if (!is_dir($directory) && !mkdir($directory, 0750, true) && !is_dir($directory)) {
            throw new RuntimeException('Impossibile inizializzare il registro client.');
        }
        $this->registryPath = rtrim($directory, '/\\') . '/registry.json';
        $this->lockPath = rtrim($directory, '/\\') . '/registry.lock';
    }

    /** @return array{client_id:string,token:string,recipients:list<string>} */
    public function create(string $clientId, array $recipients): array
    {
        $clientId = $this->clientId($clientId);
        $recipients = $this->recipients($recipients);
        return $this->mutate(function (array &$data) use ($clientId, $recipients): array {
            if (isset($data['clients'][$clientId])) {
                throw new InvalidArgumentException('Client già registrato; usa la rotazione del token o aggiorna le mappature.');
            }
            $this->assertRecipientsAvailable($data, $clientId, $recipients);
            $token = $this->token($clientId);
            $now = gmdate('c');
            $data['clients'][$clientId] = [
                'token_hash' => password_hash($token, PASSWORD_DEFAULT),
                'recipients' => $recipients,
                'active' => true,
                'created_at' => $now,
                'updated_at' => $now,
                'rotated_at' => null,
            ];
            return ['client_id' => $clientId, 'token' => $token, 'recipients' => $recipients];
        });
    }

    /** @return array{client_id:string,token:string,recipients:list<string>} */
    public function rotate(string $clientId): array
    {
        $clientId = $this->clientId($clientId);
        return $this->mutate(function (array &$data) use ($clientId): array {
            if (!isset($data['clients'][$clientId])) {
                throw new InvalidArgumentException('Client non trovato.');
            }
            $token = $this->token($clientId);
            $data['clients'][$clientId]['token_hash'] = password_hash($token, PASSWORD_DEFAULT);
            $data['clients'][$clientId]['active'] = true;
            $data['clients'][$clientId]['rotated_at'] = gmdate('c');
            $data['clients'][$clientId]['updated_at'] = gmdate('c');
            return [
                'client_id' => $clientId,
                'token' => $token,
                'recipients' => array_values($data['clients'][$clientId]['recipients'] ?? []),
            ];
        });
    }

    /** @return array{client_id:string,recipients:list<string>,active:bool} */
    public function update(string $clientId, array $recipients, bool $active = true): array
    {
        $clientId = $this->clientId($clientId);
        $recipients = $this->recipients($recipients);
        return $this->mutate(function (array &$data) use ($clientId, $recipients, $active): array {
            if (!isset($data['clients'][$clientId])) {
                throw new InvalidArgumentException('Client non trovato.');
            }
            if ($active) $this->assertRecipientsAvailable($data, $clientId, $recipients);
            $data['clients'][$clientId]['recipients'] = $recipients;
            $data['clients'][$clientId]['active'] = $active;
            $data['clients'][$clientId]['updated_at'] = gmdate('c');
            return ['client_id' => $clientId, 'recipients' => $recipients, 'active' => $active];
        });
    }

    public function authenticate(string $token): ?string
    {
        if (!preg_match('/^l2\.([A-Za-z0-9_-]+)\.([A-Za-z0-9_-]{32,})$/', $token, $matches)) return null;
        $decoded = base64_decode(strtr($matches[1], '-_', '+/'), true);
        if ($decoded === false) return null;
        $clientId = $this->clientId($decoded);
        $data = $this->read();
        $client = $data['clients'][$clientId] ?? null;
        if (!is_array($client) || empty($client['active']) || empty($client['token_hash'])) return null;
        return password_verify($token, (string) $client['token_hash']) ? $clientId : null;
    }

    /** @return array<string,string> */
    public function recipientMap(): array
    {
        $map = [];
        foreach (($this->read()['clients'] ?? []) as $clientId => $client) {
            if (!is_array($client) || empty($client['active'])) continue;
            foreach ((array) ($client['recipients'] ?? []) as $recipient) $map[(string) $recipient] = (string) $clientId;
        }
        return $map;
    }

    /** @return list<array{client_id:string,recipients:list<string>,active:bool,created_at:?string,updated_at:?string,rotated_at:?string}> */
    public function clients(): array
    {
        $rows = [];
        foreach (($this->read()['clients'] ?? []) as $clientId => $client) {
            if (!is_array($client)) continue;
            $rows[] = [
                'client_id' => (string) $clientId,
                'recipients' => array_values((array) ($client['recipients'] ?? [])),
                'active' => !empty($client['active']),
                'created_at' => isset($client['created_at']) ? (string) $client['created_at'] : null,
                'updated_at' => isset($client['updated_at']) ? (string) $client['updated_at'] : null,
                'rotated_at' => isset($client['rotated_at']) ? (string) $client['rotated_at'] : null,
            ];
        }
        usort($rows, static fn (array $a, array $b): int => strcmp($a['client_id'], $b['client_id']));
        return $rows;
    }

    private function token(string $clientId): string
    {
        $encodedId = rtrim(strtr(base64_encode($clientId), '+/', '-_'), '=');
        $secret = rtrim(strtr(base64_encode(random_bytes(32)), '+/', '-_'), '=');
        return 'l2.' . $encodedId . '.' . $secret;
    }

    private function clientId(string $value): string
    {
        $value = trim($value);
        if (!preg_match('/^[A-Za-z0-9._-]{2,80}$/', $value)) throw new InvalidArgumentException('Identificativo client non valido.');
        return $value;
    }

    /** @return list<string> */
    private function recipients(array $values): array
    {
        $result = [];
        foreach ($values as $value) {
            $value = strtoupper(preg_replace('/\s+/', '', (string) $value) ?? '');
            if (preg_match('/^IT([0-9]{11})$/', $value, $match)) $value = $match[1];
            if (!preg_match('/^[A-Z0-9]{5,32}$/', $value)) throw new InvalidArgumentException('Partita IVA o codice fiscale destinatario non valido.');
            $result[$value] = true;
        }
        if ($result === []) throw new InvalidArgumentException('Indica almeno una partita IVA o un codice fiscale destinatario.');
        return array_keys($result);
    }

    private function assertRecipientsAvailable(array $data, string $clientId, array $recipients): void
    {
        foreach (($data['clients'] ?? []) as $existingId => $client) {
            if ($existingId === $clientId || !is_array($client) || empty($client['active'])) continue;
            $duplicate = array_intersect($recipients, (array) ($client['recipients'] ?? []));
            if ($duplicate !== []) throw new InvalidArgumentException('Destinatario già assegnato al client ' . $existingId . ': ' . reset($duplicate));
        }
    }

    private function read(): array
    {
        $lock = fopen($this->lockPath, 'c+');
        if ($lock === false || !flock($lock, LOCK_SH)) throw new RuntimeException('Registro client temporaneamente non disponibile.');
        try { return $this->readUnlocked(); } finally { flock($lock, LOCK_UN); fclose($lock); }
    }

    private function mutate(callable $callback): mixed
    {
        $lock = fopen($this->lockPath, 'c+');
        if ($lock === false || !flock($lock, LOCK_EX)) throw new RuntimeException('Registro client temporaneamente non disponibile.');
        try {
            $data = $this->readUnlocked();
            $result = $callback($data);
            $json = json_encode($data, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES | JSON_THROW_ON_ERROR);
            $temporary = $this->registryPath . '.tmp.' . bin2hex(random_bytes(5));
            if (file_put_contents($temporary, $json . PHP_EOL, LOCK_EX) === false) throw new RuntimeException('Impossibile aggiornare il registro client.');
            @chmod($temporary, 0640);
            if (!rename($temporary, $this->registryPath)) { @unlink($temporary); throw new RuntimeException('Impossibile pubblicare il registro client.'); }
            return $result;
        } finally { flock($lock, LOCK_UN); fclose($lock); }
    }

    private function readUnlocked(): array
    {
        if (!is_file($this->registryPath)) return ['version' => 1, 'clients' => []];
        $decoded = json_decode((string) file_get_contents($this->registryPath), true);
        if (!is_array($decoded) || !is_array($decoded['clients'] ?? null)) throw new RuntimeException('Registro client danneggiato.');
        return $decoded;
    }
}
