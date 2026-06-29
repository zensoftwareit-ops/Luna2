package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.EcommercePlatformDAO;
import it.zensoftware.luna2.dao.EcommerceOrderDAO;
import it.zensoftware.luna2.dao.EcommerceProductDAO;
import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.EcommerceProduct;
import it.zensoftware.luna2.service.EcommercePlatformService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import java.util.Map;

/**
 * Action for eCommerce centralization dashboard and operations
 */
public class EcommerceAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(EcommerceAction.class);

    private EcommercePlatformDAO platformDAO;
    private EcommerceOrderDAO orderDAO;
    private EcommerceProductDAO productDAO;
    private EcommercePlatformService platformService;

    private EcommercePlatformDAO getPlatformDAO() {
        if (platformDAO == null) platformDAO = new EcommercePlatformDAO();
        return platformDAO;
    }
    private EcommerceOrderDAO getOrderDAO() {
        if (orderDAO == null) orderDAO = new EcommerceOrderDAO();
        return orderDAO;
    }
    private EcommerceProductDAO getProductDAO() {
        if (productDAO == null) productDAO = new EcommerceProductDAO();
        return productDAO;
    }
    private EcommercePlatformService getPlatformService() {
        if (platformService == null) platformService = new EcommercePlatformService();
        return platformService;
    }

    private List<EcommercePlatform> platforms;
    private List<EcommerceOrder> orders;
    private List<EcommerceProduct> products;
    private Map<String, Object> response;

    private Integer currentPage = 1;
    private Integer pageSize = 20;
    private String platform;
    private String status;
    private String search;

    public String dashboard() {
        try {
            logger.info("Loading eCommerce dashboard");
            platforms = getPlatformDAO().findAll();

            int offset = (currentPage - 1) * pageSize;
            orders = getOrderDAO().findAll();
            if (offset < orders.size()) {
                orders = orders.subList(offset, Math.min(offset + pageSize, orders.size()));
            } else {
                orders.clear();
            }

            products = getProductDAO().findAll();
            if (offset < products.size()) {
                products = products.subList(offset, Math.min(offset + pageSize, products.size()));
            } else {
                products.clear();
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading eCommerce dashboard", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String orders() {
        try {
            logger.info("Loading eCommerce orders");
            platforms = getPlatformDAO().findAll();

            if (platform != null && !platform.isEmpty()) {
                try {
                    EcommercePlatform.PlatformType platformType = EcommercePlatform.PlatformType.valueOf(platform.toUpperCase());
                    orders = getOrderDAO().findByPlatformType(platformType);
                } catch (IllegalArgumentException e) {
                    orders = getOrderDAO().findAll();
                }
            } else {
                orders = getOrderDAO().findAll();
            }

            if (status != null && !status.isEmpty()) {
                orders.removeIf(o -> !status.equals(o.getOrderStatus()));
            }

            int offset = (currentPage - 1) * pageSize;
            if (offset < orders.size()) {
                orders = orders.subList(offset, Math.min(offset + pageSize, orders.size()));
            } else {
                orders.clear();
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading eCommerce orders", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String products() {
        try {
            logger.info("Loading eCommerce products");
            platforms = getPlatformDAO().findAll();

            if (search != null && !search.isEmpty()) {
                products = getProductDAO().findByName(search);
            } else if (platform != null && !platform.isEmpty()) {
                try {
                    EcommercePlatform.PlatformType platformType = EcommercePlatform.PlatformType.valueOf(platform.toUpperCase());
                    products = getProductDAO().findByPlatformType(platformType);
                } catch (IllegalArgumentException e) {
                    products = getProductDAO().findAll();
                }
            } else {
                products = getProductDAO().findAll();
            }

            int offset = (currentPage - 1) * pageSize;
            if (offset < products.size()) {
                products = products.subList(offset, Math.min(offset + pageSize, products.size()));
            } else {
                products.clear();
            }

            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading eCommerce products", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String syncNow() {
        response = new java.util.HashMap<>();
        try {
            logger.info("Triggering manual sync");
            getPlatformService().syncAllPlatforms();
            response.put("success", true);
            response.put("message", "Sincronizzazione avviata");
            return "json";
        } catch (Exception e) {
            logger.error("Error triggering sync", e);
            response.put("success", false);
            response.put("message", "Errore: " + e.getMessage());
            return "json";
        }
    }

    // Getters and Setters
    public List<EcommercePlatform> getPlatforms() {
        return platforms;
    }

    public List<EcommerceOrder> getOrders() {
        return orders;
    }

    public List<EcommerceProduct> getProducts() {
        return products;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    public Integer getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(Integer currentPage) {
        this.currentPage = currentPage;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}
