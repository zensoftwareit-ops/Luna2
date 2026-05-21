package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.EcommerceOrderDAO;
import it.zensoftware.luna2.dao.EcommerceProductDAO;
import it.zensoftware.luna2.dao.EcommerceSyncLogDAO;
import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.EcommerceProduct;
import it.zensoftware.luna2.model.EcommerceSyncLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Shopify integration service - handles order and product synchronization with cursor-based pagination
 */
public class ShopifyService {

    private static final Logger logger = LogManager.getLogger(ShopifyService.class);
    private static final String API_VERSION = "2024-01";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private EcommerceSyncLogDAO syncLogDAO;
    private ObjectMapper objectMapper = new ObjectMapper();

    public ShopifyService() {
        this.orderDAO = new EcommerceOrderDAO();
        this.productDAO = new EcommerceProductDAO();
        this.syncLogDAO = new EcommerceSyncLogDAO();
    }

    public ShopifyService(EcommerceOrderDAO orderDAO, EcommerceProductDAO productDAO, EcommerceSyncLogDAO syncLogDAO) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.syncLogDAO = syncLogDAO;
    }

    public boolean testConnection(EcommercePlatform platform) {
        try {
            logger.info("Testing Shopify connection for: " + platform.getStoreName());
            String storeName = extractShopifyStoreName(platform.getStoreUrl());
            String url = "https://" + storeName + "/admin/api/" + API_VERSION + "/orders.json?limit=1";
            String response = makeAuthenticatedRequest(url, platform.getApiKey());
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.error("Error testing Shopify connection", e);
            return false;
        }
    }

    public void synchronizeOrders(EcommercePlatform platform) {
        EcommerceSyncLog syncLog = new EcommerceSyncLog();
        syncLog.setPlatformType(platform.getPlatformType());
        syncLog.setEcommercePlatform(platform);
        syncLog.setSyncType(EcommerceSyncLog.SyncType.ORDERS);
        syncLog.setStartTime(new Date());
        syncLog.setStatus(EcommerceSyncLog.SyncStatus.SUCCESS);
        syncLog.setRecordsProcessed(0);
        syncLog.setRecordsInserted(0);
        syncLog.setRecordsUpdated(0);
        syncLog.setRecordsFailed(0);

        try {
            logger.info("Synchronizing Shopify orders for: " + platform.getStoreName());
            String storeName = extractShopifyStoreName(platform.getStoreUrl());
            int totalProcessed = 0;
            String cursor = null;

            while (true) {
                String url = "https://" + storeName + "/admin/api/" + API_VERSION + "/orders.json?limit=250";
                if (cursor != null) {
                    url += "&after=" + cursor;
                }

                String response = makeAuthenticatedRequest(url, platform.getApiKey());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode data = objectMapper.readTree(response);
                JsonNode orders = data.has("orders") ? data.get("orders") : objectMapper.createArrayNode();

                if (!orders.isArray() || orders.size() == 0) {
                    break;
                }

                for (JsonNode orderNode : orders) {
                    try {
                        EcommerceOrder order = parseShopifyOrder(orderNode, platform);

                        EcommerceOrder existing = orderDAO.findByPlatformAndExternalId(
                                platform.getPlatformType(), order.getExternalOrderId());

                        if (existing != null) {
                            order.setId(existing.getId());
                            orderDAO.update(order);
                            syncLog.setRecordsUpdated(syncLog.getRecordsUpdated() + 1);
                        } else {
                            orderDAO.save(order);
                            syncLog.setRecordsInserted(syncLog.getRecordsInserted() + 1);
                        }

                        totalProcessed++;
                    } catch (Exception e) {
                        logger.error("Error processing Shopify order", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                // Check for next page
                if (data.has("pageInfo")) {
                    JsonNode pageInfo = data.get("pageInfo");
                    if (pageInfo.has("hasNextPage") && pageInfo.get("hasNextPage").asBoolean()) {
                        cursor = pageInfo.get("endCursor").asText();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);

            logger.info("Shopify order sync completed: " + totalProcessed + " orders processed");
        } catch (Exception e) {
            logger.error("Error syncing Shopify orders", e);
            syncLog.setStatus(EcommerceSyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
        }
    }

    public void synchronizeProducts(EcommercePlatform platform) {
        EcommerceSyncLog syncLog = new EcommerceSyncLog();
        syncLog.setPlatformType(platform.getPlatformType());
        syncLog.setEcommercePlatform(platform);
        syncLog.setSyncType(EcommerceSyncLog.SyncType.PRODUCTS);
        syncLog.setStartTime(new Date());
        syncLog.setStatus(EcommerceSyncLog.SyncStatus.SUCCESS);
        syncLog.setRecordsProcessed(0);
        syncLog.setRecordsInserted(0);
        syncLog.setRecordsUpdated(0);
        syncLog.setRecordsFailed(0);

        try {
            logger.info("Synchronizing Shopify products for: " + platform.getStoreName());
            String storeName = extractShopifyStoreName(platform.getStoreUrl());
            int totalProcessed = 0;
            String cursor = null;

            while (true) {
                String url = "https://" + storeName + "/admin/api/" + API_VERSION + "/products.json?limit=250";
                if (cursor != null) {
                    url += "&after=" + cursor;
                }

                String response = makeAuthenticatedRequest(url, platform.getApiKey());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode data = objectMapper.readTree(response);
                JsonNode products = data.has("products") ? data.get("products") : objectMapper.createArrayNode();

                if (!products.isArray() || products.size() == 0) {
                    break;
                }

                for (JsonNode productNode : products) {
                    try {
                        EcommerceProduct product = parseShopifyProduct(productNode, platform);

                        EcommerceProduct existing = productDAO.findByPlatformAndExternalId(
                                platform.getPlatformType(), product.getExternalProductId());

                        if (existing != null) {
                            product.setId(existing.getId());
                            productDAO.update(product);
                            syncLog.setRecordsUpdated(syncLog.getRecordsUpdated() + 1);
                        } else {
                            productDAO.save(product);
                            syncLog.setRecordsInserted(syncLog.getRecordsInserted() + 1);
                        }

                        totalProcessed++;
                    } catch (Exception e) {
                        logger.error("Error processing Shopify product", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                // Check for next page
                if (data.has("pageInfo")) {
                    JsonNode pageInfo = data.get("pageInfo");
                    if (pageInfo.has("hasNextPage") && pageInfo.get("hasNextPage").asBoolean()) {
                        cursor = pageInfo.get("endCursor").asText();
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);

            logger.info("Shopify product sync completed: " + totalProcessed + " products processed");
        } catch (Exception e) {
            logger.error("Error syncing Shopify products", e);
            syncLog.setStatus(EcommerceSyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
        }
    }

    private EcommerceOrder parseShopifyOrder(JsonNode json, EcommercePlatform platform) {
        EcommerceOrder order = new EcommerceOrder();
        order.setPlatformType(platform.getPlatformType());
        order.setEcommercePlatform(platform);
        order.setExternalOrderId("shopify_" + json.get("id").asLong());
        order.setOrderNumber(json.has("name") ? json.get("name").asText() : String.valueOf(json.get("id")));

        if (json.has("created_at")) {
            try {
                order.setOrderDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(
                        json.get("created_at").asText().substring(0, 19)));
            } catch (Exception e) {
                order.setOrderDate(new Date());
            }
        }

        if (json.has("customer")) {
            JsonNode customer = json.get("customer");
            if (customer.has("email")) {
                order.setCustomerEmail(customer.get("email").asText());
            }
            if (customer.has("first_name")) {
                order.setCustomerName(customer.get("first_name").asText() + " " +
                        customer.get("last_name").asText());
            }
        }

        order.setTotalAmount(new BigDecimal(json.get("total_price").asText()));
        order.setCurrency(json.has("currency") ? json.get("currency").asText() : "EUR");
        order.setOrderStatus(json.has("financial_status") ? json.get("financial_status").asText() : "pending");
        order.setLineItems(json.has("line_items") ? json.get("line_items").toString() : null);
        order.setExternalJson(json.toString());

        return order;
    }

    private EcommerceProduct parseShopifyProduct(JsonNode json, EcommercePlatform platform) {
        EcommerceProduct product = new EcommerceProduct();
        product.setPlatformType(platform.getPlatformType());
        product.setEcommercePlatform(platform);
        product.setExternalProductId("shopify_" + json.get("id").asLong());
        product.setProductName(json.has("title") ? json.get("title").asText() : "");
        product.setDescription(json.has("body_html") ? json.get("body_html").asText() : null);

        if (json.has("variants") && json.get("variants").isArray() && json.get("variants").size() > 0) {
            JsonNode variant = json.get("variants").get(0);
            if (variant.has("sku")) {
                product.setSku(variant.get("sku").asText());
            }
            if (variant.has("price")) {
                product.setPrice(new BigDecimal(variant.get("price").asText()));
            }
            if (variant.has("inventory_quantity")) {
                product.setStockQuantity(variant.get("inventory_quantity").asInt());
            }
        }

        product.setCurrency("EUR");

        if (json.has("images")) {
            product.setImages(json.get("images").toString());
        }

        product.setExternalJson(json.toString());

        return product;
    }

    private String extractShopifyStoreName(String storeUrl) {
        if (storeUrl.contains("myshopify.com")) {
            return storeUrl.replaceAll("https://|http://|/", "");
        }
        return storeUrl.replaceAll("https://|http://|/", "");
    }

    private String makeAuthenticatedRequest(String urlString, String accessToken) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("GET");

        conn.setRequestProperty("X-Shopify-Access-Token", accessToken);
        conn.setRequestProperty("Accept", "application/json");

        int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            logger.error("Shopify API error: " + responseCode);
            return null;
        }
    }
}


