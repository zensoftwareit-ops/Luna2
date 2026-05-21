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
 * eBay REST API integration - orders and inventory synchronization
 */
public class EbayService {

    private static final Logger logger = LogManager.getLogger(EbayService.class);
    private static final String API_VERSION = "v1";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private EcommerceSyncLogDAO syncLogDAO;
    private ObjectMapper objectMapper = new ObjectMapper();

    public EbayService() {
        this.orderDAO = new EcommerceOrderDAO();
        this.productDAO = new EcommerceProductDAO();
        this.syncLogDAO = new EcommerceSyncLogDAO();
    }

    public EbayService(EcommerceOrderDAO orderDAO, EcommerceProductDAO productDAO, EcommerceSyncLogDAO syncLogDAO) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.syncLogDAO = syncLogDAO;
    }

    public boolean testConnection(EcommercePlatform platform) {
        try {
            logger.info("Testing eBay connection for: " + platform.getStoreName());
            String url = "https://api.ebay.com/sell/fulfillment/" + API_VERSION + "/order?limit=1";
            String response = makeAuthenticatedRequest(url, "GET", platform.getApiKey());
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.error("Error testing eBay connection", e);
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
            logger.info("Synchronizing eBay orders for: " + platform.getStoreName());
            int totalProcessed = 0;
            int offset = 0;
            int limit = 50;

            while (true) {
                String url = "https://api.ebay.com/sell/fulfillment/" + API_VERSION + "/order?limit=" + limit + "&offset=" + offset;

                String response = makeAuthenticatedRequest(url, "GET", platform.getApiKey());
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
                        EcommerceOrder order = parseEbayOrder(orderNode, platform);
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
                        logger.error("Error processing eBay order", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                if (orders.size() < limit) {
                    break;
                }
                offset += limit;
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
            logger.info("eBay order sync completed: " + totalProcessed + " orders processed");
        } catch (Exception e) {
            logger.error("Error syncing eBay orders", e);
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
            logger.info("Synchronizing eBay products for: " + platform.getStoreName());
            int totalProcessed = 0;
            int offset = 0;
            int limit = 50;

            while (true) {
                String url = "https://api.ebay.com/sell/inventory/" + API_VERSION + "/inventory_item?limit=" + limit + "&offset=" + offset;

                String response = makeAuthenticatedRequest(url, "GET", platform.getApiKey());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode data = objectMapper.readTree(response);
                JsonNode items = data.has("inventory_items") ? data.get("inventory_items") : objectMapper.createArrayNode();

                if (!items.isArray() || items.size() == 0) {
                    break;
                }

                for (JsonNode itemNode : items) {
                    try {
                        EcommerceProduct product = parseEbayProduct(itemNode, platform);
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
                        logger.error("Error processing eBay product", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                if (items.size() < limit) {
                    break;
                }
                offset += limit;
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
            logger.info("eBay product sync completed: " + totalProcessed + " products processed");
        } catch (Exception e) {
            logger.error("Error syncing eBay products", e);
            syncLog.setStatus(EcommerceSyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
        }
    }

    public void updateInventory(String sku, Integer quantity) {
        try {
            logger.info("Updating eBay inventory for SKU: " + sku + " to " + quantity);
            // Implementation for bidirectional inventory sync
            String url = "https://api.ebay.com/sell/inventory/" + API_VERSION + "/inventory_item/" + sku;
            String payload = "{\"availability\": {\"availableQuantity\": " + quantity + "}}";
            makeAuthenticatedRequest(url, "PATCH", null, payload);
        } catch (Exception e) {
            logger.error("Error updating eBay inventory", e);
        }
    }

    private EcommerceOrder parseEbayOrder(JsonNode json, EcommercePlatform platform) {
        EcommerceOrder order = new EcommerceOrder();
        order.setPlatformType(platform.getPlatformType());
        order.setEcommercePlatform(platform);
        order.setExternalOrderId("ebay_" + json.get("orderId").asText());
        order.setOrderNumber(json.get("orderId").asText());

        if (json.has("creationDate")) {
            try {
                order.setOrderDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(
                        json.get("creationDate").asText().substring(0, 19)));
            } catch (Exception e) {
                order.setOrderDate(new Date());
            }
        }

        if (json.has("buyer")) {
            JsonNode buyer = json.get("buyer");
            order.setCustomerEmail(buyer.has("email") ? buyer.get("email").asText() : null);
            order.setCustomerName(buyer.has("username") ? buyer.get("username").asText() : null);
        }

        if (json.has("pricingSummary")) {
            JsonNode pricing = json.get("pricingSummary");
            order.setTotalAmount(new BigDecimal(pricing.has("total") ? pricing.get("total").asText() : "0"));
            order.setCurrency(pricing.has("currency") ? pricing.get("currency").asText() : "EUR");
        }

        order.setOrderStatus(json.has("orderStatus") ? json.get("orderStatus").asText() : "pending");
        order.setExternalJson(json.toString());

        return order;
    }

    private EcommerceProduct parseEbayProduct(JsonNode json, EcommercePlatform platform) {
        EcommerceProduct product = new EcommerceProduct();
        product.setPlatformType(platform.getPlatformType());
        product.setEcommercePlatform(platform);
        product.setExternalProductId("ebay_" + json.get("sku").asText());
        product.setProductName(json.has("title") ? json.get("title").asText() : "");
        product.setSku(json.get("sku").asText());
        product.setDescription(json.has("description") ? json.get("description").asText() : null);

        if (json.has("price")) {
            product.setPrice(new BigDecimal(json.get("price").asText()));
        } else {
            product.setPrice(new BigDecimal("0"));
        }

        product.setCurrency(json.has("currency") ? json.get("currency").asText() : "EUR");

        if (json.has("availability")) {
            JsonNode avail = json.get("availability");
            product.setStockQuantity(avail.has("availableQuantity") ? avail.get("availableQuantity").asInt() : 0);
        }

        product.setExternalJson(json.toString());

        return product;
    }

    private String makeAuthenticatedRequest(String urlString, String method, String accessToken) throws Exception {
        return makeAuthenticatedRequest(urlString, method, accessToken, null);
    }

    private String makeAuthenticatedRequest(String urlString, String method, String accessToken, String payload) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod(method);

        if (accessToken != null) {
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        }

        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");

        if (payload != null) {
            conn.setDoOutput(true);
            conn.getOutputStream().write(payload.getBytes());
        }

        int responseCode = conn.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            return response.toString();
        } else {
            logger.error("eBay API error: " + responseCode);
            return null;
        }
    }
}
