<?php

declare(strict_types=1);

namespace LunaApi\Sdi;

final class SoapResponse
{
    public static function invoiceAccepted(): never
    {
        self::send('<typ:rispostaRiceviFatture xmlns:typ="http://www.fatturapa.gov.it/sdi/ws/ricezione/v1.0/types"><Esito>ER01</Esito></typ:rispostaRiceviFatture>');
    }

    public static function oneWayAccepted(): never
    {
        self::send('');
    }

    public static function fault(string $message, bool $clientFault = false): never
    {
        $code = $clientFault ? 'soap:Client' : 'soap:Server';
        $escaped = htmlspecialchars($message, ENT_XML1 | ENT_QUOTES, 'UTF-8');
        self::send('<soap:Fault><faultcode>' . $code . '</faultcode><faultstring>' . $escaped . '</faultstring></soap:Fault>', 500);
    }

    private static function send(string $body, int $status = 200): never
    {
        http_response_code($status);
        header('Content-Type: text/xml; charset=utf-8');
        header('Cache-Control: no-store');
        header('X-Content-Type-Options: nosniff');
        echo '<?xml version="1.0" encoding="UTF-8"?>'
            . '<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Header/><soap:Body>'
            . $body
            . '</soap:Body></soap:Envelope>';
        exit;
    }
}

