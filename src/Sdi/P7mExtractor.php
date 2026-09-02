<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

final class P7mExtractor
{
    public function extract(string $content): ?string
    {
        if (str_starts_with(ltrim($content), '<')) {
            return $content;
        }
        if (!function_exists('openssl_pkcs7_verify')) {
            return null;
        }

        $directory = sys_get_temp_dir() . '/luna2-p7m-' . bin2hex(random_bytes(8));
        if (!mkdir($directory, 0700, true)) {
            return null;
        }
        $input = $directory . '/input.p7m';
        $output = $directory . '/content.xml';
        $smime = "MIME-Version: 1.0\r\n"
            . "Content-Type: application/pkcs7-mime; smime-type=signed-data; name=invoice.p7m\r\n"
            . "Content-Transfer-Encoding: base64\r\n\r\n"
            . chunk_split(base64_encode($content), 64, "\r\n");
        file_put_contents($input, $smime, LOCK_EX);

        try {
            $flags = (defined('PKCS7_NOVERIFY') ? PKCS7_NOVERIFY : 32) | (defined('PKCS7_NOSIGS') ? PKCS7_NOSIGS : 4);
            $verified = @openssl_pkcs7_verify($input, $flags, null, [], null, $output);
            if (!$verified || !is_file($output)) {
                return null;
            }
            $xml = (string) file_get_contents($output);
            return str_starts_with(ltrim($xml), '<') ? $xml : null;
        } finally {
            foreach (glob($directory . '/*') ?: [] as $file) {
                @unlink($file);
            }
            @rmdir($directory);
        }
    }
}

