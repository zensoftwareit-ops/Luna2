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
import java.util.Base64;
import java.util.Date;

/**
 * Amazon Selling Partner API integration - orders and products synchronization
 */
public class AmazonService {

    private static final Logger logger = LogManager.getLogger(AmazonService.class);
    private static final String API_VERSION = "2021-09-01";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;

    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private EcommerceSyncLogDAO syncLogDAO;
    private ObjectMapper objectMapper = new ObjectMapper();

    public AmazonService() {
        this.orderDAO = new EcommerceOrderDAO();
        this.productDAO = new EcommerceProductDAO();
        this.syncLogDAO = new EcommerceSyncLogDAO();
    }

    public AmazonService(EcommerceOrderDAO orderDAO, EcommerceProductDAO productDAO, EcommerceSyncLogDAO syncLogDAO) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.syncLogDAO = syncLogDAO;
    }

    public boolean testConnection(EcommercePlatform platform) {
        try {
            logger.info("Testing Amazon connection for: " + platform.getStoreName());
            // Test with a simple GetOrders call
            String url = "https://sellingpartnerapi-" + extractRegion(platform.getStoreUrl()) +
                    ".amazon.com/orders/" + API_VERSION + "/orders?marketplaceIds=YOUR_MARKETPLACE_ID&limit=1";
            String response = makeAuthenticatedRequest(url, "GET", platform.getApiKey());
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.error("Error testing Amazon connection", e);
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
            logger.info("Synchronizing Amazon orders for: " + platform.getStoreName());
            int totalProcessed = 0;
            String nextToken = null;

            while (true) {
                String url = "https://sellingpartnerapi-" + extractRegion(platform.getStoreUrl()) +
                        ".amazon.com/orders/" + API_VERSION + "/orders?marketplaceIds=YOUR_MARKETPLACE_ID&limit=50";

                if (nextToken != null) {
                    url += "&NextToken=" + nextToken;
                }

                String response = makeAuthenticatedRequest(url, "GET", platform.getApiKey());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode data = objectMapper.readTree(response);
                JsonNode orders = data.has("Orders") ? data.get("Orders") : objectMapper.createArrayNode();

                if (!orders.isArray() || orders.size() == 0) {
                    break;
                }

                for (JsonNode orderNode : orders) {
                    try {
                        EcommerceOrder order = parseAmazonOrder(orderNode, platform);
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
                        logger.error("Error processing Amazon order", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                if (data.has("NextToken")) {
                    nextToken = data.get("NextToken").asText();
                } else {
                    break;
                }
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
            logger.info("Amazon order sync completed: " + totalProcessed + " orders processed");
        } catch (Exception e) {
            logger.error("Error syncing Amazon orders", e);
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
            logger.info("Synchronizing Amazon products for: " + platform.getStoreName());
            int totalProcessed = 0;
            String nextToken = null;

            while (true) {
                String url = "https://sellingpartnerapi-" + extractRegion(platform.getStoreUrl()) +
                        ".amazon.com/catalog/2022-04-01/items?limit=20";

                if (nextToken != null) {
                    url += "&pageToken=" + nextToken;
                }

                String response = makeAuthenticatedRequest(url, "GET", platform.getApiKey());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode data = objectMapper.readTree(response);
                JsonNode items = data.has("items") ? data.get("items") : objectMapper.createArrayNode();

                if (!items.isArray() || items.size() == 0) {
                    break;
                }

                for (JsonNode itemNode : items) {
                    try {
                        EcommerceProduct product = parseAmazonProduct(itemNode, platform);
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
                        logger.error("Error processing Amazon product", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                if (data.has("pagination") && data.get("pagination").has("nextPageToken")) {
                    nextToken = data.get("pagination").get("nextPageToken").asText();
                } else {
                    break;
                }
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
            logger.info("Amazon product sync completed: " + totalProcessed + " products processed");
        } catch (Exception e) {
            logger.error("Error syncing Amazon products", e);
            syncLog.setStatus(EcommerceSyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
        }
    }

    public void updateInventory(Long productId, Integer quantity) {
        try {
            logger.info("Updating inventory for Amazon product: " + productId + " to " + quantity);
            // TODO: Implement bidirectional inventory sync to Amazon
        } catch (Exception e) {
            logger.error("Error updating Amazon inventory", e);
        }
    }

    private EcommerceOrder parseAmazonOrder(JsonNode json, EcommercePlatform platform) {
        EcommerceOrder order = new EcommerceOrder();
        order.setPlatformType(platform.getPlatformType());
        order.setEcommercePlatform(platform);
        order.setExternalOrderId("amz_" + json.get("AmazonOrderId").asText());
        order.setOrderNumber(json.get("AmazonOrderId").asText());

        if (json.has("PurchaseDate")) {
            try {
                order.setOrderDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(
                        json.get("PurchaseDate").asText().substring(0, 19)));
            } catch (Exception e) {
                order.setOrderDate(new Date());
            }
        }

        order.setCustomerEmail(json.has("BuyerEmail") ? json.get("BuyerEmail").asText() : null);
        order.setCustomerName(json.has("BuyerName") ? json.get("BuyerName").asText() : null);
        order.setTotalAmount(new BigDecimal(json.has("OrderTotal") ?
                json.get("OrderTotal").get("Amount").asText() : "0"));
        order.setCurrency(json.has("OrderTotal") ? json.get("OrderTotal").get("CurrencyCode").asText() : "EUR");
        order.setOrderStatus(json.has("OrderStatus") ? json.get("OrderStatus").asText() : "pending");
        order.setExternalJson(json.toString());

        return order;
    }

    private EcommerceProduct parseAmazonProduct(JsonNode json, EcommercePlatform platform) {
        EcommerceProduct product = new EcommerceProduct();
        product.setPlatformType(platform.getPlatformType());
        product.setEcommercePlatform(platform);
        product.setExternalProductId("amz_" + json.get("asin").asText());
        product.setProductName(json.has("title") ? json.get("title").asText() : "");
        product.setSku(json.has("sku") ? json.get("sku").asText() : null);

        if (json.has("attributes")) {
            JsonNode attrs = json.get("attributes");
            if (attrs.has("description")) {
                product.setDescription(attrs.get("description").asText());
            }
        }

        product.setPrice(new BigDecimal("0"));
        product.setCurrency("EUR");
        product.setStockQuantity(0);
        product.setExternalJson(json.toString());

        return product;
    }

    private String extractRegion(String storeUrl) {
        // Map marketplace to region (simplified)
        return "eu-west-1"; // Default to EU
    }

    private String makeAuthenticatedRequest(String urlString, String method, String accessToken) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod(method);
        conn.setRequestProperty("x-amzn-Authorization", "Bearer " + accessToken);
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
            logger.error("Amazon API error: " + responseCode);
            return null;
        }
    }
}
