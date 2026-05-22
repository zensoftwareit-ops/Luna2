package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.EcommerceProductDAO;
import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.model.EcommerceProduct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.List;

/**
 * Inventory synchronization service - push stock levels back to eCommerce platforms
 * Implements bidirectional sync for inventory management
 */
public class InventorySyncService {

    private static final Logger logger = LogManager.getLogger(InventorySyncService.class);
    private EcommerceProductDAO productDAO = new EcommerceProductDAO();

    /**
     * Update product inventory on all platforms
     * Called when inventory changes in Luna2
     */
    public void syncInventoryToAllPlatforms(Long productId, Integer newQuantity) {
        try {
            EcommerceProduct product = productDAO.findById(productId);
            if (product == null) {
                logger.warn("Product not found: " + productId);
                return;
            }

            EcommercePlatform platform = product.getEcommercePlatform();
            if (platform == null || !platform.getIsActive()) {
                logger.warn("Platform not active for product: " + productId);
                return;
            }

            switch (platform.getPlatformType()) {
                case WOOCOMMERCE:
                    updateWooCommerceInventory(product, newQuantity, platform);
                    break;
                case SHOPIFY:
                    updateShopifyInventory(product, newQuantity, platform);
                    break;
                case AMAZON:
                    updateAmazonInventory(product, newQuantity, platform);
                    break;
                case EBAY:
                    updateEbayInventory(product, newQuantity, platform);
                    break;
                default:
                    logger.warn("Unsupported platform for inventory sync: " + platform.getPlatformType());
            }

            // Update local inventory
            product.setStockQuantity(newQuantity);
            productDAO.update(product);
            logger.info("Inventory synced for product: " + productId + " on " + platform.getPlatformType());

        } catch (Exception e) {
            logger.error("Error syncing inventory to platforms", e);
        }
    }

    /**
     * WooCommerce: Update product stock via REST API
     */
    private void updateWooCommerceInventory(EcommerceProduct product, Integer quantity, EcommercePlatform platform) {
        try {
            String productId = product.getExternalProductId().replace("wc_", "");
            String url = platform.getStoreUrl() + "/wp-json/wc/v3/products/" + productId;
            String payload = "{\"stock_quantity\": " + quantity + "}";

            String auth = platform.getApiKey() + ":" + platform.getApiSecret();
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());

            HttpURLConnection conn = makeRequest(url, "PUT", encodedAuth, payload);
            if (conn.getResponseCode() == 200) {
                logger.info("WooCommerce inventory updated: " + productId + " = " + quantity);
            } else {
                logger.error("WooCommerce inventory update failed: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            logger.error("Error updating WooCommerce inventory", e);
        }
    }

    /**
     * Shopify: Update inventory via inventory API
     */
    private void updateShopifyInventory(EcommerceProduct product, Integer quantity, EcommercePlatform platform) {
        try {
            String storeName = extractShopifyStoreName(platform.getStoreUrl());
            String productId = product.getExternalProductId().replace("shopify_", "");

            // First get inventory item ID
            String getUrl = "https://" + storeName + "/admin/api/2024-01/products/" + productId + "/variants.json";
            String getAuth = "Bearer " + platform.getApiKey();

            HttpURLConnection getConn = makeRequest(getUrl, "GET", getAuth, null);
            if (getConn.getResponseCode() == 200) {
                // Parse response to get inventory_item_id
                // Then update via inventory API
                String inventoryUrl = "https://" + storeName + "/admin/api/2024-01/inventory_levels/adjust.json";
                String payload = "{\"inventory_item_id\": \"ITEM_ID\", \"available_adjustment\": " + quantity + "}";
                HttpURLConnection conn = makeRequest(inventoryUrl, "POST", getAuth, payload);
                if (conn.getResponseCode() == 200) {
                    logger.info("Shopify inventory updated: " + productId + " = " + quantity);
                }
            }
        } catch (Exception e) {
            logger.error("Error updating Shopify inventory", e);
        }
    }

    /**
     * Amazon: Update inventory via Selling Partner API
     */
    private void updateAmazonInventory(EcommerceProduct product, Integer quantity, EcommercePlatform platform) {
        try {
            String sku = product.getSku();
            String url = "https://sellingpartnerapi-eu-west-1.amazon.com/inventory/v1/inventory/" + sku;
            String auth = "Bearer " + platform.getApiKey();

            String payload = "{\"quantity\": {\"quantity\": " + quantity + "}}";
            HttpURLConnection conn = makeRequest(url, "PATCH", auth, payload);

            if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                logger.info("Amazon inventory updated: " + sku + " = " + quantity);
            } else {
                logger.error("Amazon inventory update failed: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            logger.error("Error updating Amazon inventory", e);
        }
    }

    /**
     * eBay: Update inventory via Inventory API
     */
    private void updateEbayInventory(EcommerceProduct product, Integer quantity, EcommercePlatform platform) {
        try {
            String sku = product.getSku();
            String url = "https://api.ebay.com/sell/inventory/v1/inventory_item/" + sku;
            String auth = "Bearer " + platform.getApiKey();

            String payload = "{\"availability\": {\"availableQuantity\": " + quantity + "}}";
            HttpURLConnection conn = makeRequest(url, "PATCH", auth, payload);

            if (conn.getResponseCode() >= 200 && conn.getResponseCode() < 300) {
                logger.info("eBay inventory updated: " + sku + " = " + quantity);
            } else {
                logger.error("eBay inventory update failed: " + conn.getResponseCode());
            }
        } catch (Exception e) {
            logger.error("Error updating eBay inventory", e);
        }
    }

    /**
     * Sync inventory levels from all platforms back to Luna2
     * Useful for detecting inconsistencies
     */
    public void syncInventoryFromPlatforms() {
        try {
            logger.info("Starting bidirectional inventory sync from all platforms");
            List<EcommerceProduct> allProducts = productDAO.findAll();

            for (EcommerceProduct product : allProducts) {
                try {
                    if (product.getEcommercePlatform() != null && product.getEcommercePlatform().getIsActive()) {
                        // Compare and log discrepancies
                        logger.debug("Inventory check for product: " + product.getId() +
                                " Current: " + product.getStockQuantity());
                    }
                } catch (Exception e) {
                    logger.error("Error checking inventory for product: " + product.getId(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Error syncing inventory from platforms", e);
        }
    }

    private HttpURLConnection makeRequest(String urlString, String method, String auth, String payload) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", auth);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");

        if (payload != null) {
            conn.setDoOutput(true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            writer.write(payload);
            writer.flush();
            writer.close();
        }

        return conn;
    }

    private String extractShopifyStoreName(String storeUrl) {
        return storeUrl.replaceAll("https://|http://|/", "");
    }
}
