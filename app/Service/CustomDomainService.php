<?php

declare(strict_types=1);

namespace Luna\Service;

use Luna\Core\Env;
use PDO;
use RuntimeException;
use Throwable;

final class CustomDomainService
{
    /** @var null|callable(string):array */
    private $dnsResolver;
    /** @var null|callable(string):array */
    private $tlsInspector;

    public function __construct(
        private readonly PDO $db,
        private readonly ?PleskApiClient $plesk = null,
        ?callable $dnsResolver = null,
        ?callable $tlsInspector = null,
    ) {
        $this->dnsResolver = $dnsResolver;
        $this->tlsInspector = $tlsInspector;
    }

    public function settings(): array
    {
        $canonical = $this->canonicalDomain();
        return [
            'canonical_domain' => $canonical,
            'cname_target' => $this->cnameTarget($canonical),
            'plesk_configured' => $this->client()->configured(),
        ];
    }

    public function all(): array
    {
        return $this->db->query('SELECT * FROM custom_domains ORDER BY created_at DESC, id DESC')->fetchAll();
    }

    public function events(int $limit = 50): array
    {
        $limit = max(1, min(200, $limit));
        return $this->db->query(
            'SELECT e.*, d.hostname FROM custom_domain_events e JOIN custom_domains d ON d.id=e.custom_domain_id ORDER BY e.id DESC LIMIT ' . $limit
        )->fetchAll();
    }

    public function add(string $hostname, int $userId): int
    {
        $hostname = self::normalizeHostname($hostname);
        $canonical = $this->canonicalDomain();
        $target = $this->cnameTarget($canonical);
        if (!$this->client()->configured()) {
            throw new RuntimeException('Il servizio di attivazione dei domini non è ancora configurato. Contatta l’assistenza Luna2.');
        }
        if ($hostname === $canonical || $hostname === $target) {
            throw new RuntimeException('Il dominio personalizzato deve essere diverso dal dominio tecnico dell’installazione.');
        }
        $statement = $this->db->prepare(
            "INSERT INTO custom_domains (hostname,cname_target,canonical_domain,status,created_by,updated_by)
             VALUES (?,?,?,'PENDING_DNS',?,?)"
        );
        try {
            $statement->execute([$hostname, $target, $canonical, $userId, $userId]);
        } catch (Throwable $exception) {
            if ((string) $exception->getCode() === '23000') {
                throw new RuntimeException('Questo dominio personalizzato è già presente.', 0, $exception);
            }
            throw $exception;
        }
        $id = (int) $this->db->lastInsertId();
        $this->event($id, 'CREATED', 'SUCCESS', 'Dominio registrato; predisposizione alias Plesk avviata.');
        $domain = $this->reconcile($id, $userId);
        if (($domain['status'] ?? '') === 'ERROR') {
            throw new RuntimeException((string) ($domain['last_error'] ?: 'Non è stato possibile predisporre il dominio. Contatta l’assistenza Luna2.'));
        }
        return $id;
    }

    public function reconcile(int $id, int $userId = 0): array
    {
        $domain = $this->find($id);
        $now = date('Y-m-d H:i:s');
        if (empty($domain['alias_provisioned_at'])) {
            if (!$this->client()->configured()) {
                $message = 'Servizio di attivazione non configurato. Contatta l’assistenza Luna2.';
                $this->update($id, ['status' => 'ERROR', 'last_checked_at' => $now, 'last_error' => $message, 'updated_by' => $userId ?: null]);
                $this->event($id, 'PLESK_ALIAS', 'ERROR', $message);
                return $this->find($id);
            }
            try {
                $aliasId = $this->client()->createAlias((string) $domain['canonical_domain'], (string) $domain['hostname']);
                $this->update($id, [
                    'status' => 'PENDING_DNS', 'plesk_alias_id' => $aliasId,
                    'alias_provisioned_at' => $now, 'last_checked_at' => $now,
                    'last_error' => null, 'updated_by' => $userId ?: null,
                ]);
                $this->event($id, 'PLESK_ALIAS', 'SUCCESS', 'Alias web predisposto; SSL It! completerà il certificato dopo la propagazione DNS.');
                $domain = $this->find($id);
            } catch (Throwable $exception) {
                $this->update($id, ['status' => 'ERROR', 'last_checked_at' => $now, 'last_error' => $exception->getMessage(), 'updated_by' => $userId ?: null]);
                $this->event($id, 'PLESK_ALIAS', 'ERROR', $exception->getMessage());
                return $this->find($id);
            }
        }

        if (!$this->dnsPointsTo((string) $domain['hostname'], (string) $domain['cname_target'])) {
            $message = 'CNAME non ancora rilevato. Configura ' . $domain['hostname'] . ' → ' . $domain['cname_target'] . '.';
            $this->update($id, ['status' => 'PENDING_DNS', 'last_checked_at' => $now, 'last_error' => $message, 'updated_by' => $userId ?: null]);
            $this->event($id, 'DNS_CHECK', 'PENDING', $message);
            return $this->find($id);
        }

        $this->update($id, ['status' => 'DNS_VERIFIED', 'dns_verified_at' => $domain['dns_verified_at'] ?: $now, 'last_checked_at' => $now, 'last_error' => null, 'updated_by' => $userId ?: null]);
        $domain = $this->find($id);

        $tls = $this->inspectTls((string) $domain['hostname']);
        if (($tls['valid'] ?? false) === true) {
            $this->update($id, [
                'status' => 'ACTIVE', 'ssl_issued_at' => $tls['valid_from'] ?? $now,
                'ssl_expires_at' => $tls['valid_to'] ?? null, 'certificate_issuer' => $tls['issuer'] ?? null,
                'last_checked_at' => $now, 'last_error' => null,
            ]);
            $this->event($id, 'TLS_CHECK', 'SUCCESS', 'Certificato HTTPS valido per il dominio.');
        } else {
            $message = (string) ($tls['error'] ?? 'Certificato non ancora disponibile. SSL It! completerà automaticamente l’emissione.');
            $this->update($id, ['status' => 'SSL_PENDING', 'last_checked_at' => $now, 'last_error' => $message]);
            $this->event($id, 'TLS_CHECK', 'PENDING', $message);
        }
        return $this->find($id);
    }

    public function reconcileAll(int $userId = 0): array
    {
        $ids = $this->db->query("SELECT id FROM custom_domains WHERE status <> 'REMOVING' ORDER BY id")->fetchAll(PDO::FETCH_COLUMN);
        $result = ['checked' => 0, 'active' => 0, 'pending' => 0, 'errors' => 0];
        foreach ($ids as $id) {
            try {
                $domain = $this->reconcile((int) $id, $userId);
                $result['checked']++;
                if ($domain['status'] === 'ACTIVE') $result['active']++;
                elseif ($domain['status'] === 'ERROR') $result['errors']++;
                else $result['pending']++;
            } catch (Throwable $exception) {
                $result['checked']++;
                $result['errors']++;
                $this->update((int) $id, ['status' => 'ERROR', 'last_error' => $exception->getMessage(), 'last_checked_at' => date('Y-m-d H:i:s')]);
                $this->event((int) $id, 'RECONCILE', 'ERROR', $exception->getMessage());
            }
        }
        return $result;
    }

    public function remove(int $id): void
    {
        $domain = $this->find($id);
        if (!empty($domain['alias_provisioned_at'])) {
            if (!$this->client()->configured()) {
                throw new RuntimeException('Configura l’API Plesk prima di rimuovere un alias già pubblicato.');
            }
            $this->client()->deleteAlias((string) $domain['hostname']);
        }
        $this->db->prepare('DELETE FROM custom_domains WHERE id=?')->execute([$id]);
    }

    public static function normalizeHostname(string $hostname): string
    {
        $hostname = mb_strtolower(trim($hostname));
        $hostname = rtrim($hostname, '.');
        if (function_exists('idn_to_ascii')) {
            $ascii = idn_to_ascii($hostname, IDNA_DEFAULT, INTL_IDNA_VARIANT_UTS46);
            if ($ascii !== false) $hostname = mb_strtolower($ascii);
        }
        if (strlen($hostname) > 253 || !preg_match('/^(?=.{1,253}$)(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?$/', $hostname)) {
            throw new RuntimeException('Inserisci un nome host completo valido, per esempio gestionale.cliente.it.');
        }
        if (filter_var($hostname, FILTER_VALIDATE_IP) || str_contains($hostname, '*')) {
            throw new RuntimeException('Indirizzi IP e wildcard non sono ammessi.');
        }
        return $hostname;
    }

    private function canonicalDomain(): string
    {
        $configured = trim((string) Env::get('PLESK_PRIMARY_DOMAIN', ''));
        if ($configured !== '') return self::normalizeHostname($configured);
        $value = $this->db->query('SELECT canonical_domain FROM instance_identity WHERE id=1')->fetchColumn();
        $domain = trim((string) $value);
        if ($domain === '') $domain = (string) parse_url((string) Env::get('APP_URL', ''), PHP_URL_HOST);
        if ($domain === '') throw new RuntimeException('Dominio canonico non configurato in APP_URL o PLESK_PRIMARY_DOMAIN.');
        return self::normalizeHostname($domain);
    }

    private function cnameTarget(string $canonical): string
    {
        $target = trim((string) Env::get('CUSTOM_DOMAIN_CNAME_TARGET', ''));
        return $target === '' ? $canonical : self::normalizeHostname($target);
    }

    private function dnsPointsTo(string $hostname, string $expected): bool
    {
        $resolver = $this->dnsResolver ?? static fn (string $name): array => dns_get_record($name, DNS_CNAME) ?: [];
        $current = $hostname;
        for ($hop = 0; $hop < 6; $hop++) {
            $records = $resolver($current);
            if (!is_array($records) || $records === []) return false;
            $target = '';
            foreach ($records as $record) {
                if (isset($record['target'])) { $target = rtrim(mb_strtolower((string) $record['target']), '.'); break; }
            }
            if ($target === '') return false;
            if ($target === $expected) return true;
            if ($target === $current) return false;
            $current = $target;
        }
        return false;
    }

    private function inspectTls(string $hostname): array
    {
        if ($this->tlsInspector !== null) return (array) ($this->tlsInspector)($hostname);
        $context = stream_context_create(['ssl' => [
            'capture_peer_cert' => true, 'verify_peer' => true, 'verify_peer_name' => true,
            'peer_name' => $hostname, 'SNI_enabled' => true,
        ]]);
        $error = '';
        $socket = @stream_socket_client('ssl://' . $hostname . ':443', $errno, $error, 8, STREAM_CLIENT_CONNECT, $context);
        if ($socket === false) return ['valid' => false, 'error' => 'HTTPS non ancora valido: ' . ($error ?: 'connessione non disponibile')];
        $params = stream_context_get_params($socket);
        fclose($socket);
        $certificate = $params['options']['ssl']['peer_certificate'] ?? null;
        $parsed = $certificate ? openssl_x509_parse($certificate) : false;
        if (!is_array($parsed)) return ['valid' => false, 'error' => 'Certificato HTTPS non leggibile.'];
        return [
            'valid' => true,
            'valid_from' => isset($parsed['validFrom_time_t']) ? date('Y-m-d H:i:s', (int) $parsed['validFrom_time_t']) : null,
            'valid_to' => isset($parsed['validTo_time_t']) ? date('Y-m-d H:i:s', (int) $parsed['validTo_time_t']) : null,
            'issuer' => implode(', ', array_filter((array) ($parsed['issuer'] ?? []), 'is_scalar')),
        ];
    }

    private function client(): PleskApiClient { return $this->plesk ?? new PleskApiClient(); }

    private function find(int $id): array
    {
        $statement = $this->db->prepare('SELECT * FROM custom_domains WHERE id=?');
        $statement->execute([$id]);
        $row = $statement->fetch();
        if (!$row) throw new RuntimeException('Dominio personalizzato non trovato.');
        return $row;
    }

    private function update(int $id, array $values): void
    {
        if ($values === []) return;
        $allowed = ['status','plesk_alias_id','dns_verified_at','alias_provisioned_at','ssl_issued_at','ssl_expires_at','certificate_issuer','last_checked_at','last_error','updated_by'];
        $set = []; $parameters = [];
        foreach ($values as $column => $value) {
            if (!in_array($column, $allowed, true)) throw new RuntimeException('Campo dominio non aggiornabile.');
            $set[] = '`' . $column . '`=?'; $parameters[] = $value;
        }
        $parameters[] = $id;
        $this->db->prepare('UPDATE custom_domains SET ' . implode(',', $set) . ' WHERE id=?')->execute($parameters);
    }

    private function event(int $id, string $type, string $result, string $detail): void
    {
        $latest = $this->db->prepare('SELECT event_type,result,detail FROM custom_domain_events WHERE custom_domain_id=? ORDER BY id DESC LIMIT 1');
        $latest->execute([$id]);
        $previous = $latest->fetch();
        if ($previous && $previous['event_type'] === $type && $previous['result'] === $result && $previous['detail'] === mb_substr($detail, 0, 5000)) {
            return;
        }
        $this->db->prepare('INSERT INTO custom_domain_events (custom_domain_id,event_type,result,detail) VALUES (?,?,?,?)')
            ->execute([$id, $type, $result, mb_substr($detail, 0, 5000)]);
    }
}
