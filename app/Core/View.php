<?php

declare(strict_types=1);

namespace Luna\Core;

use RuntimeException;

final class View
{
    public function __construct(private readonly string $basePath, private readonly array $config)
    {
    }

    public function render(string $template, array $data = [], int $status = 200): never
    {
        $file = $this->basePath . '/views/' . $template . '.php';
        if (!is_file($file)) {
            throw new RuntimeException("Vista non trovata: {$template}");
        }

        http_response_code($status);
        extract($data, EXTR_SKIP);
        $view = $this;
        $config = $this->config;

        ob_start();
        require $file;
        $content = (string) ob_get_clean();

        if (($data['layout'] ?? true) === false) {
            echo $content;
            exit;
        }

        require $this->basePath . '/views/layout.php';
        exit;
    }

    public static function e(mixed $value): string
    {
        return htmlspecialchars((string) $value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
    }

    public static function money(mixed $value): string
    {
        return number_format((float) $value, 2, ',', '.') . ' €';
    }

    public static function date(mixed $value): string
    {
        if (!$value) {
            return '—';
        }
        $time = strtotime((string) $value);
        return $time ? date('d/m/Y', $time) : self::e($value);
    }
}
