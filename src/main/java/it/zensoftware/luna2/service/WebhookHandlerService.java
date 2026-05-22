package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.EcommerceOrderDAO;
import it.zensoftware.luna2.dao.EcommerceProductDAO;
import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.EcommerceProduct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Webhook handler for real-time notifications from eCommerce platforms
 * Supports WooCommerce, Shopify, Amazon, eBay webhooks
 */
public class WebhookHandlerService {

    private static final Logger logger = LogManager.getLogger(WebhookHandlerService.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private InventorySyncService inventoryService;
    private EcommerceOrderIntegrationService integrationService;
    private EcommercePaymentReconciliationService paymentReconciliationService;
    private EcommerceShipmentTrackingService shipmentTrackingService;

    private EcommerceOrderDAO getOrderDAO() {
        if (orderDAO == null) {
            orderDAO = new EcommerceOrderDAO();
        }
        return orderDAO;
    }

    private EcommerceProductDAO getProductDAO() {
        if (productDAO == null) {
            productDAO = new EcommerceProductDAO();
        }
        return productDAO;
    }

    private InventorySyncService getInventoryService() {
        if (inventoryService == null) {
            inventoryService = new InventorySyncService();
        }
        return inventoryService;
    }

    private EcommerceOrderIntegrationService getIntegrationService() {
        if (integrationService == null) {
            integrationService = new EcommerceOrderIntegrationService();
        }
        return integrationService;
    }

    private EcommercePaymentReconciliationService getPaymentReconciliationService() {
        if (paymentReconciliationService == null) {
            paymentReconciliationService = new EcommercePaymentReconciliationService();
        }
        return paymentReconciliationService;
    }

    private EcommerceShipmentTrackingService getShipmentTrackingService() {
        if (shipmentTrackingService == null) {
            shipmentTrackingService = new EcommerceShipmentTrackingService();
        }
        return shipmentTrackingService;
    }

    /**
     * Handle incoming webhook from WooCommerce
     */
    public boolean handleWooCommerceWebhook(String payload, String signature, String secret) {
        try {
            if (!verifyWooCommerceSignature(payload, signature, secret)) {
                logger.warn("Invalid WooCommerce webhook signature");
                return false;
            }

            JsonNode data = objectMapper.readTree(payload);
            String event = data.has("event") ? data.get("event").asText() : "";

            switch (event) {
                case "order.created":
                case "order.updated":
                    processOrderWebhook(data, EcommercePlatform.PlatformType.WOOCOMMERCE);
                    break;
                case "product.created":
                case "product.updated":
                    processProductWebhook(data, EcommercePlatform.PlatformType.WOOCOMMERCE);
                    break;
                default:
                    logger.debug("Unhandled WooCommerce event: " + event);
            }

            return true;
        } catch (Exception e) {
            logger.error("Error handling WooCommerce webhook", e);
            return false;
        }
    }

    /**
     * Handle incoming webhook from Shopify
     */
    public boolean handleShopifyWebhook(String payload, String signature, String secret) {
        try {
            if (!verifyShopifySignature(payload, signature, secret)) {
                logger.warn("Invalid Shopify webhook signature");
                return false;
            }

            JsonNode data = objectMapper.readTree(payload);
            String topic = data.has("topic") ? data.get("topic").asText() : "";

            switch (topic) {
                case "orders/created":
                case "orders/updated":
                    processOrderWebhook(data, EcommercePlatform.PlatformType.SHOPIFY);
                    break;
                case "products/created":
                case "products/updated":
                    processProductWebhook(data, EcommercePlatform.PlatformType.SHOPIFY);
                    break;
                case "inventory_levels/update":
                    processInventoryWebhook(data, EcommercePlatform.PlatformType.SHOPIFY);
                    break;
                default:
                    logger.debug("Unhandled Shopify event: " + topic);
            }

            return true;
        } catch (Exception e) {
            logger.error("Error handling Shopify webhook", e);
            return false;
        }
    }

    /**
     * Handle incoming webhook from Amazon
     */
    public boolean handleAmazonWebhook(String payload, String signature, String secret) {
        try {
            // Amazon uses different signature verification
            if (!verifyAmazonSignature(payload, signature, secret)) {
                logger.warn("Invalid Amazon webhook signature");
                return false;
            }

            JsonNode data = objectMapper.readTree(payload);
            String eventType = data.has("EventType") ? data.get("EventType").asText() : "";

            switch (eventType) {
                case "ORDER_STATUS_CHANGE":
                    processOrderWebhook(data, EcommercePlatform.PlatformType.AMAZON);
                    break;
                case "INVENTORY_CHANGED":
                    processInventoryWebhook(data, EcommercePlatform.PlatformType.AMAZON);
                    break;
                default:
                    logger.debug("Unhandled Amazon event: " + eventType);
            }

            return true;
        } catch (Exception e) {
            logger.error("Error handling Amazon webhook", e);
            return false;
        }
    }

    /**
     * Handle incoming webhook from eBay
     */
    public boolean handleEbayWebhook(String payload, String signature, String secret) {
        try {
            if (!verifyEbaySignature(payload, signature, secret)) {
                logger.warn("Invalid eBay webhook signature");
                return false;
            }

            JsonNode data = objectMapper.readTree(payload);
            String event = data.has("event") ? data.get("event").asText() : "";

            switch (event) {
                case "order.created":
                case "order.updated":
                    processOrderWebhook(data, EcommercePlatform.PlatformType.EBAY);
                    break;
                case "inventory.updated":
                    processInventoryWebhook(data, EcommercePlatform.PlatformType.EBAY);
                    break;
                default:
                    logger.debug("Unhandled eBay event: " + event);
            }

            return true;
        } catch (Exception e) {
            logger.error("Error handling eBay webhook", e);
            return false;
        }
    }

    private void processOrderWebhook(JsonNode data, EcommercePlatform.PlatformType platformType) {
        try {
            logger.info("Processing order webhook from " + platformType);
            EcommerceOrder order = null;

            switch (platformType) {
                case WOOCOMMERCE:
                    order = parseWooCommerceOrder(data);
                    break;
                case SHOPIFY:
                    order = parseShopifyOrder(data);
                    break;
                case AMAZON:
                    order = parseAmazonOrder(data);
                    break;
                case EBAY:
                    order = parseEbayOrder(data);
                    break;
            }

            if (order != null) {
                order.setPlatformType(platformType);
                EcommerceOrder existing = getOrderDAO().findByPlatformAndExternalId(
                        order.getPlatformType(), order.getExternalOrderId());

                if (existing != null) {
                    order.setId(existing.getId());
                    getOrderDAO().update(order);
                    logger.info("Order updated via webhook: " + order.getExternalOrderId());
                } else {
                    getOrderDAO().save(order);
                    logger.info("New order saved via webhook: " + order.getExternalOrderId());
                }

                // Integrate order into Luna2 sales workflow
                getIntegrationService().integrateEcommerceOrderToLuna2(order);

                // Track payment status
                getPaymentReconciliationService().reconcileOrderPayment(order);

                // Track shipment status
                getShipmentTrackingService().trackShipment(order);
            }
        } catch (Exception e) {
            logger.error("Error processing order webhook", e);
        }
    }

    private void processProductWebhook(JsonNode data, EcommercePlatform.PlatformType platformType) {
        try {
            logger.info("Processing product webhook from " + platformType);
            EcommerceProduct product = null;

            switch (platformType) {
                case WOOCOMMERCE:
                    product = parseWooCommerceProduct(data);
                    break;
                case SHOPIFY:
                    product = parseShopifyProduct(data);
                    break;
                case AMAZON:
                    product = parseAmazonProduct(data);
                    break;
                case EBAY:
                    product = parseEbayProduct(data);
                    break;
            }

            if (product != null) {
                EcommerceProduct existing = getProductDAO().findByPlatformAndExternalId(
                        product.getPlatformType(), product.getExternalProductId());

                if (existing != null) {
                    product.setId(existing.getId());
                    getProductDAO().update(product);
                    logger.info("Product updated via webhook: " + product.getExternalProductId());
                } else {
                    getProductDAO().save(product);
                    logger.info("New product saved via webhook: " + product.getExternalProductId());
                }
            }
        } catch (Exception e) {
            logger.error("Error processing product webhook", e);
        }
    }

    private void processInventoryWebhook(JsonNode data, EcommercePlatform.PlatformType platformType) {
        try {
            logger.info("Processing inventory webhook from " + platformType);
            getInventoryService().syncInventoryFromPlatforms();
        } catch (Exception e) {
            logger.error("Error processing inventory webhook", e);
        }
    }

    private EcommerceOrder parseWooCommerceOrder(JsonNode json) {
        EcommerceOrder order = new EcommerceOrder();
        order.setExternalOrderId("wc_" + json.get("id").asLong());
        order.setOrderNumber(json.get("number").asText());
        order.setOrderDate(new Date());
        order.setCustomerEmail(json.get("billing").get("email").asText());
        order.setCustomerName(json.get("billing").get("first_name").asText() + " " +
                json.get("billing").get("last_name").asText());
        order.setTotalAmount(new java.math.BigDecimal(json.get("total").asText()));
        order.setCurrency(json.has("currency_code") ? json.get("currency_code").asText() : "EUR");
        order.setOrderStatus(json.get("status").asText());
        order.setExternalJson(json.toString());
        return order;
    }

    private EcommerceProduct parseWooCommerceProduct(JsonNode json) {
        EcommerceProduct product = new EcommerceProduct();
        product.setExternalProductId("wc_" + json.get("id").asLong());
        product.setProductName(json.get("name").asText());
        product.setSku(json.has("sku") ? json.get("sku").asText() : "");
        product.setDescription(json.has("description") ? json.get("description").asText() : "");
        product.setPrice(new java.math.BigDecimal(json.get("price").asText()));
        product.setCurrency("EUR");
        product.setStockQuantity(json.has("stock_quantity") ? json.get("stock_quantity").asInt() : 0);
        product.setExternalJson(json.toString());
        return product;
    }

    private EcommerceOrder parseShopifyOrder(JsonNode json) {
        EcommerceOrder order = new EcommerceOrder();
        order.setExternalOrderId("shopify_" + json.get("id").asLong());
        order.setOrderNumber(json.get("order_number").asText());
        order.setOrderDate(new Date());
        order.setCustomerEmail(json.has("customer") ? json.get("customer").get("email").asText() : "");
        order.setCustomerName(json.has("customer") ?
                json.get("customer").get("first_name").asText() + " " +
                json.get("customer").get("last_name").asText() : "");
        order.setTotalAmount(new java.math.BigDecimal(json.get("total_price").asText()));
        order.setCurrency(json.get("currency").asText());
        order.setOrderStatus(json.get("financial_status").asText());
        order.setExternalJson(json.toString());
        return order;
    }

    private EcommerceProduct parseShopifyProduct(JsonNode json) {
        EcommerceProduct product = new EcommerceProduct();
        product.setExternalProductId("shopify_" + json.get("id").asLong());
        product.setProductName(json.get("title").asText());
        product.setDescription(json.has("body_html") ? json.get("body_html").asText() : "");
        product.setPrice(new java.math.BigDecimal("0"));
        product.setCurrency("EUR");
        product.setExternalJson(json.toString());
        return product;
    }

    private EcommerceOrder parseAmazonOrder(JsonNode json) {
        EcommerceOrder order = new EcommerceOrder();
        order.setExternalOrderId("amz_" + json.get("AmazonOrderId").asText());
        order.setOrderNumber(json.get("AmazonOrderId").asText());
        order.setOrderDate(new Date());
        order.setCustomerEmail(json.has("BuyerEmail") ? json.get("BuyerEmail").asText() : "");
        order.setCustomerName(json.has("BuyerName") ? json.get("BuyerName").asText() : "");
        order.setTotalAmount(new java.math.BigDecimal(
                json.has("OrderTotal") ? json.get("OrderTotal").get("Amount").asText() : "0"));
        order.setCurrency(json.has("OrderTotal") ? json.get("OrderTotal").get("CurrencyCode").asText() : "EUR");
        order.setOrderStatus(json.has("OrderStatus") ? json.get("OrderStatus").asText() : "pending");
        order.setExternalJson(json.toString());
        return order;
    }

    private EcommerceProduct parseAmazonProduct(JsonNode json) {
        EcommerceProduct product = new EcommerceProduct();
        product.setExternalProductId("amz_" + json.get("asin").asText());
        product.setProductName(json.has("title") ? json.get("title").asText() : "");
        product.setSku(json.has("sku") ? json.get("sku").asText() : "");
        product.setPrice(new java.math.BigDecimal("0"));
        product.setCurrency("EUR");
        product.setStockQuantity(0);
        product.setExternalJson(json.toString());
        return product;
    }

    private EcommerceOrder parseEbayOrder(JsonNode json) {
        EcommerceOrder order = new EcommerceOrder();
        order.setExternalOrderId("ebay_" + json.get("orderId").asText());
        order.setOrderNumber(json.get("orderId").asText());
        order.setOrderDate(new Date());
        if (json.has("buyer")) {
            JsonNode buyer = json.get("buyer");
            order.setCustomerEmail(buyer.has("email") ? buyer.get("email").asText() : "");
            order.setCustomerName(buyer.has("username") ? buyer.get("username").asText() : "");
        }
        if (json.has("pricingSummary")) {
            JsonNode pricing = json.get("pricingSummary");
            order.setTotalAmount(new java.math.BigDecimal(
                    pricing.has("total") ? pricing.get("total").asText() : "0"));
            order.setCurrency(pricing.has("currency") ? pricing.get("currency").asText() : "EUR");
        }
        order.setOrderStatus(json.has("orderStatus") ? json.get("orderStatus").asText() : "pending");
        order.setExternalJson(json.toString());
        return order;
    }

    private EcommerceProduct parseEbayProduct(JsonNode json) {
        EcommerceProduct product = new EcommerceProduct();
        product.setExternalProductId("ebay_" + json.get("sku").asText());
        product.setProductName(json.has("title") ? json.get("title").asText() : "");
        product.setSku(json.get("sku").asText());
        product.setDescription(json.has("description") ? json.get("description").asText() : "");
        if (json.has("price")) {
            product.setPrice(new java.math.BigDecimal(json.get("price").asText()));
        } else {
            product.setPrice(new java.math.BigDecimal("0"));
        }
        product.setCurrency(json.has("currency") ? json.get("currency").asText() : "EUR");
        if (json.has("availability")) {
            JsonNode avail = json.get("availability");
            product.setStockQuantity(avail.has("availableQuantity") ? avail.get("availableQuantity").asInt() : 0);
        }
        product.setExternalJson(json.toString());
        return product;
    }

    private boolean verifyWooCommerceSignature(String payload, String signature, String secret) throws Exception {
        String computed = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA256").digest(
                        (payload + secret).getBytes()
                )
        );
        return computed.equals(signature);
    }

    private boolean verifyShopifySignature(String payload, String signature, String secret) throws Exception {
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes());
        String computed = Base64.getEncoder().encodeToString(digest);
        return computed.equals(signature);
    }

    private boolean verifyAmazonSignature(String payload, String signature, String secret) throws Exception {
        // Amazon uses X.509 certificate verification
        return true; // Placeholder - implement certificate validation
    }

    private boolean verifyEbaySignature(String payload, String signature, String secret) throws Exception {
        // eBay uses HMAC-SHA256
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes());
        String computed = Base64.getEncoder().encodeToString(digest);
        return computed.equals(signature);
    }

    /**
     * Generate webhook secret for platform registration
     */
    public static String generateWebhookSecret() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * Get webhook endpoint URL for a platform
     */
    public static String getWebhookUrl(EcommercePlatform.PlatformType platformType) {
        String baseUrl = System.getenv("WEBHOOK_BASE_URL");
        if (baseUrl == null) {
            baseUrl = "https://api.luna2.local/webhook";
        }
        return baseUrl + "/" + platformType.toString().toLowerCase();
    }
}
