<?php

declare(strict_types=1);

namespace Luna\Service;

use DOMDocument;
use DOMXPath;
use GuzzleHttp\Client;
use Luna\Core\Env;
use RuntimeException;

final class PleskApiClient
{
    /** @var null|callable(string):string */
    private $transport;

    public function __construct(?callable $transport = null)
    {
        $this->transport = $transport;
    }

    public function configured(): bool
    {
        return $this->transport !== null || (
            trim((string) Env::get('PLESK_API_URL', '')) !== ''
            && trim((string) Env::get('PLESK_API_KEY', '')) !== ''
        );
    }

    public function createAlias(string $canonicalDomain, string $alias): ?int
    {
        $siteId = $this->siteId($canonicalDomain);
        // AliasInfoType richiede questo ordine: impostazioni, manage-dns, site-id, name.
        $packet = '<packet><site-alias><create><status>0</status>'
            . '<pref><web>1</web><mail>0</mail><tomcat>0</tomcat><seo-redirect>0</seo-redirect></pref>'
            . '<manage-dns>0</manage-dns><site-id>' . $siteId . '</site-id><name>'
            . self::xml($alias) . '</name></create></site-alias></packet>';

        $response = $this->request($packet);
        return $this->firstInteger($response, '//site-alias/create/result/id');
    }

    public function ensureAlias(string $canonicalDomain, string $alias): ?int
    {
        $expectedSiteId = $this->siteId($canonicalDomain);
        $existing = $this->aliasInfo($alias);
        if ($existing !== null) {
            if ((int) $existing['site_id'] !== $expectedSiteId) {
                throw new RuntimeException(
                    'Il dominio esiste già in Plesk ma non appartiene al webspace ' . $canonicalDomain
                    . '. Rimuovi l’oggetto in conflitto da Plesk e riprova.'
                );
            }
            if (!$existing['web_enabled']) {
                throw new RuntimeException('L’alias esiste in Plesk ma il servizio web non è abilitato. Abilitalo e riprova.');
            }
            // Forza Plesk a rigenerare il virtual host: un alias presente nel database
            // può altrimenti continuare a mostrare la pagina predefinita del server.
            $this->cli('domalias', ['--update', $alias, '-web', 'true', '-mail', 'false']);
            return $existing['id'];
        }

        $id = $this->createAlias($canonicalDomain, $alias);
        $this->cli('domalias', ['--update', $alias, '-web', 'true', '-mail', 'false']);
        return $id;
    }

    public function issueCertificateWithAliases(string $canonicalDomain): string
    {
        $result = $this->cli('extension', [
            '--call', 'sslit', '--certificate', '-issue', '-domain', $canonicalDomain,
            '-secure-domain', '-aliases',
        ], 120);
        return trim((string) ($result['stdout'] ?? ''));
    }

    public function deleteAlias(string $alias): void
    {
        $packet = '<packet><site-alias><delete><filter><name>' . self::xml($alias)
            . '</name></filter></delete></site-alias></packet>';
        try {
            $this->request($packet);
        } catch (RuntimeException $exception) {
            $message = mb_strtolower($exception->getMessage());
            if (!str_contains($message, 'not exist') && !str_contains($message, 'non esist')) {
                throw $exception;
            }
        }
    }

    private function siteId(string $canonicalDomain): int
    {
        $packet = '<packet><site><get><filter><name>' . self::xml($canonicalDomain)
            . '</name></filter><dataset><gen_info/></dataset></get></site></packet>';
        $id = $this->firstInteger($this->request($packet), '//site/get/result/id');
        if ($id === null || $id <= 0) {
            throw new RuntimeException('Plesk non ha restituito il webspace del dominio canonico ' . $canonicalDomain . '.');
        }
        return $id;
    }

    private function aliasInfo(string $alias): ?array
    {
        $packet = '<packet><site-alias><get><filter><name>' . self::xml($alias)
            . '</name></filter></get></site-alias></packet>';
        $xpath = $this->request($packet, true);
        $result = $xpath->query('//site-alias/get/result[status="ok"]')->item(0);
        if ($result === null) {
            return null;
        }

        $id = trim((string) $xpath->evaluate('string(id)', $result));
        $siteId = trim((string) $xpath->evaluate('string(info/site-id)', $result));
        $web = mb_strtolower(trim((string) $xpath->evaluate('string(info/pref/web | info/prefs/web)', $result)));
        if (!ctype_digit($siteId)) {
            throw new RuntimeException('Plesk non ha restituito il webspace associato all’alias ' . $alias . '.');
        }
        return [
            'id' => ctype_digit($id) ? (int) $id : null,
            'site_id' => (int) $siteId,
            'web_enabled' => in_array($web, ['1', 'true'], true),
        ];
    }

    private function request(string $packet, bool $allowApiErrors = false): DOMXPath
    {
        if ($this->transport !== null) {
            $body = (string) ($this->transport)($packet);
        } else {
            $url = rtrim((string) Env::get('PLESK_API_URL', ''), '/');
            $key = trim((string) Env::get('PLESK_API_KEY', ''));
            if ($url === '' || $key === '') {
                throw new RuntimeException('Configura PLESK_API_URL e PLESK_API_KEY nel file .env.');
            }
            if (parse_url($url, PHP_URL_SCHEME) !== 'https' || !parse_url($url, PHP_URL_HOST)) {
                throw new RuntimeException('PLESK_API_URL deve essere un indirizzo HTTPS completo e valido.');
            }
            $client = new Client([
                'base_uri' => $url . '/',
                'timeout' => 20,
                'connect_timeout' => 8,
                'verify' => Env::bool('PLESK_API_VERIFY_TLS', true),
            ]);
            try {
                $response = $client->post('enterprise/control/agent.php', [
                    'headers' => ['KEY' => $key, 'Content-Type' => 'text/xml; charset=UTF-8'],
                    'body' => $packet,
                ]);
                $body = (string) $response->getBody();
            } catch (\Throwable $exception) {
                throw new RuntimeException('Plesk non raggiungibile: ' . $exception->getMessage(), 0, $exception);
            }
        }

        $dom = new DOMDocument();
        $previous = libxml_use_internal_errors(true);
        $loaded = $dom->loadXML($body, LIBXML_NONET | LIBXML_NOBLANKS);
        libxml_clear_errors();
        libxml_use_internal_errors($previous);
        if (!$loaded) {
            throw new RuntimeException('Plesk ha restituito una risposta XML non valida.');
        }
        $xpath = new DOMXPath($dom);
        $errors = $xpath->query('//result[status="error"]');
        if (!$allowApiErrors && $errors !== false && $errors->length > 0) {
            $node = $errors->item(0);
            $code = trim((string) $xpath->evaluate('string(errcode)', $node));
            $text = trim((string) $xpath->evaluate('string(errtext)', $node));
            throw new RuntimeException('Errore Plesk' . ($code !== '' ? ' ' . $code : '') . ': ' . ($text ?: 'operazione non completata'));
        }
        return $xpath;
    }

    private function cli(string $command, array $parameters, int $timeout = 30): array
    {
        if ($this->transport !== null) {
            // I test con transport simulato riguardano l'API XML. La chiamata CLI
            // viene considerata riuscita senza introdurre una seconda transport API.
            return ['code' => 0, 'stdout' => 'Transport di test', 'stderr' => ''];
        }
        $url = rtrim((string) Env::get('PLESK_API_URL', ''), '/');
        $key = trim((string) Env::get('PLESK_API_KEY', ''));
        if ($url === '' || $key === '') {
            throw new RuntimeException('Configura PLESK_API_URL e PLESK_API_KEY nel file .env.');
        }
        try {
            $response = (new Client([
                'base_uri' => $url . '/', 'timeout' => $timeout, 'connect_timeout' => 8,
                'verify' => Env::bool('PLESK_API_VERIFY_TLS', true),
            ]))->post('api/v2/cli/' . rawurlencode($command) . '/call', [
                'headers' => ['X-API-Key' => $key, 'Accept' => 'application/json', 'Content-Type' => 'application/json'],
                'json' => ['params' => array_values($parameters)],
            ]);
        } catch (\Throwable $exception) {
            throw new RuntimeException('Comando Plesk ' . $command . ' non eseguibile: ' . $exception->getMessage(), 0, $exception);
        }
        $result = json_decode((string) $response->getBody(), true);
        if (!is_array($result)) {
            throw new RuntimeException('Plesk ha restituito una risposta CLI non valida.');
        }
        if ((int) ($result['code'] ?? 1) !== 0) {
            $detail = trim((string) ($result['stderr'] ?? $result['stdout'] ?? 'errore sconosciuto'));
            throw new RuntimeException('Errore Plesk ' . $command . ': ' . ($detail ?: 'comando non completato'));
        }
        return $result;
    }

    private function firstInteger(DOMXPath $xpath, string $query): ?int
    {
        $value = trim((string) $xpath->evaluate('string(' . $query . ')'));
        return ctype_digit($value) ? (int) $value : null;
    }

    private static function xml(string $value): string
    {
        return htmlspecialchars($value, ENT_XML1 | ENT_QUOTES, 'UTF-8');
    }
}
