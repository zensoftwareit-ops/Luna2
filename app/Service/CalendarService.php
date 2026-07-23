<?php

declare(strict_types=1);

namespace Luna\Service;

use GuzzleHttp\Client;
use InvalidArgumentException;
use PDO;
use Throwable;

final class CalendarService
{
    private Client $http;

    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId = 0)
    {
        $this->http = new Client(['timeout' => 30, 'connect_timeout' => 10, 'http_errors' => true]);
    }

    public function sync(int $accountId): array
    {
        $account = $this->account($accountId);
        $credentials = json_decode(SecretResolver::resolve($account['secret_reference'] ?? null), true);
        if (!is_array($credentials)) { throw new InvalidArgumentException('Il segreto calendario deve contenere JSON valido.'); }
        $this->db->prepare("INSERT INTO calendar_sync_logs (organization_id, calendar_account_id, direction, status, started_at) VALUES (?, ?, 'BIDIRECTIONAL', 'RUNNING', NOW())")->execute([$this->organizationId, $accountId]);
        $logId = (int) $this->db->lastInsertId(); $processed = 0; $errors = 0;
        try {
            foreach ($this->all("SELECT * FROM calendar_events WHERE organization_id = ? AND calendar_account_id = ? AND sync_status IN ('LOCAL','PENDING','ERROR') AND deleted_external = 0", [$this->organizationId, $accountId]) as $event) {
                try { $this->push($account, $credentials, $event); $processed++; }
                catch (Throwable $e) { $errors++; $this->db->prepare("UPDATE calendar_events SET sync_status = 'ERROR', sync_error = ? WHERE id = ?")->execute([mb_substr($e->getMessage(), 0, 2000), $event['id']]); }
            }
            foreach ($this->pull($account, $credentials) as $external) { $this->upsert($account, $external); $processed++; }
            $status = $errors === 0 ? 'SUCCESS' : ($processed > 0 ? 'PARTIAL' : 'ERROR');
            $this->db->prepare('UPDATE calendar_sync_logs SET status = ?, processed_count = ?, error_count = ?, ended_at = NOW() WHERE id = ?')->execute([$status, $processed, $errors, $logId]);
            $this->db->prepare('UPDATE calendar_accounts SET last_sync_at = NOW(), last_error = ?, updated_at = NOW() WHERE id = ?')->execute([$errors ? $errors . ' eventi non sincronizzati' : null, $accountId]);
            return compact('processed', 'errors');
        } catch (Throwable $exception) {
            $this->db->prepare("UPDATE calendar_sync_logs SET status = 'ERROR', error_count = error_count + 1, ended_at = NOW(), message = ? WHERE id = ?")->execute([mb_substr($exception->getMessage(), 0, 2000), $logId]);
            $this->db->prepare('UPDATE calendar_accounts SET last_error = ?, updated_at = NOW() WHERE id = ?')->execute([mb_substr($exception->getMessage(), 0, 2000), $accountId]);
            throw $exception;
        }
    }

    public function syncAll(): array
    {
        $ok = 0; $failed = 0;
        foreach ($this->all('SELECT id FROM calendar_accounts WHERE organization_id = ? AND active = 1', [$this->organizationId]) as $account) {
            try { $this->sync((int) $account['id']); $ok++; } catch (Throwable) { $failed++; }
        }
        return compact('ok', 'failed');
    }

    private function push(array $account, array &$credentials, array $event): void
    {
        if ($account['provider'] === 'GOOGLE') {
            $token = $this->googleToken($credentials);
            $calendarId = rawurlencode((string) ($account['external_calendar_id'] ?: 'primary'));
            $body = ['summary' => $event['title'], 'description' => $event['description'], 'location' => $event['location'], 'start' => $event['all_day'] ? ['date' => substr((string) $event['starts_at'], 0, 10)] : ['dateTime' => date(DATE_RFC3339, strtotime((string) $event['starts_at']))], 'end' => $event['all_day'] ? ['date' => substr((string) ($event['ends_at'] ?: $event['starts_at']), 0, 10)] : ['dateTime' => date(DATE_RFC3339, strtotime((string) ($event['ends_at'] ?: $event['starts_at'])))] ];
            $method = $event['provider_event_id'] ? 'PUT' : 'POST';
            $url = 'https://www.googleapis.com/calendar/v3/calendars/' . $calendarId . '/events' . ($event['provider_event_id'] ? '/' . rawurlencode((string) $event['provider_event_id']) : '');
            $response = $this->http->request($method, $url, ['headers' => ['Authorization' => 'Bearer ' . $token], 'json' => $body]);
            $external = json_decode((string) $response->getBody(), true, 512, JSON_THROW_ON_ERROR);
            $this->db->prepare("UPDATE calendar_events SET provider = 'GOOGLE', provider_event_id = ?, etag = ?, sync_status = 'SYNCED', last_synced_at = NOW(), sync_error = NULL WHERE id = ?")
                ->execute([$external['id'] ?? $event['provider_event_id'], $external['etag'] ?? null, $event['id']]);
            return;
        }
        $endpoint = rtrim((string) $account['endpoint_url'], '/');
        if (!str_starts_with($endpoint, 'https://')) { throw new InvalidArgumentException('L’endpoint CalDAV deve usare HTTPS.'); }
        $uid = $event['provider_event_id'] ?: 'luna-' . $this->organizationId . '-' . $event['id'];
        $ics = $this->ics($event, $uid);
        $auth = $this->caldavAuth($account, $credentials);
        $options = $auth;
        $options['headers'] = array_merge($auth['headers'] ?? [], ['Content-Type' => 'text/calendar; charset=utf-8', 'If-Match' => $event['etag'] ?: '*']);
        $options['body'] = $ics;
        if (!$event['etag']) { $options['headers']['If-None-Match'] = '*'; unset($options['headers']['If-Match']); }
        $response = $this->http->put($endpoint . '/' . rawurlencode($uid) . '.ics', $options);
        $this->db->prepare("UPDATE calendar_events SET provider = ?, provider_event_id = ?, etag = ?, sync_status = 'SYNCED', last_synced_at = NOW(), sync_error = NULL WHERE id = ?")
            ->execute([$account['provider'], $uid, $response->getHeaderLine('ETag') ?: null, $event['id']]);
    }

    private function pull(array $account, array &$credentials): array
    {
        if ($account['provider'] === 'GOOGLE') {
            $token = $this->googleToken($credentials); $calendarId = rawurlencode((string) ($account['external_calendar_id'] ?: 'primary'));
            $query = ['singleEvents' => 'true', 'maxResults' => 2500, 'timeMin' => gmdate(DATE_RFC3339, strtotime('-1 year'))];
            if ($account['sync_token']) { $query = ['syncToken' => $account['sync_token'], 'singleEvents' => 'true', 'maxResults' => 2500]; }
            $payload = json_decode((string) $this->http->get('https://www.googleapis.com/calendar/v3/calendars/' . $calendarId . '/events', ['headers' => ['Authorization' => 'Bearer ' . $token], 'query' => $query])->getBody(), true, 512, JSON_THROW_ON_ERROR);
            if (!empty($payload['nextSyncToken'])) { $this->db->prepare('UPDATE calendar_accounts SET sync_token = ? WHERE id = ?')->execute([$payload['nextSyncToken'], $account['id']]); }
            return array_map(static fn (array $e): array => ['id' => $e['id'] ?? '', 'etag' => $e['etag'] ?? null, 'title' => $e['summary'] ?? '(senza titolo)', 'description' => $e['description'] ?? null, 'location' => $e['location'] ?? null, 'starts_at' => $e['start']['dateTime'] ?? $e['start']['date'] ?? date('c'), 'ends_at' => $e['end']['dateTime'] ?? $e['end']['date'] ?? null, 'all_day' => isset($e['start']['date']), 'cancelled' => ($e['status'] ?? '') === 'cancelled'], $payload['items'] ?? []);
        }
        $endpoint = rtrim((string) $account['endpoint_url'], '/');
        $xml = '<?xml version="1.0" encoding="UTF-8"?><c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav"><d:prop><d:getetag/><c:calendar-data/></d:prop><c:filter><c:comp-filter name="VCALENDAR"><c:comp-filter name="VEVENT"/></c:comp-filter></c:filter></c:calendar-query>';
        $auth = $this->caldavAuth($account, $credentials);
        $options = $auth;
        $options['headers'] = array_merge($auth['headers'] ?? [], ['Depth' => '1', 'Content-Type' => 'application/xml; charset=utf-8']);
        $options['body'] = $xml;
        $response = $this->http->request('REPORT', $endpoint . '/', $options);
        return $this->parseCalDav((string) $response->getBody());
    }

    private function upsert(array $account, array $e): void
    {
        if (($e['id'] ?? '') === '') { return; }
        $provider = $account['provider'];
        $existing = $this->one('SELECT id FROM calendar_events WHERE organization_id = ? AND provider = ? AND provider_event_id = ?', [$this->organizationId, $provider, $e['id']]);
        if (!empty($e['cancelled'])) { if ($existing) { $this->db->prepare('UPDATE calendar_events SET deleted_external = 1, sync_status = \'SYNCED\', last_synced_at = NOW() WHERE id = ?')->execute([$existing['id']]); } return; }
        if ($existing) {
            $this->db->prepare("UPDATE calendar_events SET calendar_account_id = ?, title = ?, starts_at = ?, ends_at = ?, all_day = ?, location = ?, etag = ?, sync_status = 'SYNCED', description = ?, last_synced_at = NOW(), sync_error = NULL WHERE id = ?")
                ->execute([$account['id'], $e['title'], $this->sqlDate($e['starts_at']), $e['ends_at'] ? $this->sqlDate($e['ends_at']) : null, $e['all_day'] ? 1 : 0, $e['location'], $e['etag'], $e['description'], $existing['id']]);
        } else {
            $this->db->prepare("INSERT INTO calendar_events (organization_id, calendar_account_id, title, starts_at, ends_at, all_day, location, provider, provider_event_id, etag, sync_status, description, last_synced_at, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'SYNCED', ?, NOW(), ?, ?, NOW(), NOW())")
                ->execute([$this->organizationId, $account['id'], $e['title'], $this->sqlDate($e['starts_at']), $e['ends_at'] ? $this->sqlDate($e['ends_at']) : null, $e['all_day'] ? 1 : 0, $e['location'], $provider, $e['id'], $e['etag'], $e['description'], $this->userId ?: null, $this->userId ?: null]);
        }
    }

    private function googleToken(array &$credentials): string
    {
        if (!empty($credentials['access_token']) && (int) ($credentials['expires_at'] ?? PHP_INT_MAX) > time() + 60) { return (string) $credentials['access_token']; }
        foreach (['refresh_token','client_id','client_secret'] as $required) { if (empty($credentials[$required])) { throw new InvalidArgumentException('Credenziali OAuth Google incomplete.'); } }
        $payload = json_decode((string) $this->http->post('https://oauth2.googleapis.com/token', ['form_params' => ['client_id' => $credentials['client_id'], 'client_secret' => $credentials['client_secret'], 'refresh_token' => $credentials['refresh_token'], 'grant_type' => 'refresh_token']])->getBody(), true, 512, JSON_THROW_ON_ERROR);
        return (string) ($payload['access_token'] ?? throw new InvalidArgumentException('Google non ha restituito un access token.'));
    }

    private function caldavAuth(array $account, array $credentials): array
    {
        return match ($account['auth_type']) {
            'BEARER', 'OAUTH2' => ['headers' => ['Authorization' => 'Bearer ' . ($credentials['access_token'] ?? '')]],
            default => ['auth' => [$credentials['username'] ?? $account['account_email'], $credentials['password'] ?? '']],
        };
    }

    private function parseCalDav(string $xml): array
    {
        $events = [];
        if (!preg_match_all('/<[^>]*calendar-data[^>]*>(.*?)<\/[^>]*calendar-data>/si', $xml, $matches)) { return []; }
        foreach ($matches[1] as $encoded) {
            $ics = html_entity_decode(strip_tags($encoded), ENT_QUOTES | ENT_XML1);
            $get = static function (string $ics, string $key): ?string { return preg_match('/^' . preg_quote($key, '/') . '(?:;[^:]*)?:(.*)$/mi', $ics, $m) ? trim($m[1]) : null; };
            $start = $get($ics, 'DTSTART'); $end = $get($ics, 'DTEND'); $uid = $get($ics, 'UID');
            if (!$uid || !$start) { continue; }
            $events[] = ['id' => $uid, 'etag' => null, 'title' => $get($ics, 'SUMMARY') ?: '(senza titolo)', 'description' => $get($ics, 'DESCRIPTION'), 'location' => $get($ics, 'LOCATION'), 'starts_at' => $this->parseIcsDate($start), 'ends_at' => $end ? $this->parseIcsDate($end) : null, 'all_day' => strlen($start) === 8, 'cancelled' => strtoupper((string) $get($ics, 'STATUS')) === 'CANCELLED'];
        }
        return $events;
    }

    private function ics(array $e, string $uid): string
    {
        $escape = static fn (mixed $v): string => str_replace(["\\", ";", ",", "\r", "\n"], ["\\\\", "\\;", "\\,", '', '\\n'], (string) $v);
        $start = !empty($e['all_day']) ? 'DTSTART;VALUE=DATE:' . date('Ymd', strtotime((string) $e['starts_at'])) : 'DTSTART:' . gmdate('Ymd\THis\Z', strtotime((string) $e['starts_at']));
        $end = !empty($e['all_day']) ? 'DTEND;VALUE=DATE:' . date('Ymd', strtotime((string) ($e['ends_at'] ?: $e['starts_at']))) : 'DTEND:' . gmdate('Ymd\THis\Z', strtotime((string) ($e['ends_at'] ?: $e['starts_at'])));
        return implode("\r\n", ['BEGIN:VCALENDAR','VERSION:2.0','PRODID:-//Luna2//ERP//IT','BEGIN:VEVENT','UID:' . $escape($uid),'DTSTAMP:' . gmdate('Ymd\THis\Z'),$start,$end,'SUMMARY:' . $escape($e['title']),'DESCRIPTION:' . $escape($e['description'] ?? ''),'LOCATION:' . $escape($e['location'] ?? ''),'END:VEVENT','END:VCALENDAR','']);
    }

    private function parseIcsDate(string $value): string { $format = strlen($value) === 8 ? 'Ymd' : (str_ends_with($value, 'Z') ? 'Ymd\THis\Z' : 'Ymd\THis'); $d = \DateTimeImmutable::createFromFormat($format, $value); return $d ? $d->format('Y-m-d H:i:s') : date('Y-m-d H:i:s'); }
    private function sqlDate(string $value): string { return date('Y-m-d H:i:s', strtotime($value)); }
    private function account(int $id): array { $r = $this->one('SELECT * FROM calendar_accounts WHERE id = ? AND organization_id = ? AND active = 1', [$id, $this->organizationId]); if (!$r) { throw new InvalidArgumentException('Account calendario non valido.'); } return $r; }
    private function one(string $sql, array $params): array|false { $s = $this->db->prepare($sql); $s->execute($params); return $s->fetch(); }
    private function all(string $sql, array $params): array { $s = $this->db->prepare($sql); $s->execute($params); return $s->fetchAll(); }
}
