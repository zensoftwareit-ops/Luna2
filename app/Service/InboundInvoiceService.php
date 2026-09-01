<?php
declare(strict_types=1);
namespace Luna\Service;
use GuzzleHttp\Client;
use InvalidArgumentException;
use PDO;
final class InboundInvoiceService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId) {}
    public function receive(): array
    {
        $s=$this->db->prepare("SELECT * FROM api_endpoint_configs WHERE organization_id=? AND service_key='EINVOICE' AND enabled=1 ORDER BY environment='PRODUCTION' DESC,id DESC LIMIT 1");$s->execute([$this->organizationId]);$e=$s->fetch();
        if(!$e||empty($e['receive_path'])){throw new InvalidArgumentException('Configura e abilita un endpoint EINVOICE con percorso di ricezione.');}
        $headers=json_decode((string)($e['header_json']??'{}'),true)?:[];$secret=SecretResolver::resolve($e['secret_reference']??null);
        if($e['auth_type']==='BEARER'){$headers['Authorization']='Bearer '.$secret;} if($e['auth_type']==='API_KEY'){$headers['X-API-Key']=$secret;}
        $response=(new Client(['timeout'=>(int)$e['timeout_seconds'],'verify'=>(bool)$e['verify_tls']]))->get(rtrim((string)$e['base_url'],'/').'/'.ltrim((string)$e['receive_path'],'/'),['headers'=>['Accept'=>'application/json']+$headers,'query'=>['direction'=>'inbound','format'=>'fatturapa']]);
        $payload=json_decode((string)$response->getBody(),true);$items=is_array($payload['invoices']??null)?$payload['invoices']:(is_array($payload)?$payload:[]);$files=[];
        foreach($items as $i=>$item){if(!is_array($item))continue;$content=base64_decode((string)($item['content_base64']??''),true);if($content===false||$content==='')continue;$files[]=['name'=>basename((string)($item['filename']??('fattura-'.($i+1).'.xml'))),'content'=>$content];}
        if($files===[]){throw new InvalidArgumentException('L’endpoint non ha restituito nuove fatture XML.');}return $files;
    }
}
