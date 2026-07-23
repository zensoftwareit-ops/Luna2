<?php

declare(strict_types=1);

namespace Luna\Service;

use GuzzleHttp\Client;
use InvalidArgumentException;
use PDO;
use Throwable;

final class EcommerceService
{
    public function __construct(private readonly PDO $db, private readonly int $organizationId, private readonly int $userId = 0)
    {
    }

    public function enqueue(int $channelId, string $operation, ?string $entityType = null, ?int $entityId = null, array $payload = []): int
    {
        $allowed = ['PULL_ORDERS', 'PULL_PRODUCTS', 'PUSH_STOCK', 'PUSH_PRICES', 'ACK_ORDER', 'PROCESS_WEBHOOK'];
        if (!in_array($operation, $allowed, true)) {
            throw new InvalidArgumentException('Operazione e-commerce non valida.');
        }
        $this->channel($channelId);
        $this->db->prepare("INSERT INTO ecommerce_sync_queue (organization_id, ecommerce_channel_id, operation, entity_type, entity_id, payload_json, status, attempts, available_at, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, 'QUEUED', 0, NOW(), NOW(), NOW())")
            ->execute([$this->organizationId, $channelId, $operation, $entityType, $entityId, json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)]);
        return (int) $this->db->lastInsertId();
    }

    public function processQueue(int $limit = 10): array
    {
        $processed = 0; $failed = 0;
        $items = $this->all("SELECT * FROM ecommerce_sync_queue WHERE organization_id = ? AND status IN ('QUEUED','RETRY') AND available_at <= NOW() ORDER BY id LIMIT " . max(1, min(50, $limit)), [$this->organizationId]);
        foreach ($items as $item) {
            $claim = $this->db->prepare("UPDATE ecommerce_sync_queue SET status = 'RUNNING', attempts = attempts + 1, locked_at = NOW(), updated_at = NOW() WHERE id = ? AND status IN ('QUEUED','RETRY')");
            $claim->execute([$item['id']]);
            if ($claim->rowCount() !== 1) { continue; }
            try {
                $channel = $this->channel((int) $item['ecommerce_channel_id']);
                $payload = json_decode((string) ($item['payload_json'] ?? '{}'), true) ?: [];
                $payload['_entity_id'] = $item['entity_id'] ?? null;
                $payload['_entity_type'] = $item['entity_type'] ?? null;
                match ($item['operation']) {
                    'PULL_ORDERS' => $this->pullOrders($channel),
                    'PULL_PRODUCTS' => $this->pullProducts($channel),
                    'PUSH_STOCK' => $this->pushStock($channel, $payload),
                    'PUSH_PRICES' => $this->pushPrice($channel, $payload),
                    'ACK_ORDER' => $this->acknowledgeOrder($channel, $payload),
                    'PROCESS_WEBHOOK' => $this->processWebhookPayload($channel, $payload),
                    default => throw new InvalidArgumentException('Operazione e-commerce non riconosciuta.'),
                };
                $this->db->prepare("UPDATE ecommerce_sync_queue SET status = 'DONE', error_message = NULL, updated_at = NOW() WHERE id = ?")->execute([$item['id']]);
                $processed++;
            } catch (Throwable $exception) {
                $attempt = (int) $item['attempts'] + 1;
                $status = $attempt >= 5 ? 'FAILED' : 'RETRY';
                $delay = min(720, 5 * (2 ** min(7, $attempt)));
                $availableAt = date('Y-m-d H:i:s', time() + $delay * 60);
                $this->db->prepare('UPDATE ecommerce_sync_queue SET status = ?, available_at = ?, error_message = ?, updated_at = NOW() WHERE id = ?')
                    ->execute([$status, $availableAt, mb_substr($exception->getMessage(), 0, 2000), $item['id']]);
                $this->db->prepare("UPDATE ecommerce_channels SET status = 'ERROR', last_error = ?, updated_at = NOW() WHERE id = ?")
                    ->execute([mb_substr($exception->getMessage(), 0, 2000), $item['ecommerce_channel_id']]);
                $failed++;
            }
        }
        return compact('processed', 'failed');
    }

    public function acceptWebhook(int $channelId, string $eventType, string $externalId, string $rawBody, array $headers): int
    {
        $channel = $this->channel($channelIvÛ~º¶‰žËkºwµçB–b‚‚†fÆöB’F&Ææ6U²v÷Væ–æuö†÷W'2uÒ²†fÆöB’F&Ææ6U²v67'VVEö†÷W'2uÒ²†fÆöB’F&Ææ6U²vF§W7FVEö†÷W'2uÒÒ†fÆöB’F&Ææ6U²wW6VEö†÷W'2uÒ’²ãRÂ†fÆöB’FÆVfU²w&WVW7FVEö†÷W'2uÒ’°¢F‡&÷ræWr–çfÆ–D&wVÖVçDW†6WF–öâ‚u6ÆFòfW&–R÷W&ÖW76’–ç7Vff–6–VçFRâr“°¢Ð¢GF†—2ÓæF"Óç&W&R‚uUDDRÆVfUö&Ææ6W24UBW6VEö†÷W'2ÒW6VEö†÷W'2²òt„U$R–BÒòr’ÓæW†V7WFR…²FÆVfU²w&WVW7FVEö†÷W'2uÒÂF&Ææ6U²v–BuÕÒ“°¢Ð¢–b‚F&÷fR—²GW6W#ÒGF†—2ÓæöæR‚u4TÄT5BæÖRe$ôÒW6W'2t„U$R–CÓòäB÷&væ—¦F–öåö–CÓòrÅ²FÆVfU²wW6W%ö–BuÒÂGF†—2Óæ÷&væ—¦F–öä–EÒ“²G&V6÷&EG—SÖÖF6‚‚FÆVfU²vÆVfU÷G—RuÒ—²t„ôÄ”D’sÓât„ôÄ”D’rÂu4”4²sÓâu4”4²rÂtõD„U"sÓâtõD„U"rÆFVfVÇCÓâtÄTdRwÓ²FFFW3ÒGF†—2Óçv÷&´FFW2†æWrFFUF–ÖT–Ö×WF&ÆR‚‡7G&–ær’FÆVfU²w7F'G5öBuÒ’ÆæWrFFUF–ÖT–Ö×WF&ÆR‚‡7G&–ær’FÆVfU²vVæG5öBuÒ’“²G&VÖ–æ–æsÒ†fÆöB’FÆVfU²w&WVW7FVEö†÷W'2uÓ¶f÷&V6‚‚FFFW22F–æFWƒÓâFFFR—²FF—4ÆVgCÖ6÷VçB‚FFFW2’ÒF–æFWƒ²FF”†÷W'3×&÷VæB‚G&VÖ–æ–æròFF—4ÆVgBÃ"“²G&VÖ–æ–æs×&÷VæB‚G&VÖ–æ–ærÒFF”†÷W'2Ã"“²GF†—2ÓæF"Óç&W&R‚t”å4U%B”åDòF–ÖU÷&V6÷&G2†÷&væ—¦F–öåö–BÇW6W%ö–BÇv÷&µöFFRÆV×Æ÷–VUöæÖRÇ&V6÷&E÷G—RÆ†÷W'2Æ&÷fVBÆæ÷FW2Æ7&VFVEö'’ÇWFFVEö'’Æ7&VFVEöBÇWFFVEöB’dÅTU2ƒòÃòÃòÃòÃòÃòÃÃòÃòÃòÄäõr‚’Ääõr‚’’ôâEUÄ”4DR´U’UDDR†÷W'3ÕdÅTU2††÷W'2’Æ&÷fVCÓÆæ÷FW3ÕdÅTU2†æ÷FW2’ÇWFFVEö'“ÕdÅTU2‡WFFVEö'’’ÇWFFVEöCÔäõr‚’r’ÓæW†V7WFR…²GF†—2Óæ÷&væ—¦F–öä–BÂFÆVfU²wW6W%ö–BuÒÂFFFRÂGW6W%²væÖRuÓóòtF—VæFVçFRrÂG&V6÷&EG—RÂFF”†÷W'2Âu&–6†–W7F2râFÆVfT–BÂGF†—2Óæ7F÷$–BÂGF†—2Óæ7F÷$–EÒ“·×Ð¢GF†—2ÓæF"Óç&W&R‚uUDDRÆVfU÷&WVW7G24UB7FGW2ÒòÂFV6–FVEöBÒäõr‚’ÂFV6–FVEö'’ÒòÂFV6—6–öåöæ÷FW2ÒòÂWFFVEöBÒäõr‚’t„U$R–BÒòr’ÓæW†V7WFR…²G7FGW2ÂGF†—2Óæ7F÷$–BÂFæ÷FW2ÂFÆVfT–EÒ“°¢GF†—2ÓæF"Óç&W&R‚uUDDR&÷fÅ÷&WVW7G24UB7FGW2ÒòÂ&÷fW%ö–BÒòÂFV6–FVEöBÒäõr‚’ÂFV6—6–öåöæ÷FW2ÒòÂWFFVEöBÒäõr‚’t„U$R–BÒòr’ÓæW†V7WFR…²G7FGW2ÂGF†—2Óæ7F÷$–BÂFæ÷FW2ÂFÆVfU²v&÷fÅ÷&WVW7Eö–BuÕÒ“°¢GF†—2Óæ†—7F÷'’‚†–çB’FÆVfU²v&÷fÅ÷&WVW7Eö–BuÒÂF&÷fRòt$õdRr¢u$T¤T5BrÂFæ÷FW2“°¢GF†—2ÓæF"Óæ6öÖÖ—B‚“°¢Ò6F6‚…F‡&÷v&ÆRFW†6WF–öâ’°¢–b‚GF†—2ÓæF"Óæ–åG&ç67F–öâ‚’’²GF†—2ÓæF"Óç&öÆÄ&6²‚“²Ð¢F‡&÷rFW†6WF–öã°¢Ð¢Ð ¢V&Æ–2gVæ7F–öâ6æ6VÂ†–çBFÆVfT–BÆ&ööÂF6äÖævSÖfÇ6R“¢fö–@¢°¢GF†—2ÓæF"Óæ&Vv–åG&ç67F–öâ‚“·G'—²FÆVfSÒGF†—2ÓæöæR‚u4TÄT5B¢e$ôÒÆVfU÷&WVW7G2t„U$R–CÓòäB÷&væ—¦F–öåö–CÓòdõ"UDDRrÅ²FÆVfT–BÂGF†—2Óæ÷&væ—¦F–öä–EÒ“¶–b‚FÆVfWÇÂ–åö'&’‚FÆVfU²w7FGW2uÒÅ²u5T$Ô•EDTBrÂt$õdTBuÒÇG'VR’—·F‡&÷ræWr–çfÆ–D&wVÖVçDW†6WF–öâ‚u&–6†–W7FæöâæçVÆÆ&–ÆRâr“·Ö–b‚†–çB’FÆVfU²wW6W%ö–BuÒÓÒGF†—2Óæ7F÷$–BbbF6äÖævR—·F‡&÷ræWr–çfÆ–D&wVÖVçDW†6WF–öâ‚tæöâVö’æçVÆÆ&RÆ&–6†–W7FF’VâÇG&òWFVçFRâr“·Ö–b‚FÆVfU²w7FGW2uÓÓÓÒt$õdTBr—¶–b†–åö'&’‚FÆVfU²vÆVfU÷G—RuÒÅ²t„ôÄ”D’rÂuU$Ô•BrÂu$ôÂrÂtõD„U"uÒÇG'VR’—²G–V#Ò†–çB—7V'7G"‚‡7G&–ær’FÆVfU²w7F'G5öBuÒÃÃB“²GF†—2ÓæF"Óç&W&R‚uUDDRÆVfUö&Ææ6W24UBW6VEö†÷W'3Ôu$TDU5BƒÇW6VEö†÷W'2Óò’t„U$R÷&væ—¦F–öåö–CÓòäBW6W%ö–CÓòäB&Ææ6U÷–V#ÓòäBÆVfU÷G—SÓòr’ÓæW†V7WFR…²FÆVfU²w&WVW7FVEö†÷W'2uÒÂGF†—2Óæ÷&væ—¦F–öä–BÂFÆVfU²wW6W%ö–BuÒÂG–V"ÂFÆVfU²vÆVfU÷G—RuÕÒ“·ÒGF†—2ÓæF"Óç&W&R‚tDTÄUDRe$ôÒF–ÖU÷&V6÷&G2t„U$R÷&væ—¦F–öåö–CÓòäBW6W%ö–CÓòäBæ÷FW3Óòr’ÓæW†V7WFR…²GF†—2Óæ÷&væ—¦F–öä–BÂFÆVfU²wW6W%ö–BuÒÂu&–6†–W7F2râFÆVfT–EÒ“·ÒGF†—2ÓæF"Óç&W&R‚%UDDRÆVfU÷&WVW7G24UB7FGW3Òt4ä4TÄÄTBrÆFV6–FVEöCÔäõr‚’ÆFV6–FVEö'“ÓòÆFV6—6–öåöæ÷FW3ÒtæçVÆÆFrÇWFFVEöCÔäõr‚’t„U$R–CÓò"’ÓæW†V7WFR…²GF†—2Óæ7F÷$–BÂFÆVfT–EÒ“²GF†—2ÓæF"Óç&W&R‚%UDDR&÷fÅ÷&WVW7G24UB7FGW3Òt4ä4TÄÄTBrÆ&÷fW%ö–CÓòÆFV6–FVEöCÔäõr‚’ÆFV6—6–öåöæ÷FW3ÒtæçVÆÆFrÇWFFVEöCÔäõr‚’t„U$R–CÓò"’ÓæW†V7WFR…²GF†—2Óæ7F÷$–BÂFÆVfU²v&÷fÅ÷&WVW7Eö–BuÕÒ“²GF†—2Óæ†—7F÷'’‚†–çB’FÆVfU²v&÷fÅ÷&WVW7Eö–BuÒÂt4ä4TÂrÂu&–6†–W7FæçVÆÆFr“²GF†—2ÓæF"Óæ6öÖÖ—B‚“·Ö6F6‚…F‡&÷v&ÆRFW†6WF–öâ—¶–b‚GF†—2ÓæF"Óæ–åG&ç67F–öâ‚’—²GF†—2ÓæF"Óç&öÆÄ&6²‚“·×F‡&÷rFW†6WF–öã·Ð¢Ð ¢&—fFRgVæ7F–öâv÷&´FFW2„FFUF–ÖT–Ö×WF&ÆRG7F'BÄFFUF–ÖT–Ö×WF&ÆRFVæB“¢'&—²FFFW3ÕµÓ²F7W'6÷#ÒG7F'BÓç6WEF–ÖRƒÃ“²FÆ7CÒFVæBÓç6WEF–ÖRƒÃ“·v†–ÆR‚F7W'6÷#ÃÒFÆ7B—¶–b‚†–çB’F7W'6÷"Óæf÷&ÖB‚târ“ÃÓR—²FFFW5µÓÒF7W'6÷"Óæf÷&ÖB‚u’ÖÒÖBr“·ÒF7W'6÷#ÒF7W'6÷"ÓæÖöF–g’‚r³F’r“·×&WGW&âFFFW3·Ð ¢&—fFRgVæ7F–öâ†—7F÷'’†–çBF&÷fÄ–BÂ7G&–ærF7F–öâÂ÷7G&–ærFæ÷FW2“¢fö–@¢°¢GF†—2ÓæF"Óç&W&R‚t”å4U%B”åDò&÷fÅö†—7F÷'’†÷&væ—¦F–öåö–BÂ&÷fÅ÷&WVW7Eö–BÂ7F–öâÂ7F÷%ö–BÂæ÷FW2Â7&VFVEöB’dÅTU2ƒòÂòÂòÂòÂòÂäõr‚’’r¢ÓæW†V7WFR…²GF†—2Óæ÷&væ—¦F–öä–BÂF&÷fÄ–BÂF7F–öâÂGF†—2Óæ7F÷$–Bó¢çVÆÂÂFæ÷FW5Ò“°¢Ð ¢&—fFRgVæ7F–öâöæR‡7G&–ærG7ÂÂ'&’G&×2“¢'&—ÆfÇ6P¢°¢G7FFVÖVçBÒGF†—2ÓæF"Óç&W&R‚G7Â“²G7FFVÖVçBÓæW†V7WFR‚G&×2“²&WGW&âG7FFVÖVçBÓæfWF6‚‚“°¢Ð§Ð