<?php
declare(strict_types=1);
namespace Luna\Service;
use GuzzleHttp\Client;
use InvalidArgumentException;
use PDO;
use RuntimeException;
final class InboundInvoiceService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId) {}
    public function receive(): array
    {
        $e=$this->endpoint();
        $payload=$this->request($e,'GET',(string)$e['receive_path'],['query'=>['direction'=>'inbound','pending_only'=>'true','limit'=>200]]);
        $items=is_array($payload['invoices']??null)?$payload['invoices']:[];$files=[];
        foreach($items as $i=>$item){
            if(!is_array($item))continue;
            $externalId=(string)($item['external_id']??$item['id']??'');
            if($externalId==='')continue;
            $contentPayload=$this->request($e,'GET',rtrim((string)$e['receive_path'],'/').'/inbound/'.rawurlencode($externalId).'/content');
            $content=base64_decode((string)($contentPayload['content_base64']??''),true);
            if($content===false||$content==='')continue;
            $files[]=['external_id'=>$externalId,'name'=>basename((string)($item['filename']??('fattura-'.($i+1).'.xml'))),'content'=>$content];
        }
        if($files===[]){throw new InvalidArgumentException('L’endpoint non ha restituito nuove fatture XML.');}return $files;
    }

    public function acknowledge(array $files): void
    {
        $e=$this->endpoint();
        foreach($files as $file){
            if(empty($file['external_id']))continue;
            $this->request($e,'POST',rtrim((string)$e['receive_path'],'/').'/inbound/'.rawurlencode((string)$file['external_id']).'/ack');
        }
    }

    private function endpoint(): array
    {
        $s=$this->db->prepare("SELECT * FROM api_endpoint_configs WHERE organization_id=? AND service_key IN ('EINVOICE_SDI','EINVOICE') AND enabled=1 ORDER BY environment='PRODUCTION' DESC,id DESC LIMIT 1");$s->execute([$this->organizationId]);$e=$s->fetch();
        if(!$e||empty($e['receive_path'])){throw new InvalidArgumentException('Configura e abilita il canale EINVOICE_SDI con percorso di ricezione.');}
        return $e;
    }

    private function request(array $e,string $method,string $path,array $options=[]): array
    {
        $headers=json_decode((string)($e['header_json']??'{}'),true)?:[];$secret=SecretResolver::resolve($e['secret_reference']??null);
        if($e['auth_type']==='BEARER'){$headers['Authorization']='Bearer '.$secret;} if($e['auth_type']==='API_KEY'){$headers['X-API-Key']=$secret;}
        $options['headers']=['Accept'=>'application/json','X-Request-Id'=>bin2hex(random_bytes(12))]+$headers+($options['headers']??[]);$options['http_errors']=false;
        $response=(new Client(['timeout'=>(int)$e['timeout_seconds'],'verify'=>(bool)$e['verify_tls']]))->request($method,rtrim((string)$e['base_url'],'/').'/'.ltrim($path,'/'),$options);
        $payload=json_decode((string)$response->getBody(),true);
        if(!is_array($payload)||$response->getStatusCode()<200||$response->getStatusCode()>=300){throw new RuntimeException((string)($payload['message']??('Servizio SdI HTTP '.$response->getStatusCode())));}
        return $payload;
    }
}
