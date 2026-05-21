package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.EcommercePlatformDAO;
import it.zensoftware.luna2.dao.EcommerceOrderDAO;
import it.zensoftware.luna2.dao.EcommerceProductDAO;
import it.zensoftware.luna2.dao.EcommerceSyncLogDAO;
import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.EcommerceProduct;
import it.zensoftware.luna2.model.EcommerceSyncLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import java.util.Date;

/**
 * Service for orchestrating eCommerce platform operations and sync
 */
public class EcommercePlatformService {

    private static final Logger logger = LogManager.getLogger(EcommercePlatformService.class);

    private EcommercePlatformDAO platformDAO;
    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private EcommerceSyncLogDAO syncLogDAO;

    public EcommercePlatformService() {
        this.platformDAO = new EcommercePlatformDAO();
        this.orderDAO = new EcommerceOrderDAO();
        this.productDAO = new EcommerceProductDAO();
        this.syncLogDAO = new EcommerceSyncLogDAO();
    }

    /**
     * Configure a new eCommerce platform
     */
    public EcommercePlatform configurePlatform(EcommercePlatform platform) {
        try {
            platformDAO.save(platform);
            logger.info("Platform configured: " + platform.getPlatformType() + " - " + platform.getStoreName());
            return platform;
        } catch (Exception e) {
            logger.error("Error configuring platform", e);
            throw new RuntimeException("Error configuring platform: " + e.getMessage());
        }
    }

    /**
     * Test connection to a platform
     */
    public boolean testConnection(Long platformId) {
        try {
            EcommercePlatform platform = platformDAO.findById(platformId);
            if (platform == null) {
                logger.warn("Platform not found: " + platformId);
                return false;
            }

            switch (platform.getPlatformType()) {
                case WOOCOMMERCE:
                    WooCommerceService wooService = new WooCommerceService();
                    return wooService.testConnection(platform);
                case SHOPIFY:
                    ShopifyService shopifyService = new ShopifyService();
                    return shopifyService.testConnection(platform);
                default:
                    logger.warn("Unsupported platform type: " + platform.getPlatformType());
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            return false;
        }
    }

    /**
     * Remove a platform and optionally cascade delete its data
     */
    public void removePlatform(Long platformId) {
        try {
            EcommercePlatform platform = platformDAO.findById(platformId);
            if (platform != null) {
                // Delete all orders from this platform
                List<EcommerceOrder> orders = orderDAO.findByPlatform(platformId);
                for (EcommerceOrder order : orders) {
                    orderDAO.delete(order);
                }

                // Delete all products from this platform
                List<EcommerceProduct> products = productDAO.findByPlatform(platformId);
                for (EcommerceProduct product : products) {
                    productDAO.delete(product);
                }

                // Delete all sync logs for this platform
                platformDAO.delete(platform);
                logger.info("Platform removed: " + platform.getPlatformType());
            }
        } catch (Exception e) {
            logger.error("Error removing platform", e);
            throw new RuntimeException("Error removing platform: " + e.getMessage());
        }
    }

    /**
     * Get all configured platforms
     */
    public List<EcommercePlatform> getConfiguredPlatforms() {
        return platformDAO.findAll();
    }

    /**
     * Get all active platforms
     */
    public List<EcommercePlatform> getActivePlatforms() {
        return platformDAO.findActive();
    }

    /**
     * Sync all active platforms
     */
    public void syncAllPlatforms() {
        logger.info("Starting sync for all active platforms");
        List<EcommercePlatform> platforms = getActivePlatforms();

        for (EcommercePlatform platform : platforms) {
            try {
                syncPlatform(platform.getId());
            } catch (Exception e) {
                logger.error("Error syncing platform " + platform.getPlatformType(), e);
            }
        }

        logger.info("Sync completed for all platforms");
    }

    /**
     * Sync a single platform
     */
    public void syncPlatform(Long platformId) {
        EcommercePlatform platform = platformDAO.findById(platformId);
        if (platform == null) {
            logger.warn("Platform not found: " + platformId);
            return;
        }

        logger.info("Starting sync for platform: " + platform.getPlatformType());

        try {
            switch (platform.getPlatformType()) {
                case WOOCOMMERCE:
                    syncWooCommerce(platform);
                    break;
                case SHOPIFY:
                    syncShopify(platform);
                    break;
                case AMAZON:
                    syncAmazon(platform);
                    break;
                case EBAY:
                    syncEbay(platform);
                    break;
                default:
                    logger.warn("Unsupported platform type: " + platform.getPlatformType());
            }
        } catch (Exception e) {
            logger.error("Error syncing platform " + platform.getPlatformType(), e);
        }
    }

    /**
     * Sync orders and products from WooCommerce
     */
    private void syncWooCommerce(EcommercePlatform platform) {
        try {
            WooCommerceService service = new WooCommerceService(orderDAO, productDAO, syncLogDAO);
            service.synchronizeOrders(platform);
            service.synchronizeProducts(platform);
            platform.setLastSync(new Date());
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.SUCCESS);
            platformDAO.update(platform);
        } catch (Exception e) {
            logger.error("Error syncing WooCommerce", e);
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.FAILED);
            platformDAO.update(platform);
        }
    }

    /**
     * Sync orders and products from Shopify
     */
    private void syncShopify(EcommercePlatform platform) {
        try {
            ShopifyService service = new ShopifyService(orderDAO, productDAO, syncLogDAO);
            service.synchronizeOrders(platform);
            service.synchronizeProducts(platform);
            platform.setLastSync(new Date());
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.SUCCESS);
            platformDAO.update(platform);
        } catch (Exception e) {
            logger.error("Error syncing Shopify", e);
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.FAILED);
            platformDAO.update(platform);
        }
    }

    /**
     * Sync orders and products from Amazon
     */
    private void syncAmazon(EcommercePlatform platform) {
        try {
            AmazonService service = new AmazonService(orderDAO, productDAO, syncLogDAO);
            service.synchronizeOrders(platform);
            service.synchronizeProducts(platform);
            platform.setLastSync(new Date());
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.SUCCESS);
            platformDAO.update(platform);
        } catch (Exception e) {
            logger.error("Error syncing Amazon", e);
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.FAILED);
            platformDAO.update(platform);
        }
    }

    /**
     * Sync orders and inventory from eBay
     */
    private void syncEbay(EcommercePlatform platform) {
        try {
            EbayService service = new EbayService(orderDAO, productDAO, syncLogDAO);
            service.synchronizeOrders(platform);
            service.synchronizeProducts(platform);
            platform.setLastSync(new Date());
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.SUCCESS);
            platformDAO.update(platform);
        } catch (Exception e) {
            logger.error("Error syncing eBay", e);
            platform.setLastSyncStatus(EcommercePlatform.SyncStatus.FAILED);
            platformDAO.update(platform);
        }
    }

    /**
     * Get sync logs for a platform
     */
    public List<EcommerceSyncLog> getSyncLogs(Long platformId) {
        return syncLogDAO.findSyncsByPlatformAndType(platformId, null);
    }

    /**
     * Get latest sync status for a platform
     */
    public EcommerceSyncLog getLatestSyncLog(Long platformId) {
        return syncLogDAO.findLatestSyncByPlatform(platformId);
    }
}
