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
        $channel = $this->channel($channelId);
        $credentials = $this->credentials($channel, true);
        $valid = $this->verifySignature($channel, $rawBody, $headers, $credentials);
        $this->db->prepare("INSERT INTO webhook_events (organization_id, channel_id, provider, event_type, external_id, signature_valid, payload_json, status, received_at) VALUES (?, ?, ?, ?, ?, ?, ?, 'RECEIVED', NOW()) ON DUPLICATE KEY UPDATE id = LAST_INSERT_ID(id)")
            ->execute([$this->organizationId, $channelId, $channel['platform'], $eventType, $externalId ?: hash('sha256', $rawBody), $valid ? 1 : 0, $rawBody]);
        $id = (int) $this->db->lastInsertId();
        if (!$valid) {
            $this->db->prepare("UPDATE webhook_events SET status = 'ERROR', error_message = 'Firma webhook non valida' WHERE id = ?")->execute([$id]);
            throw new InvalidArgumentException('Firma webhook non valida.');
        }
        $this->enqueue($channelId, 'PROCESS_WEBHOOK', 'webhook_events', $id, json_decode($rawBody, true) ?: []);
        return $id;
    }

    public function convertOrder(int $ecommerceOrderId): int
    {
        $this->db->beginTransaction();
        try {
            $order = $this->one('SELECT * FROM ecommerce_orders WHERE id = ? AND organization_id = ? FOR UPDATE', [$ecommerceOrderId, $this->organizationId]);
            if (!$order) { throw new InvalidArgumentException('Ordine e-commerce non trovato.'); }
            if ($order['luna_document_id']) { $this->db->commit(); return (int) $order['luna_document_id']; }
            $lines = $this->all('SELECT * FROM ecommerce_order_lines WHERE ecommerce_order_id = ? AND organization_id = ? ORDER BY id', [$ecommerceOrderId, $this->organizationId]);
            if ($lines === []) { throw new InvalidArgumentException('Ordine senza righe.'); }
            $customer = $this->resolveCustomer((string) $order['customer_email']);
            $taxable = round(array_sum(array_map(static fn (array $line): float => (float) $line['total_amount'] - (float) $line['tax_amount'], $lines)), 2);
            $vat = round(array_sum(array_column($lines, 'tax_amount')), 2);
            $number = (new DocumentNumberService($this->db, $this->organizationId))->next('SALES_ORDER', (string) $order['order_date']);
            $this->db->prepare("INSERT INTO documents (organization_id, document_type, number, fiscal_year, document_date, counterparty_type, counterparty_id, counterparty_name, subject, currency, taxable_total, vat_total, total, balance_due, status, fulfillment_status, notes, external_key, created_by, updated_by, created_at, updated_at) VALUES (?, 'SALES_ORDER', ?, ?, ?, 'CUSTOMER', ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMED', 'OPEN', ?, ?, ?, ?, NOW(), NOW())")
                ->execute([$this->organizationId, $number, (int) substr((string) $order['order_date'], 0, 4), $order['order_date'], $customer['id'], $customer['business_name'], 'Ordine ' . $order['platform'] . ' #' . $order['external_order_id'], $order['currency'], $taxable, $vat, $order['total'], $order['total'], 'Import automatico canale e-commerce', hash('sha256', $order['platform'] . '|' . $order['external_order_id']), $this->userId ?: null, $this->userId ?: null]);
            $documentId = (int) $this->db->lastInsertId();
            $insert = $this->db->prepare('INSERT INTO document_lines (organization_id, document_id, line_number, product_id, product_code, description, quantity, unit, unit_price, taxable_amount, vat_rate, vat_amount, total_amount, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())');
            foreach ($lines as $index => $line) {
                $taxableLine = round((float) $line['total_amount'] - (float) $line['tax_amount'], 2);
                $rate = $taxableLine > 0 ? round((float) $line['tax_amount'] / $taxableLine * 100, 2) : 0;
                $insert->execute([$this->organizationId, $documentId, $index + 1, $line['product_id'], $line['sku'], $line['description'], $line['quantity'], 'NR', $line['unit_price'], $taxableLine, $rate, $line['tax_amount'], $line['total_amount']]);
            }
            $this->db->prepare("UPDATE ecommerce_orders SET import_status = 'IMPORTED', luna_document_id = ?, updated_at = NOW() WHERE id = ?")->execute([$documentId, $ecommerceOrderId]);
            $this->db->commit();
            return $documentId;
        } catch (Throwable $exception) {
            if ($this->db->inTransaction()) { $this->db->rollBack(); }
            throw $exception;
        }
    }

    private function pullOrders(array $channel): void
    {
        [$request, $extract] = $this->orderRequest($channel);
        $response = (new Client(['timeout' => 30, 'connect_timeout' => 10, 'http_errors' => true]))->request($request['method'], $request['url'], $request['options']);
        $payload = json_decode((string) $response->getBody(), true, 512, JSON_THROW_ON_ERROR);
        if (!empty($payload['errors'])) { throw new InvalidArgumentException('Errore adapter: ' . json_encode($payload['errors'], JSON_UNESCAPED_UNICODE)); }
        $orders = $extract($payload);
        foreach ($orders as $external) { $this->upsertExternalOrder($channel, $external); }
        $this->db->prepare("UPDATE ecommerce_channels SET status = 'CONNECTED', last_sync_at = NOW(), last_success_at = NOW(), last_error = NULL WHERE id = ?")->execute([$channel['id']]);
    }

    private function pullProducts(array $channel): void
    {
        [$request, $extract] = $this->productRequest($channel);
        $response = (new Client(['timeout' => 30, 'connect_timeout' => 10, 'http_errors' => true]))->request($request['method'], $request['url'], $request['options']);
        $payload = json_decode((string) $response->getBody(), true, 512, JSON_THROW_ON_ERROR);
        if (!empty($payload['errors'])) { throw new InvalidArgumentException('Errore adapter: ' . json_encode($payload['errors'], JSON_UNESCAPED_UNICODE)); }
        foreach ($extract($payload) as $external) {
            $product = $this->normalizeProduct((string) $channel['platform'], $external);
            if ($product['external_id'] === '') {
                continue;
            }
            $local = $product['sku'] !== '' ? $this->one('SELECT id FROM products WHERE organization_id = ? AND (sku = ? OR code = ? OR ean = ?) LIMIT 1', [$this->organizationId, $product['sku'], $product['sku'], $product['sku']]) : false;
            $this->db->prepare('INSERT INTO ecommerce_products (organization_id, ecommerce_channel_id, platform, external_product_id, product_id, sku, name, price, quantity, sync_status, raw_data_json, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW()) ON DUPLICATE KEY UPDATE ecommerce_channel_id=VALUES(ecommerce_channel_id), product_id=COALESCE(product_id,VALUES(product_id)), sku=VALUES(sku), name=VALUES(name), price=VALUES(price), quantity=VALUES(quantity), sync_status=VALUES(sync_status), raw_data_json=VALUES(raw_data_json), updated_at=NOW()')
                ->execute([$this->organizationId, $channel['id'], $channel['platform'], $product['external_id'], $local['id'] ?? null, $product['sku'] ?: null, $product['name'], $product['price'], $product['quantity'], 'SYNCED', json_encode($product['raw'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)]);
        }
        $this->db->prepare("UPDATE ecommerce_channels SET status = 'CONNECTED', last_sync_at = NOW(), last_success_at = NOW(), last_error = NULL WHERE id = ?")->execute([$channel['id']]);
    }

    private function productRequest(array $channel): array
    {
        $credentials = $this->credentials($channel);
        $settings = json_decode((string) ($channel['settings_json'] ?? '{}'), true) ?: [];
        $base = rtrim((string) $channel['base_url'], '/');
        if (!str_starts_with($base, 'https://')) {
            throw new InvalidArgumentException('Lâ€™endpoint e-commerce deve usare HTTPS.');
        }
        return match ($channel['platform']) {
            'WOOCOMMERCE' => [[
                'method' => 'GET', 'url' => $base . '/wp-json/wc/v3/products',
                'options' => ['auth' => [$credentials['consumer_key'] ?? '', $credentials['consumer_secret'] ?? ''], 'query' => ['per_page' => 100]],
            ], static fn (array $payload): array => $payload],
            'SHOPIFY' => [[
                'method' => 'POST', 'url' => $base . '/admin/api/' . ($settings['api_version'] ?? '2026-07') . '/graphql.json',
                'options' => ['headers' => ['X-Shopify-Access-Token' => $credentials['access_token'] ?? ''], 'json' => ['query' => '{ products(first: 100) { nodes { id title variants(first: 100) { nodes { id title sku price inventoryQuantity inventoryItem { id } } } } } }']],
            ], static function (array $payload): array {
             #^´ï›h‘éì¶»§q«^t[˜ÙVÉÜ™\Ù\™YÜ]X[]I×NÂˆ	\ËO™‹Oœ™\\™J	ÕTUH[™[ÜžWØ˜[[˜Ù\ÈÑU]X[]HHË™\Ù\™YÜ]X[]HHË]™\˜YÙWØÛÜÝHË\]YØ]H“ÕÊ
HÒT‘HYHÉÊBˆO™^XÝ]JÉ™]Ô]X[]K	™\Ù\™Y	]™\˜YÙPÛÜÝ	˜[[˜ÙVÉÚY	×WJNÂˆ	\ËO™‹Oœ™\\™J	ÒS”ÑT•S•È[™[ÜžWÛ[Ý™[Y[È
[Ý™[Y[Ý]ZYÜ™Ø[š^˜][Û—ÚY[Ý™[Y[Ù]K›ÙXÝÚY›ÙXÝØÛÙKØ\™ZÝ\ÙWÚYØ\™ZÝ\ÙWØÛÙK[Ý™[Y[Ý\K]X[]K[š]ØÛÜÝ™X\ÛÛ‹ØÝ[Y[Ý\KØÝ[Y[Û[X™\‹ÛÝ\˜ÙWÝ\KÛÝ\˜ÙWÚY™]™\œÙYÛ[Ý™[Y[ÚYÜ™X]YØžK\]YØžKÜ™X]YØ]\]YØ]
HSQTÈ
ËËËËËËËËËËËËËËËËËË“ÕÊ
K“ÕÊ
JIÊBˆO™^XÝ]JÉ]ZY	\ËO›Ü™Ø[š^˜][Û’Y	]VÉÛ[Ý™[Y[Ù]I×HÏÈ]J	ÖK[KY	ÊK	›ÙXÝY	›ÙXÝÉØÛÙI×K	Ø\™ZÝ\ÙRY	Ø\™ZÝ\ÙVÉØÛÙI×K	\K	]X[]K	[š]ÛÜÝ
Ýš[™ÊH
	]VÉÜ™X\ÛÛ‰×HÏÈ	Ó[Ýš[Y[ÈX[X[IÊK	]VÉÙØÝ[Y[Ý\I×HÏÈ[	]VÉÙØÝ[Y[Û[X™\‰×HÏÈ[	]VÉÜÛÝ\˜ÙWÝ\I×HÏÈ[	]VÉÜÛÝ\˜ÙWÚY	×HÏÈ[	]VÉÜ™]™\œÙYÛ[Ý™[Y[ÚY	×HÏÈ[	\ËO\Ù\’Y	\ËO\Ù\’YJNÂˆ™]\›ˆ
[
H	\ËO™‹O›\Ý[œÙ\Y

NÂˆB‚ˆš]˜]H[˜Ý[Ûˆ˜[[˜ÙJ[	Ø\™ZÝ\ÙRY[	›ÙXÝY›ÛÛ	ØÚÊNˆ\œ˜^BˆÂˆ	ÝY™š^H	ØÚÈÈ	È“ÔˆTUIÈˆ	ÉÎÂˆ	˜[[˜ÙHH	\ËO›Û™J	ÔÑSPÕ
ˆ”“ÓH[™[ÜžWØ˜[[˜Ù\ÈÒT‘HÜ™Ø[š^˜][Û—ÚYHÈS‘Ø\™ZÝ\ÙWÚYHÈS‘›ÙXÝÚYHÉÈˆ	ÝY™š^É\ËO›Ü™Ø[š^˜][Û’Y	Ø\™ZÝ\ÙRY	›ÙXÝYJNÂˆYˆ
I˜[[˜ÙJHÂˆ	\ËO™‹Oœ™\\™J	ÒS”ÑT•S•È[™[ÜžWØ˜[[˜Ù\È
Ü™Ø[š^˜][Û—ÚYØ\™ZÝ\ÙWÚY›ÙXÝÚY]X[]K™\Ù\™YÜ]X[]KZ[š[][WÜÝØÚË]™\˜YÙWØÛÜÝ\]YØ]
HSQTÈ
ËËË“ÕÊ
JIÊBˆO™^XÝ]JÉ\ËO›Ü™Ø[š^˜][Û’Y	Ø\™ZÝ\ÙRY	›ÙXÝYJNÂˆ	˜[[˜ÙHH	\ËO›Û™J	ÔÑSPÕ
ˆ”“ÓH[™[ÜžWØ˜[[˜Ù\ÈÒT‘HÜ™Ø[š^˜][Û—ÚYHÈS‘Ø\™ZÝ\ÙWÚYHÈS‘›ÙXÝÚYHÉÈˆ	ÝY™š^É\ËO›Ü™Ø[š^˜][Û’Y	Ø\™ZÝ\ÙRY	›ÙXÝYJNÂˆBˆ™]\›ˆ	˜[[˜ÙNÂˆB‚ˆš]˜]H[˜Ý[ÛˆØ\™ZÝ\ÙJ[	Y
Nˆ\œ˜^BˆÂˆ	Ø\™ZÝ\ÙHH	\ËO›Û™J	ÔÑSPÕYÛÙH”“ÓHØ\™ZÝ\Ù\ÈÒT‘HYHÈS‘Ü™Ø[š^˜][Û—ÚYHÈS‘XÝ]™HHIËÉY	\ËO›Ü™Ø[š^˜][Û’YJNÂˆYˆ
IØ\™ZÝ\ÙJHÂˆ›ÝÈ™]È[˜[Y\™Ý[Y[^Ù\[ÛŠ	ÓXYØ^žš[›È›Ûˆ˜[YË‰ÊNÂˆBˆ™]\›ˆ	Ø\™ZÝ\ÙNÂˆB‚ˆš]˜]H[˜Ý[Ûˆ˜[œÙ™\Š[	Y›ÛÛ	ØÚÊNˆ\œ˜^BˆÂˆ	˜[œÙ™\ˆH	\ËO›Û™J	ÔÑSPÕ
ˆ”“ÓH[™[ÜžWÝ˜[œÙ™\œÈÒT‘HYHÈS‘Ü™Ø[š^˜][Û—ÚYHÉÈˆ
	ØÚÈÈ	È“ÔˆTUIÈˆ	ÉÊKÉY	\ËO›Ü™Ø[š^˜][Û’YJNÂˆYˆ
I˜[œÙ™\ŠHÂˆ›ÝÈ™]È[˜[Y\™Ý[Y[^Ù\[ÛŠ	Õ˜\Ù™\š[Y[È›Ûˆ›Ý˜]Ë‰ÊNÂˆBˆ™]\›ˆ	˜[œÙ™\ŽÂˆB‚ˆš]˜]H[˜Ý[Ûˆ™^Ü\˜][Û˜[[X™\ŠÝš[™È	™Yš^Ýš[™È	]JNˆÝš[™ÂˆÂˆ	YX\ˆHÝXœÝŠ	]K
NÂˆ	Ù^HH	ÓÔËIÈˆ	™Yš^ˆ	ËIÈˆ	YX\ŽÂˆ	Ù\]Y[˜ÙHH	\ËO›Û™J	ÔÑSPÕY™^Ý˜[YKY[™È”“ÓHØÝ[Y[ÜÙ\]Y[˜Ù\ÈÒT‘HÜ™Ø[š^˜][Û—ÚYHÈS‘Ù\]Y[˜ÙWÚÙ^HHÈ“ÔˆTUIËÉ\ËO›Ü™Ø[š^˜][Û’Y	Ù^WJNÂˆYˆ
IÙ\]Y[˜ÙJHÂˆ	\ËO™‹Oœ™\\™J	ÒS”ÑT•S•ÈØÝ[Y[ÜÙ\]Y[˜Ù\È
Ü™Ø[š^˜][Û—ÚYÙ\]Y[˜ÙWÚÙ^K™Yš^™^Ý˜[YKY[™ËÜ™X]YØ]\]YØ]
HSQTÈ
ËËË‹K“ÕÊ
K“ÕÊ
JIÊKO™^XÝ]JÉ\ËO›Ü™Ø[š^˜][Û’Y	Ù^K	™Yš^ˆ	ËIÈˆ	YX\ˆˆ	ËI×JNÂˆ™]\›ˆ	™Yš^ˆ	ËIÈˆ	YX\ˆˆ	ËLIÎÂˆBˆ	\ËO™‹Oœ™\\™J	ÕTUHØÝ[Y[ÜÙ\]Y[˜Ù\ÈÑU™^Ý˜[YHH™^Ý˜[YH
ÈHÒT‘HYHÉÊKO™^XÝ]JÉÙ\]Y[˜ÙVÉÚY	×WJNÂˆ™]\›ˆ	™Yš^ˆ	ËIÈˆ	YX\ˆˆ	ËIÈˆÝ—ÜY

Ýš[™ÊH	Ù\]Y[˜ÙVÉÛ™^Ý˜[YI×K
[
H	Ù\]Y[˜ÙVÉÜY[™É×K	Ì	ËÕ—ÔQÓQ•
NÂˆB‚ˆš]˜]H[˜Ý[Ûˆ]ZY›ÜŠÝš[™È	ØÛÜK[	\™[[	[™JNˆÝš[™ÂˆÂˆ	^H\Ú
	ÜÚLM‰Ë[\ÙJ	ß	ËÉ\ËO›Ü™Ø[š^˜][Û’Y	ØÛÜK	\™[	[™WJJNÂˆ™]\›ˆÝXœÝŠ	^
Hˆ	ËIÈˆÝXœÝŠ	^
Hˆ	ËM	ÈˆÝXœÝŠ	^LËÊHˆ	ËXIÈˆÝXœÝŠ	^MËÊHˆ	ËIÈˆÝXœÝŠ	^ŒLŠNÂˆB‚ˆš]˜]H[˜Ý[Ûˆ]ZY

NˆÝš[™ÂˆÂˆ	ž]\ÈH˜[™ÛWØž]\ÊMŠNÂˆ	ž]\ÖÍ—HHÚŠ
Ü™
	ž]\ÖÍ—JH	ˆŠH
NÂˆ	ž]\ÖÎHHÚŠ
Ü™
	ž]\ÖÎJH	ˆÙŠH
NÂˆ™]\›ˆœÜš[Š	É\É\ËI\ËI\ËI\ËI\É\É\ÉËÝ—ÜÜ]
š[Œš^
	ž]\ÊK
JNÂˆB‚ˆš]˜]H[˜Ý[ÛˆÛ™JÝš[™È	Ü[\œ˜^H	\˜[\ÊNˆ\œ˜^_˜[ÙBˆÂˆ	Ý][Y[H	\ËO™‹Oœ™\\™J	Ü[
NÂˆ	Ý][Y[O™^XÝ]J	\˜[\ÊNÂˆ™]\›ˆ	Ý][Y[O™™]Ú

NÂˆB‚ˆš]˜]H[˜Ý[Ûˆ[
Ýš[™È	Ü[\œ˜^H	\˜[\ÊNˆ\œ˜^BˆÂˆ	Ý][Y[H	\ËO™‹Oœ™\\™J	Ü[
NÂˆ	Ý][Y[O™^XÝ]J	\˜[\ÊNÂˆ™]\›ˆ	Ý][Y[O™™]Ú[

NÂˆB‚ˆš]˜]H[˜Ý[Ûˆ˜[œØXÝ[ÛŠØ[X›H	Ø[˜XÚÊNˆZ^YˆÂˆ	ÝÛœÈHI\ËO™‹Oš[•˜[œØXÝ[ÛŠ
NÂˆYˆ
	ÝÛœÊHÂˆ	\ËO™‹O˜™YÚ[•˜[œØXÝ[ÛŠ
NÂˆBˆžHÂˆ	™\Ý[H	Ø[˜XÚÊ
NÂˆYˆ
	ÝÛœÊHÂˆ	\ËO™‹O˜ÛÛ[Z]

NÂˆBˆ™]\›ˆ	™\Ý[ÂˆHØ]Ú
›ÝØX›H	^Ù\[ÛŠHÂˆYˆ
	ÝÛœÈ	‰ˆ	\ËO™‹Oš[•˜[œØXÝ[ÛŠ
JHÂˆ	\ËO™‹Oœ›Û˜XÚÊ
NÂˆBˆ›ÝÈ	^Ù\[ÛŽÂˆBˆBŸB