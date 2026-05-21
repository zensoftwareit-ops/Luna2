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

    private EcommercePlatformDAO platformDAO = new EcommercePlatformDAO();
    private EcommerceOrderDAO orderDAO = new EcommerceOrderDAO();
    private EcommerceProductDAO productDAO = new EcommerceProductDAO();
    private EcommercePlatformService platformService = new EcommercePlatformService();

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
            platforms = platformDAO.findAll();

            int offset = (currentPage - 1) * pageSize;
            orders = orderDAO.findAll();
            if (offset < orders.size()) {
                orders = orders.subList(offset, Math.min(offset + pageSize, orders.size()));
            } else {
                orders.clear();
            }

            products = productDAO.findAll();
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
            platforms = platformDAO.findAll();

            if (platform != null && !platform.isEmpty()) {
                try {
                    EcommercePlatform.PlatformType platformType = EcommercePlatform.PlatformType.valueOf(platform.toUpperCase());
                    orders = orderDAO.findByPlatformType(platformType);
                } catch (IllegalArgumentException e) {
                    orders = orderDAO.findAll();
                }
            } else {
                orders = orderDAO.findAll();
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
            platforms = platformDAO.findAll();

            if (search != null && !search.isEmpty()) {
                products = productDAO.findByName(search);
            } else if (platform != null && !platform.isEmpty()) {
                try {
                    EcommercePlatform.PlatformType platformType = EcommercePlatform.PlatformType.valueOf(platform.toUpperCase());
                    products = productDAO.findByPlatformType(platformType);
                } catch (IllegalArgumentException e) {
                    products = productDAO.findAll();
                }
            } else {
                products = productDAO.findAll();
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
            platformService.syncAllPlatforms();
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
