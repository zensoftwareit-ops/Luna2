<?php

declare(strict_types=1);

namespace Luna\Core;

use Throwable;

final class ErrorReporter
{
    public static function report(Throwable $exception, string $basePath): string
    {
        try {
            $reference = strtoupper(date('ymd-His') . '-' . bin2hex(random_bytes(3)));
        } catch (Throwable) {
            $reference = strtoupper(date('ymd-His') . '-' . substr(sha1(uniqid('', true)), 0, 6));
        }

        $context = [
            'reference' => $reference,
            'time' => date(DATE_ATOM),
            'method' => $_SERVER['REQUEST_METHOD'] ?? null,
            'path' => parse_url($_SERVER['REQUEST_URI'] ?? '/', PHP_URL_PATH),
            'user_id' => Auth::id() ?: null,
            'organization_id' => Auth::organizationId() ?: null,
            'exception' => $exception::class,
            'message' => $exception->getMessage(),
            'file' => $exception->getFile(),
            'line' => $exception->getLine(),
            'trace' => $exception->getTraceAsString(),
        ];
        $line = json_encode($context, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) . PHP_EOL;
        $logDirectory = $basePath . '/storage/logs';
        if (is_dir($logDirectory) && is_writable($logDirectory)) {
            @file_put_contents($logDirectory . '/application-' . date('Y-m-d') . '.log', $line, FILE_APPEND | LOCK_EX);
        }
        error_log('[Luna2 ' . $reference . '] ' . $exception->__toString());
        return $reference;
    }

    public static function isSchemaError(Throwable $exception): bool
    {
        $message = strtolower($exception->getMessage());
        return str_contains($message, 'base table or view not found')
            || str_contains($message, "doesn't exist")
            || str_contains($message, 'unknown column');
    }
}
