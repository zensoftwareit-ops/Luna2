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
 * WooCommerce integration service - handles order and product synchronization
 */
public class WooCommerceService {

    private static final Logger logger = LogManager.getLogger(WooCommerceService.class);
    private static final String API_VERSION = "v3";
    private static final int CONNECT_TIMEOUT = 15000;
    private static final int READ_TIMEOUT = 30000;
    private static final int PAGE_SIZE = 100;

    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private EcommerceSyncLogDAO syncLogDAO;
    private ObjectMapper objectMapper = new ObjectMapper();

    public WooCommerceService() {
        this.orderDAO = new EcommerceOrderDAO();
        this.productDAO = new EcommerceProductDAO();
        this.syncLogDAO = new EcommerceSyncLogDAO();
    }

    public WooCommerceService(EcommerceOrderDAO orderDAO, EcommerceProductDAO productDAO, EcommerceSyncLogDAO syncLogDAO) {
        this.orderDAO = orderDAO;
        this.productDAO = productDAO;
        this.syncLogDAO = syncLogDAO;
    }

    public boolean testConnection(EcommercePlatform platform) {
        try {
            logger.info("Testing WooCommerce connection for: " + platform.getStoreName());
            String url = platform.getStoreUrl() + "/wp-json/wc/" + API_VERSION + "/orders?per_page=1";
            String response = makeAuthenticatedRequest(url, platform.getApiKey(), platform.getApiSecret());
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            logger.error("Error testing WooCommerce connection", e);
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
            logger.info("Synchronizing WooCommerce orders for: " + platform.getStoreName());
            int page = 1;
            int totalProcessed = 0;

            while (true) {
                String url = platform.getStoreUrl() + "/wp-json/wc/" + API_VERSION + "/orders" +
                        "?page=" + page + "&per_page=" + PAGE_SIZE + "&status=any";

                String response = makeAuthenticatedRequest(url, platform.getApiKey(), platform.getApiSecret());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode orders = objectMapper.readTree(response);
                if (!orders.isArray() || orders.size() == 0) {
                    break;
                }

                for (JsonNode orderNode : orders) {
                    try {
                        EcommerceOrder order = parseWooCommerceOrder(orderNode, platform);

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
                        logger.error("Error processing WooCommerce order", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                page++;
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);

            logger.info("WooCommerce order sync completed: " + totalProcessed + " orders processed");
        } catch (Exception e) {
            logger.error("Error syncing WooCommerce orders", e);
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
            logger.info("Synchronizing WooCommerce products for: " + platform.getStoreName());
            int page = 1;
            int totalProcessed = 0;

            while (true) {
                String url = platform.getStoreUrl() + "/wp-json/wc/" + API_VERSION + "/products" +
                        "?page=" + page + "&per_page=" + PAGE_SIZE;

                String response = makeAuthenticatedRequest(url, platform.getApiKey(), platform.getApiSecret());
                if (response == null || response.isEmpty()) {
                    break;
                }

                JsonNode products = objectMapper.readTree(response);
                if (!products.isArray() || products.size() == 0) {
                    break;
                }

                for (JsonNode productNode : products) {
                    try {
                        EcommerceProduct product = parseWooCommerceProduct(productNode, platform);

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
                        logger.error("Error processing WooCommerce product", e);
                        syncLog.setRecordsFailed(syncLog.getRecordsFailed() + 1);
                    }
                }

                page++;
            }

            syncLog.setRecordsProcessed(totalProcessed);
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);

            logger.info("WooCommerce product sync completed: " + totalProcessed + " products processed");
        } catch (Exception e) {
            logger.error("Error syncing WooCommerce products", e);
            syncLog.setStatus(EcommerceSyncLog.SyncStatus.FAILED);
            syncLog.setErrorMessage(e.getMessage());
            syncLog.setEndTime(new Date());
            syncLogDAO.save(syncLog);
        }
    }

    private EcommerceOrder parseWooCommerceOrder(JsonNode json, EcommercePlatform platform) {
        EcommerceOrder order = new EcommerceOrder();
        order.setPlatformType(platform.getPlatformType());
        order.setEcommercePlatform(platform);
        order.setExternalOrderId("wc_" + json.get("id").asLong());
        order.setOrderNumber(json.has("number") ? json.get("number").asText() : String.valueOf(json.get("id")));

        if (json.has("date_created")) {
            try {
                order.setOrderDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(
                        json.get("date_created").asText().substring(0, 19)));
            } catch (Exception e) {
                order.setOrderDate(new Date());
            }
        }

        if (json.has("billing")) {
            JsonNode billing = json.get("billing");
            order.setCustomerEmail(billing.has("email") ? billing.get("email").asText() : null);
            order.setCustomerName(billing.has("first_name") ?
                    billing.get("first_name").asText() + " " +
                    billing.get("last_name").asText() : null);
        }

        order.setTotalAmount(new BigDecimal(json.get("total").asText()));
        order.setCurrency(json.has("currency") ? json.get("currency").asText() : "EUR");
        order.setOrderStatus(json.has("status") ? json.get("status").asText() : "pending");
        order.setLineItems(json.has("line_items") ? json.get("line_items").toString() : null);
        order.setExternalJson(json.toString());

        return order;
    }

    private EcommerceProduct parseWooCommerceProduct(JsonNode json, EcommercePlatform platform) {
        EcommerceProduct product = new EcommerceProduct();
        product.setPlatformType(platform.getPlatformType());
        product.setEcommercePlatform(platform);
        product.setExternalProductId("wc_" + json.get("id").asLong());
        product.setProductName(json.has("name") ? json.get("name").asText() : "");
        product.setSku(json.has("sku") ? json.get("sku").asText() : null);
        product.setDescription(json.has("description") ? json.get("description").asText() : null);
        product.setPrice(new BigDecimal(json.has("price") ? json.get("price").asText() : "0"));
        product.setCurrency("EUR");
        product.setStockQuantity(json.has("stock_quantity") ? json.get("stock_quantity").asInt() : 0);

        if (json.has("images")) {
            product.setImages(json.get("images").toString());
        }

        product.setExternalJson(json.toString());

        return product;
    }

    private String makeAuthenticatedRequest(String urlString, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(CONNECT_TIMEOUT);
        conn.setReadTimeout(READ_TIMEOUT);
        conn.setRequestMethod("GET");

        String auth = apiKey + ":" + apiSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        conn.setRequestProperty("Authorization", "Basic " + encodedAuth);
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
            logger.error("WooCommerce API error: " + responseCode);
            return null;
        }
    }
}


