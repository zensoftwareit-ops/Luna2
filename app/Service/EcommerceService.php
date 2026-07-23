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
            throw new InvalidArgumentException('L’endpoint e-commerce deve usare HTTPS.');
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
                $result = [];
                foreach ($payload['data']['products']['nodes'] ?? [] as $product) {
                    foreach ($product['variants']['nodes'] ?? [] as $variant) {
                        $variant['_product_id'] = $product['id'] ?? null;
                        $variant['_product_title'] = $product['title'] ?? '';
                        $result[] = $variant;
                    }
                }
                return $result;
            }],
            'EBAY' => [[
                'method' => 'GET', 'url' => $base . '/sell/inventory/v1/inventory_item',
                'options' => ['headers' => ['Authorization' => 'Bearer ' . ($credentials['access_token'] ?? ''), 'Accept-Language' => $settings['locale'] ?? 'it-IT'], 'query' => ['limit' => 200]],
            ], static fn (array $payload): array => $payload['inventoryItems'] ?? []],
            'AMAZON' => $this->amazonProductRequest($channel, $credentials, $settings),
            default => throw new InvalidArgumentException('Piattaforma non supportata.'),
        };
    }

    private function amazonProductRequest(array $channel, array $credentials, array $settings): array
    {
        $sellerId = trim((string) ($settings['seller_id'] ?? ''));
        if ($sellerId === '') {
            throw new InvalidArgumentException('Configura seller_id nelle impostazioni JSON del canale Amazon.');
        }
        $url = rtrim((string) $channel['base_url'], '/') . '/listings/2021-08-01/items/' . rawurlencode($sellerId);
        return [[
            'method' => 'GET', 'url' => $url,
            'options' => ['headers' => ['x-amz-access-token' => $this->amazonAccessToken($credentials), 'Accept' => 'application/json'], 'query' => [
                'marketplaceIds' => $settings['marketplace_id'] ?? 'APJ6JRA9NG5V4',
                'includedData' => 'summaries,offers,fulfillmentAvailability', 'pageSize' => 20,
            ]],
        ], static fn (array $payload): array => $payload['items'] ?? []];
    }

    private function normalizeProduct(string $platform, array $product): array
    {
        if ($platform === 'SHOPIFY') {
            return ['external_id'=>(string)($product['id']??''),'sku'=>(string)($product['sku']??''),'name'=>trim((string)($product['_product_title']??'').' · '.(string)($product['title']??''),' ·'),'price'=>(float)($product['price']??0),'quantity'=>(float)($product['inventoryQuantity']??0),'raw'=>$product];
        }
        if ($platform === 'EBAY') {
            return ['external_id'=>(string)($product['sku']??''),'sku'=>(string)($product['sku']??''),'name'=>(string)($product['product']['title']??$product['sku']??'Articolo eBay'),'price'=>null,'quantity'=>(float)($product['availability']['shipToLocationAvailability']['quantity']??0),'raw'=>$product];
        }
        if ($platform === 'AMAZON') {
            $summary = $product['summaries'][0] ?? []; $offer = $product['offers'][0] ?? []; $availability = $product['fulfillmentAvailability'][0] ?? [];
            return ['external_id'=>(string)($product['sku']??''),'sku'=>(string)($product['sku']??''),'name'=>(string)($summary['itemName']??$product['sku']??'Articolo Amazon'),'price'=>(float)($offer['price']['amount']??0),'quantity'=>(float)($availability['quantity']??0),'raw'=>$product];
        }
        return ['external_id'=>(string)($product['id']??''),'sku'=>(string)($product['sku']??''),'name'=>(string)($product['name']??'Articolo WooCommerce'),'price'=>(float)($product['price']??0),'quantity'=>(float)($product['stock_quantity']??0),'raw'=>$product];
    }

    private function orderRequest(array $channel): array
    {
        $credentials = $this->credentials($channel);
        $settings = json_decode((string) ($channel['settings_json'] ?? '{}'), true) ?: [];
        $base = rtrim((string) $channel['base_url'], '/');
        if (!str_starts_with($base, 'https://')) { throw new InvalidArgumentException('L’endpoint e-commerce deve usare HTTPS.'); }
        return match ($channel['platform']) {
            'WOOCOMMERCE' => [[
                'method' => 'GET', 'url' => $base . '/wp-json/wc/v3/orders',
                'options' => ['auth' => [$credentials['consumer_key'] ?? '', $credentials['consumer_secret'] ?? ''], 'query' => ['per_page' => 100, 'after' => $channel['sync_cursor'] ?: null]],
            ], fn (array $payload): array => $payload],
            'SHOPIFY' => [[
                'method' => 'POST', 'url' => $base . '/admin/api/' . ($settings['api_version'] ?? '2026-07') . '/graphql.json',
                'options' => ['headers' => ['X-Shopify-Access-Token' => $credentials['access_token'] ?? ''], 'json' => ['query' => '{ orders(first: 100, sortKey: CREATED_AT, reverse: true) { nodes { id name createdAt displayFinancialStatus email currencyCode totalPriceSet { shopMoney { amount currencyCode } } lineItems(first: 100) { nodes { id title sku quantity originalUnitPriceSet { shopMoney { amount } } discountedTotalSet { shopMoney { amount } } } } } } }']],
            ], static fn (array $payload): array => $payload['data']['orders']['nodes'] ?? []],
            'EBAY' => [[
                'method' => 'GET', 'url' => $base . '/sell/fulfillment/v1/order',
                'options' => ['headers' => ['Authorization' => 'Bearer ' . ($credentials['access_token'] ?? ''), 'X-EBAY-C-MARKETPLACE-ID' => $settings['marketplace_id'] ?? 'EBAY_IT'], 'query' => ['limit' => 200]],
            ], static fn (array $payload): array => $payload['orders'] ?? []],
            'AMAZON' => $this->amazonOrderRequest($channel, $credentials, $settings),
            default => throw new InvalidArgumentException('Piattaforma non supportata.'),
        };
    }

    private function amazonOrderRequest(array $channel, array $credentials, array $settings): array
    {
        $path = '/orders/2026-01-01/orders';
        $query = http_build_query([
            'marketplaceIds' => $settings['marketplace_id'] ?? 'APJ6JRA9NG5V4',
            'createdAfter' => $channel['sync_cursor'] ?: gmdate('Y-m-d\TH:i:s\Z', strtotime('-7 days')),
            'includedData' => 'BUYER,PROCEEDS,FULFILLMENT',
            'maxResultsPerPage' => 100,
        ], '', '&', PHP_QUERY_RFC3986);
        $url = rtrim((string) $channel['base_url'], '/') . $path . '?' . $query;
        return [[
            'method' => 'GET',
            'url' => $url,
            'options' => ['headers' => ['x-amz-access-token' => $this->amazonAccessToken($credentials), 'Accept' => 'application/json']],
        ], static fn (array $payload): array => $payload['orders'] ?? []];
    }

    private function amazonAccessToken(array $credentials): string
    {
        if (!empty($credentials['access_token'])) {
            return (string) $credentials['access_token'];
        }
        foreach (['refresh_token', 'client_id', 'client_secret'] as $required) {
            if (empty($credentials[$required])) {
                throw new InvalidArgumentException('Credenziali Amazon incomplete: servono refresh_token, client_id e client_secret.');
            }
        }
        $response = (new Client(['timeout' => 20, 'connect_timeout' => 10, 'http_errors' => true]))->post('https://api.amazon.com/auth/o2/token', [
            'form_params' => [
                'grant_type' => 'refresh_token',
                'refresh_token' => $credentials['refresh_token'],
                'client_id' => $credentials['client_id'],
                'client_secret' => $credentials['client_secret'],
            ],
        ]);
        $payload = json_decode((string) $response->getBody(), true, 512, JSON_THROW_ON_ERROR);
        if (empty($payload['access_token'])) {
            throw new InvalidArgumentException('Amazon LWA non ha restituito un access token.');
        }
        return (string) $payload['access_token'];
    }

    private function pushStock(array $channel, array $payload): void
    {
        if (empty($payload['product_id'])) { throw new InvalidArgumentException('Prodotto mancante per sincronizzazione giacenza.'); }
        $product = $this->one('SELECT p.*, COALESCE(SUM(b.quantity - b.reserved_quantity),0) available FROM products p LEFT JOIN inventory_balances b ON b.product_id = p.id WHERE p.id = ? AND p.organization_id = ? GROUP BY p.id', [(int) $payload['product_id'], $this->organizationId]);
        if (!$product) { throw new InvalidArgumentException('Prodotto non trovato.'); }
        $mapping = $this->one('SELECT * FROM ecommerce_products WHERE ecommerce_channel_id = ? AND product_id = ? AND organization_id = ?', [$channel['id'], $product['id'], $this->organizationId]);
        if (!$mapping) { throw new InvalidArgumentException('Prodotto non mappato sul canale.'); }
        $credentials = $this->credentials($channel); $settings = json_decode((string)($channel['settings_json']??'{}'),true)?:[]; $base = rtrim((string) $channel['base_url'], '/');
        $client = new Client(['timeout' => 30, 'connect_timeout' => 10]);
        if ($channel['platform'] === 'WOOCOMMERCE') {
            $client->put($base . '/wp-json/wc/v3/products/' . rawurlencode((string) $mapping['external_product_id']), ['auth' => [$credentials['consumer_key'] ?? '', $credentials['consumer_secret'] ?? ''], 'json' => ['stock_quantity' => (int) floor((float) $product['available']), 'manage_stock' => true]]);
        } elseif ($channel['platform'] === 'SHOPIFY') {
            $raw=json_decode((string)($mapping['raw_data_json']??'{}'),true)?:[];$inventoryItem=(string)($raw['inventoryItem']['id']??'');$location=(string)($settings['location_id']??'');
            if($inventoryItem===''||$location===''){throw new InvalidArgumentException('Shopify richiede inventoryItem e location_id nelle impostazioni del canale.');}
            $this->shopifyGraphql($client,$channel,$credentials,'mutation SetInventory($input: InventorySetQuantitiesInput!) { inventorySetQuantities(input: $input) { userErrors { field message } } }',['input'=>['name'=>'available','reason'=>'correction','referenceDocumentUri'=>'gid://luna2/Product/'.$product['id'],'quantities'=>[['inventoryItemId'=>$inventoryItem,'locationId'=>$location,'quantity'=>(int)floor((float)$product['available'])]]]]);
        } elseif ($channel['platform'] === 'EBAY') {
            $raw=json_decode((string)($mapping['raw_data_json']??'{}'),true)?:[];$raw['availability']['shipToLocationAvailability']['quantity']=(int)floor((float)$product['available']);unset($raw['sku']);
            $client->put($base.'/sell/inventory/v1/inventory_item/'.rawurlencode((string)$mapping['sku']),['headers'=>['Authorization'=>'Bearer '.($credentials['access_token']??''),'Content-Language'=>$settings['locale']??'it-IT'],'json'=>$raw]);
        } elseif ($channel['platform'] === 'AMAZON') {
            $this->amazonPatchListing($client,$channel,$credentials,$settings,(string)$mapping['sku'],'/attributes/fulfillment_availability',[['fulfillment_channel_code'=>$settings['fulfillment_channel_code']??'DEFAULT','quantity'=>(int)floor((float)$product['available'])]],$mapping);
        } else {
            throw new InvalidArgumentException('Piattaforma non supportata.');
        }
        $this->markChannelHealthy((int)$channel['id']);
    }

    private function pushPrice(array $channel, array $payload): void
    {
        [$product,$mapping]=$this->mappedProduct($channel,$payload);$price=round((float)$product['sale_price'],4);$credentials=$this->credentials($channel);$settings=json_decode((string)($channel['settings_json']??'{}'),true)?:[];$base=rtrim((string)$channel['base_url'],'/');$client=new Client(['timeout'=>30,'connect_timeout'=>10,'http_errors'=>true]);
        if($channel['platform']==='WOOCOMMERCE'){$client->put($base.'/wp-json/wc/v3/products/'.rawurlencode((string)$mapping['external_product_id']),['auth'=>[$credentials['consumer_key']??'',$credentials['consumer_secret']??''],'json'=>['regular_price'=>number_format($price,2,'.','')]]);
        }elseif($channel['platform']==='SHOPIFY'){$raw=json_decode((string)($mapping['raw_data_json']??'{}'),true)?:[];$productGid=(string)($raw['_product_id']??'');if($productGid===''){throw new InvalidArgumentException('Mapping Shopify senza product GID. Riesegui PULL_PRODUCTS.');}$this->shopifyGraphql($client,$channel,$credentials,'mutation Price($productId: ID!, $variants: [ProductVariantsBulkInput!]!) { productVariantsBulkUpdate(productId: $productId, variants: $variants) { userErrors { field message } } }',['productId'=>$productGid,'variants'=>[['id'=>(string)$mapping['external_product_id'],'price'=>(string)$price]]]);
        }elseif($channel['platform']==='AMAZON'){$this->amazonPatchListing($client,$channel,$credentials,$settings,(string)$mapping['sku'],'/attributes/purchasable_offer',[['audience'=>'ALL','currency'=>$settings['currency']??'EUR','our_price'=>[['schedule'=>[['value_with_tax'=>$price]]]]]],$mapping);
        }elseif($channel['platform']==='EBAY'){$offers=$client->get($base.'/sell/inventory/v1/offer',['headers'=>['Authorization'=>'Bearer '.($credentials['access_token']??'')],'query'=>['sku'=>$mapping['sku'],'marketplace_id'=>$settings['marketplace_id']??'EBAY_IT','limit'=>20]]);$data=json_decode((string)$offers->getBody(),true,512,JSON_THROW_ON_ERROR);$offer=$data['offers'][0]??null;if(!$offer||empty($offer['offerId'])){throw new InvalidArgumentException('Nessuna offerta eBay pubblicata per lo SKU.');}$offerId=$offer['offerId'];unset($offer['offerId'],$offer['listing'],$offer['status']);$offer['pricingSummary']['price']=['value'=>number_format($price,2,'.',''),'currency'=>$settings['currency']??'EUR'];$client->put($base.'/sell/inventory/v1/offer/'.rawurlencode((string)$offerId),['headers'=>['Authorization'=>'Bearer '.($credentials['access_token']??''),'Content-Language'=>$settings['locale']??'it-IT'],'json'=>$offer]);
        }else{throw new InvalidArgumentException('Piattaforma non supportata.');}
        $this->db->prepare('UPDATE ecommerce_products SET price=?,sync_status=\'SYNCED\',updated_at=NOW() WHERE id=?')->execute([$price,$mapping['id']]);$this->markChannelHealthy((int)$channel['id']);
    }

    private function acknowledgeOrder(array $channel,array $payload): void
    {
        $id=(int)($payload['ecommerce_order_id']??$payload['_entity_id']??0);$order=$this->one('SELECT * FROM ecommerce_orders WHERE id=? AND ecommerce_channel_id=? AND organization_id=?',[$id,$channel['id'],$this->organizationId]);if(!$order){throw new InvalidArgumentException('Ordine e-commerce da confermare non trovato.');}
        $credentials=$this->credentials($channel);$settings=json_decode((string)($channel['settings_json']??'{}'),true)?:[];$base=rtrim((string)$channel['base_url'],'/');$client=new Client(['timeout'=>30,'connect_timeout'=>10,'http_errors'=>true]);
        if($channel['platform']==='WOOCOMMERCE'){$client->put($base.'/wp-json/wc/v3/orders/'.rawurlencode((string)$order['external_order_id']),['auth'=>[$credentials['consumer_key']??'',$credentials['consumer_secret']??''],'json'=>['meta_data'=>[['key'=>'luna2_imported_at','value'=>date(DATE_ATOM)]]]]);
        }elseif($channel['platform']==='SHOPIFY'){$this->shopifyGraphql($client,$channel,$credentials,'mutation Ack($id: ID!, $tags: [String!]!) { tagsAdd(id: $id, tags: $tags) { userErrors { field message } } }',['id'=>$order['external_order_id'],'tags'=>['Luna2-importato']]);
        }else{$this->db->prepare("INSERT INTO ecommerce_sync_logs (organization_id,ecommerce_channel_id,sync_type,status,started_at,ended_at,processed_count,error_count,message,created_at) VALUES (?,?,'ACK_ORDER','SUCCESS',NOW(),NOW(),1,0,?,NOW())")->execute([$this->organizationId,$channel['id'],'Ordine '.$order['external_order_id'].' acquisito in Luna2; il provider non espone un ack non distruttivo.']);}
        $this->markChannelHealthy((int)$channel['id']);
    }

    private function mappedProduct(array $channel,array $payload): array
    {
        $id=(int)($payload['product_id']??$payload['_entity_id']??0);if($id<=0){throw new InvalidArgumentException('Prodotto mancante per la sincronizzazione.');}$product=$this->one('SELECT p.*,COALESCE(SUM(b.quantity-b.reserved_quantity),0) available FROM products p LEFT JOIN inventory_balances b ON b.product_id=p.id WHERE p.id=? AND p.organization_id=? GROUP BY p.id',[$id,$this->organizationId]);if(!$product){throw new InvalidArgumentException('Prodotto non trovato.');}$mapping=$this->one('SELECT * FROM ecommerce_products WHERE ecommerce_channel_id=? AND product_id=? AND organization_id=?',[$channel['id'],$id,$this->organizationId]);if(!$mapping){throw new InvalidArgumentException('Prodotto non mappato sul canale. Esegui prima PULL_PRODUCTS.');}return [$product,$mapping];
    }

    private function shopifyGraphql(Client $client,array $channel,array $credentials,string $query,array $variables): array
    {
        $settings=json_decode((string)($channel['settings_json']??'{}'),true)?:[];$response=$client->post(rtrim((string)$channel['base_url'],'/').'/admin/api/'.($settings['api_version']??'2026-07').'/graphql.json',['headers'=>['X-Shopify-Access-Token'=>$credentials['access_token']??''],'json'=>['query'=>$query,'variables'=>$variables]]);$data=json_decode((string)$response->getBody(),true,512,JSON_THROW_ON_ERROR);$errors=$data['errors']??[];foreach((array)($data['data']??[]) as $result){foreach((array)($result['userErrors']??[]) as $error){$errors[]=$error;}}if($errors!==[]){throw new InvalidArgumentException('Shopify: '.json_encode($errors,JSON_UNESCAPED_UNICODE));}return $data;
    }

    private function amazonPatchListing(Client $client,array $channel,array $credentials,array $settings,string $sku,string $path,array $value,array $mapping): void
    {
        $seller=trim((string)($settings['seller_id']??''));$marketplace=(string)($settings['marketplace_id']??'APJ6JRA9NG5V4');$raw=json_decode((string)($mapping['raw_data_json']??'{}'),true)?:[];$productType=(string)($raw['summaries'][0]['productType']??$settings['product_type']??'PRODUCT');if($seller===''||$sku===''){throw new InvalidArgumentException('Amazon richiede seller_id e SKU mappato.');}$client->patch(rtrim((string)$channel['base_url'],'/').'/listings/2021-08-01/items/'.rawurlencode($seller).'/'.rawurlencode($sku),['headers'=>['x-amz-access-token'=>$this->amazonAccessToken($credentials),'Accept'=>'application/json'],'query'=>['marketplaceIds'=>$marketplace],'json'=>['productType'=>$productType,'patches'=>[['op'=>'replace','path'=>$path,'value'=>$value]]]]);
    }

    private function markChannelHealthy(int $channelId): void{$this->db->prepare("UPDATE ecommerce_channels SET status='CONNECTED',last_success_at=NOW(),last_error=NULL,updated_at=NOW() WHERE id=? AND organization_id=?")->execute([$channelId,$this->organizationId]);}

    private function processWebhookPayload(array $channel, array $payload): void
    {
        $this->upsertExternalOrder($channel, $payload);
    }

    private function upsertExternalOrder(array $channel, array $payload): void
    {
        $normalized = $this->normalizeOrder((string) $channel['platform'], $payload);
        if ($normalized['external_id'] === '') { throw new InvalidArgumentException('ID ordine esterno mancante.'); }
        $this->db->prepare('INSERT INTO ecommerce_orders (organization_id, ecommerce_channel_id, order_date, platform, external_order_id, customer_email, total, currency, status, import_status, raw_data_json, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, \'PENDING\', ?, ?, ?, NOW(), NOW()) ON DUPLICATE KEY UPDATE customer_email = VALUES(customer_email), total = VALUES(total), currency = VALUES(currency), status = VALUES(status), raw_data_json = VALUES(raw_data_json), updated_at = NOW(), id = LAST_INSERT_ID(id)')
            ->execute([$this->organizationId, $channel['id'], $normalized['date'], $channel['platform'], $normalized['external_id'], $normalized['email'] ?: null, $normalized['total'], $normalized['currency'], $normalized['status'], json_encode($payload, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES), $this->userId ?: null, $this->userId ?: null]);
        $orderId = (int) $this->db->lastInsertId();
        $this->db->prepare('DELETE FROM ecommerce_order_lines WHERE ecommerce_order_id = ?')->execute([$orderId]);
        $insert = $this->db->prepare('INSERT INTO ecommerce_order_lines (organization_id, ecommerce_order_id, external_line_id, external_product_id, product_id, sku, description, quantity, unit_price, tax_amount, total_amount, raw_data_json) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)');
        foreach ($normalized['lines'] as $line) {
            $product = $this->one('SELECT id FROM products WHERE organization_id = ? AND (sku = ? OR code = ? OR ean = ?) LIMIT 1', [$this->organizationId, $line['sku'], $line['sku'], $line['sku']]);
            $insert->execute([$this->organizationId, $orderId, $line['id'], $line['product_id'], $product['id'] ?? null, $line['sku'] ?: null, $line['description'], $line['quantity'], $line['unit_price'], $line['tax'], $line['total'], json_encode($line['raw'], JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES)]);
        }
    }

    private function normalizeOrder(string $platform, array $p): array
    {
        if ($platform === 'SHOPIFY') {
            $nodes = $p['lineItems']['nodes'] ?? [];
            return ['external_id' => (string) ($p['id'] ?? ''), 'date' => substr((string) ($p['createdAt'] ?? date('c')), 0, 10), 'email' => (string) ($p['email'] ?? ''), 'total' => (float) ($p['totalPriceSet']['shopMoney']['amount'] ?? 0), 'currency' => (string) ($p['currencyCode'] ?? 'EUR'), 'status' => (string) ($p['displayFinancialStatus'] ?? ''), 'lines' => array_map(static fn (array $l): array => ['id' => (string) ($l['id'] ?? ''), 'product_id' => '', 'sku' => (string) ($l['sku'] ?? ''), 'description' => (string) ($l['title'] ?? ''), 'quantity' => (float) ($l['quantity'] ?? 0), 'unit_price' => (float) ($l['originalUnitPriceSet']['shopMoney']['amount'] ?? 0), 'tax' => 0, 'total' => (float) ($l['discountedTotalSet']['shopMoney']['amount'] ?? 0), 'raw' => $l], $nodes)];
        }
        if ($platform === 'AMAZON') {
            $lines=[];
            foreach((array)($p['orderItems']??[]) as $line){$tax=0.0;foreach((array)($line['proceeds']['breakdowns']??[]) as $breakdown){if(($breakdown['type']??'')==='TAX'){$tax+=(float)($breakdown['subtotal']['amount']??0);}}$quantity=(float)($line['quantityOrdered']??1);$total=(float)($line['proceeds']['proceedsTotal']['amount']??0);$unit=(float)($line['product']['price']['unitPrice']['amount']??($quantity>0?($total-$tax)/$quantity:0));$lines[]=['id'=>(string)($line['orderItemId']??''),'product_id'=>(string)($line['product']['asin']??''),'sku'=>(string)($line['product']['sellerSku']??''),'description'=>(string)($line['product']['title']??'Articolo Amazon'),'quantity'=>$quantity,'unit_price'=>$unit,'tax'=>$tax,'total'=>$total,'raw'=>$line];}
            return ['external_id'=>(string)($p['orderId']??''),'date'=>substr((string)($p['createdTime']??date('c')),0,10),'email'=>(string)($p['buyer']['buyerEmail']??''),'total'=>(float)($p['proceeds']['grandTotal']['amount']??array_sum(array_column($lines,'total'))),'currency'=>(string)($p['proceeds']['grandTotal']['currencyCode']??($lines[0]['raw']['proceeds']['proceedsTotal']['currencyCode']??'EUR')),'status'=>(string)($p['fulfillment']['fulfillmentStatus']??''),'lines'=>$lines];
        }
        $lines = $p['line_items'] ?? $p['lineItems'] ?? $p['orderFulfillmentStatus']['lineItems'] ?? [];
        return ['external_id' => (string) ($p['id'] ?? $p['orderId'] ?? $p['AmazonOrderId'] ?? ''), 'date' => substr((string) ($p['date_created_gmt'] ?? $p['creationDate'] ?? $p['PurchaseDate'] ?? date('c')), 0, 10), 'email' => (string) ($p['billing']['email'] ?? $p['buyer']['buyerRegistrationAddress']['email'] ?? $p['BuyerInfo']['BuyerEmail'] ?? ''), 'total' => (float) ($p['total'] ?? $p['pricingSummary']['total']['value'] ?? $p['OrderTotal']['Amount'] ?? 0), 'currency' => (string) ($p['currency'] ?? $p['pricingSummary']['total']['currency'] ?? $p['OrderTotal']['CurrencyCode'] ?? 'EUR'), 'status' => (string) ($p['status'] ?? $p['orderFulfillmentStatus'] ?? $p['OrderStatus'] ?? ''), 'lines' => array_map(static fn (array $l): array => ['id' => (string) ($l['id'] ?? $l['lineItemId'] ?? ''), 'product_id' => (string) ($l['product_id'] ?? $l['legacyItemId'] ?? ''), 'sku' => (string) ($l['sku'] ?? $l['lineItemSKU'] ?? ''), 'description' => (string) ($l['name'] ?? $l['title'] ?? 'Articolo'), 'quantity' => (float) ($l['quantity'] ?? 1), 'unit_price' => (float) ($l['price'] ?? $l['lineItemCost']['value'] ?? 0), 'tax' => (float) ($l['total_tax'] ?? 0), 'total' => (float) ($l['total'] ?? $l['lineItemCost']['value'] ?? 0), 'raw' => $l], is_array($lines) ? $lines : [])];
    }

    private function verifySignature(array $channel, string $body, array $headers, array $credentials): bool
    {
        $header = static function (array $headers, string $name): string { foreach ($headers as $key => $value) { if (strcasecmp((string) $key, $name) === 0) { return is_array($value) ? (string) reset($value) : (string) $value; } } return ''; };
        $secret = $credentials['webhook_secret'] ?? SecretResolver::resolve($channel['webhook_secret_reference'] ?? null);
        if ($channel['platform'] === 'WOOCOMMERCE') { return hash_equals(base64_encode(hash_hmac('sha256', $body, $secret, true)), $header($headers, 'X-WC-Webhook-Signature')); }
        if ($channel['platform'] === 'SHOPIFY') { return hash_equals(base64_encode(hash_hmac('sha256', $body, $secret, true)), $header($headers, 'X-Shopify-Hmac-Sha256')); }
        return $secret !== '' && hash_equals(hash_hmac('sha256', $body, $secret), $header($headers, 'X-Luna-Signature'));
    }

    private function credentials(array $channel, bool $optional = false): array
    {
        try { $json = SecretResolver::resolve($channel['secret_reference'] ?? null); }
        catch (Throwable $e) { if ($optional) { return []; } throw $e; }
        $credentials = json_decode($json, true);
        if (!is_array($credentials)) { throw new InvalidArgumentException('Il segreto del canale deve contenere JSON valido.'); }
        return $credentials;
    }

    private function resolveCustomer(string $email): array
    {
        $customer = $email !== '' ? $this->one('SELECT * FROM customers WHERE organization_id = ? AND email = ? LIMIT 1', [$this->organizationId, $email]) : false;
        if ($customer) { return $customer; }
        $code = 'ECOM-' . strtoupper(substr(hash('sha256', $email ?: uniqid('', true)), 0, 10));
        $name = $email !== '' ? $email : 'Cliente e-commerce';
        $this->db->prepare('INSERT INTO customers (organization_id, code, business_name, email, country_code, active, created_by, updated_by, created_at, updated_at) VALUES (?, ?, ?, ?, \'IT\', 1, ?, ?, NOW(), NOW())')
            ->execute([$this->organizationId, $code, $name, $email ?: null, $this->userId ?: null, $this->userId ?: null]);
        return ['id' => (int) $this->db->lastInsertId(), 'business_name' => $name];
    }

    private function channel(int $id): array
    {
        $channel = $this->one('SELECT * FROM ecommerce_channels WHERE id = ? AND organization_id = ? AND active = 1', [$id, $this->organizationId]);
        if (!$channel) { throw new InvalidArgumentException('Canale e-commerce non valido.'); }
        return $channel;
    }

    private function one(string $sql, array $params): array|false { $s = $this->db->prepare($sql); $s->execute($params); return $s->fetch(); }
    private function all(string $sql, array $params): array { $s = $this->db->prepare($sql); $s->execute($params); return $s->fetchAll(); }
}
