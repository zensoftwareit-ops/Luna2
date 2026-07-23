<?php

declare(strict_types=1);

namespace Luna\Service;

use InvalidArgumentException;
use PDO;
use PHPMailer\PHPMailer\PHPMailer;
use Throwable;

final class MailService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId = 0)
    {
    }

    public function saveSettings(array $data): void
    {
        $transport = strtoupper((string) ($data['transport'] ?? 'DISABLED'));
        $encryption = strtoupper((string) ($data['encryption'] ?? 'TLS'));
        if (!in_array($transport, ['SMTP', 'SENDMAIL', 'DISABLED'], true) || !in_array($encryption, ['NONE', 'TLS', 'SMTPS'], true)) {
            throw new InvalidArgumentException('Configurazione e-mail non valida.');
        }
        $this->db->prepare('INSERT INTO communication_settings (organization_id, transport, host, port, encryption, username, secret_reference, from_email, from_name, reply_to, tracking_base_url, tracking_enabled, max_attempts, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) ON DUPLICATE KEY UPDATE transport = VALUES(transport), host = VALUES(host), port = VALUES(port), encryption = VALUES(encryption), username = VALUES(username), secret_reference = VALUES(secret_reference), from_email = VALUES(from_email), from_name = VALUES(from_name), reply_to = VALUES(reply_to), tracking_base_url = VALUES(tracking_base_url), tracking_enabled = VALUES(tracking_enabled), max_attempts = VALUES(max_attempts), updated_at = NOW()')
            ->execute([$this->organizationId, $transport, trim((string) ($data['host'] ?? '')) ?: null, max(1, min(65535, (int) ($data['port'] ?? 587))), $encryption, trim((string) ($data['username'] ?? '')) ?: null, trim((string) ($data['secret_reference'] ?? '')) ?: null, $this->email($data['from_email'] ?? '', true), trim((string) ($data['from_name'] ?? '')) ?: null, $this->email($data['reply_to'] ?? '', true), rtrim(trim((string) ($data['tracking_base_url'] ?? '')), '/') ?: null, !empty($data['tracking_enabled']) ? 1 : 0, max(1, min(10, (int) ($data['max_attempts'] ?? 5)))]);
    }

    public function queue(string $recipient, string $subject, string $html, ?int $documentId = null, array $attachments = []): int
    {
        $recipient = $this->email($recipient);
        if (trim($subject) === '' || trim($html) === '') {
            throw new InvalidArgumentException('Oggetto e contenuto e-mail sono obbligatori.');
        }
        if ($documentId !== null && !$this->one('SELECT id FROM documents WHERE id = ? AND organization_id = ?', [$documentId, $this->organizationId])) {
            throw new InvalidArgumentException('Documento da inviare non valido.');
        }
        $uuid = $this->uuid();
        $token = bin2hex(random_bytes(32));
        $this->db->prepare("INSERT INTO outbound_emails (organization_id, message_uuid, document_id, recipient, subject, html_body, text_body, attachment_json, tracking_token, status, attempts, next_attempt_at, created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'QUEUED', 0, NOW(), ?, NOW(), NOW())")
            ->execute([$this->organizationId, $uuid, $documentId, $recipient, trim($subject), $html, trim(strip_tags($html)), json_encode($attachments, JSON_UNESCAPED_SLASHES), $token, $this->userId ?: null]);
        return (int) $this->db->lastInsertId();
    }

    public function processQueue(int $limit = 20): array
    {
        $settings = $this->one('SELECT * FROM communication_settings WHERE organization_id = ?', [$this->organizationId]);
        if (!$settings || $settings['transport'] === 'DISABLED') {
            return ['sent' => 0, 'failed' => 0, 'message' => 'Invio e-mail disattivato.'];
        }
        $sent = 0;
        $failed = 0;
        $messages = $this->all("SELECT * FROM outbound_emails WHERE organization_id = ? AND status IN ('QUEUED','RETRY') AND (next_attempt_at IS NULL OR next_attempt_at <= NOW()) ORDER BY id LIMIT " . max(1, min(100, $limit)), [$this->organizationId]);
        foreach ($messages as $message) {
            $claim = $this->db->prepare("UPDATE outbound_emails SET status = 'SENDING', attempts = attempts + 1, updated_at = NOW() WHERE id = ? AND organization_id = ? AND status IN ('QUEUED','RETRY')");
            $claim->execute([$message['id'], $this->organizationId]);
            if ($claim->rowCount() !== 1) {
                continue;
            }
            try {
                $mailer = $this->mailer($settings);
                $mailer->addAddress($message['recipient']);
                $mailer->Subject = $message['subject'];
                $html = $this->trackingHtml($message, $settings);
                $mailer->isHTML(true);
                $mailer->Body = $html;
                $mailer->AltBody = $message['text_body'] ?: strip_tags($html);
                foreach (json_decode((string) ($message['attachment_json'] ?? '[]'), true) ?: [] as $attachment) {
                    $path = realpath((string) ($attachment['path'] ?? ''));
                    $base = realpath(dirname(__DIR__, 2) . '/storage');
                    if ($path && $base && str_starts_with($path, $base . DIRECTORY_SEPARATOR) && is_file($path)) {
                        $mailer->addAttachment($path, (string) ($attachment['name'] ?? basename($path)));
                    }
                }
                $mailer->send();
                $this->db->prepare("UPDATE outbound_emails SET status = 'SENT', sent_at = NOW(), message_id = ?, error_message = NULL, updated_at = NOW() WHERE id = ?")
                    ->execute([$mailer->getLastMessageID() ?: null, $message['id']]);
                $sent++;
            } catch (Throwable $exception) {
                $attempts = (int) $message['attempts'] + 1;
                $status = $attempts >= (int) $settings['max_attempts'] ? 'FAILED' : 'RETRY';
                $delay = min(1440, 5 * (2 ** min(8, $attempts)));
                $nextAttempt = date('Y-m-d H:i:s', time() + $delay * 60);
                $this->db->prepare('UPDATE outbound_emails SET status = ?, next_attempt_at = ?, error_message = ?, updated_at = NOW() WHERE id = ?')
                    ->execute([$status, $nextAttempt, mb_substr($exception->getMessage(), 0, 2000), $message['id']]);
                $failed++;
            }
        }
        return compact('sent', 'failed');
    }

    public function registerEvent(string $token, string $type, ?string $targetUrl, string $ip, string $userAgent): bool
    {
        $message = $this->one('SELECT id, organization_id FROM outbound_emails WHERE tracking_token = ?', [$token]);
        if (!$message || !in_array($type, ['OPEN', 'CLICK', 'DOWNLOAD'], true)) {
            return false;
        }
        $this->db->prepare('INSERT INTO email_events (organization_id, outbound_email_id, event_type, target_url, ip_hash, user_agent_hash, occurred_at) VALUES (?, ?, ?, ?, ?, ?, NOW())')
            ->execute([$message['organization_id'], $message['id'], $type, $targetUrl, hash('sha256', $ip), hash('sha256', $userAgent)]);
        return true;
    }

    private function trackingHtml(array $message, array $settings): string
    {
        $html = (string) $message['html_body'];
        $base = rtrim((string) ($settings['tracking_base_url'] ?? ''), '/');
        if (!(bool) $settings['tracking_enabled'] || $base === '' || !str_starts_with($base, 'https://')) {
            return $html;
        }
        $token = rawurlencode((string) $message['tracking_token']);
        $html = preg_replace_callback('/href=([' . "'\"" . '])(https:\/\/[^' . "'\"" . ']+)\1/i', static fn (array $match): string => 'href=' . $match[1] . $base . '/mail/c/' . $token . '?url=' . rawurlencode(html_entity_decode($match[2], ENT_QUOTES)) . $match[1], $html) ?? $html;
        return $html . '<img src="' . htmlspecialchars($base . '/mail/o/' . $token . '.gif', ENT_QUOTES) . '" alt="" width="1" height="1" style="display:none">';
    }

    private function mailer(array $settings): PHPMailer
    {
        $mailer = new PHPMailer(true);
        $mailer->CharSet = 'UTF-8';
        if ($settings['transport'] === 'SMTP') {
            $mailer->isSMTP();
            $mailer->Host = (string) $settings['host'];
            $mailer->Port = (int) $settings['port'];
            $mailer->SMTPAuth = !empty($settings['username']);
            $mailer->Username = (string) ($settings['username'] ?? '');
            $mailer->Password = SecretResolver::resolve($settings['secret_reference'] ?? null);
            $mailer->SMTPSecure = match ($settings['encryption']) { 'SMTPS' => PHPMailer::ENCRYPTION_SMTPS, 'TLS' => PHPMailer::ENCRYPTION_STARTTLS, default => '' };
        } else {
            $mailer->isSendmail();
        }
        $from = $this->email($settings['from_email'] ?? '');
        $mailer->setFrom($from, (string) ($settings['from_name'] ?? 'Luna2'));
        if (!empty($settings['reply_to'])) {
            $mailer->addReplyTo((string) $settings['reply_to']);
        }
        return $mailer;
    }

    private function email(mixed $value, bool $nullable = false): ?string
    {
        $email = mb_strtolower(trim((string) $value));
        if ($nullable && $email === '') {
            return null;
        }
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            throw new InvalidArgumentException('Indirizzo e-mail non valido.');
        }
        return $email;
    }

    private function uuid(): string
    {
        $bytes = random_bytes(16);
        $bytes[6] = chr((ord($bytes[6]) & 0x0f) | 0x40);
        $bytes[8] = chr((ord($bytes[8]) & 0x3f) | 0x80);
        return vsprintf('%s%s-%s-%s-%s-%s%s%s', str_split(bin2hex($bytes), 4));
    }

    private function one(string $sql, array $params): array|false
    {
        $statement = $this->db->prepare($sql); $statement->execute($params); return $statement->fetch();
    }

    private function all(string $sql, array $params): array
    {
        $statement = $this->db->prepare($sql); $statement->execute($params); return $statement->fetchAll();
    }
}
