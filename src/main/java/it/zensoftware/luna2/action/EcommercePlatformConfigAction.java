package it.zensoftware.luna2.action;

import com.opensymphony.xwork2.ActionSupport;
import it.zensoftware.luna2.dao.EcommercePlatformDAO;
import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.service.EcommercePlatformService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.List;
import java.util.Map;

/**
 * Action for managing eCommerce platform configuration
 */
public class EcommercePlatformConfigAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(EcommercePlatformConfigAction.class);

    private EcommercePlatformDAO platformDAO = new EcommercePlatformDAO();
    private EcommercePlatformService platformService = new EcommercePlatformService();

    private List<EcommercePlatform> platforms;
    private EcommercePlatform platform;
    private Map<String, Object> response;

    private Long id;
    private String platformType;
    private String storeName;
    private String storeUrl;
    private String apiKey;
    private String apiSecret;
    private Boolean isActive = true;
    private Integer syncFrequencyMinutes = 15;

    public String list() {
        try {
            logger.info("Loading eCommerce platforms");
            platforms = platformDAO.findAll();
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading platforms", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String edit() {
        try {
            if (id != null) {
                platform = platformDAO.findById(id);
                if (platform == null) {
                    addActionError("Piattaforma non trovata");
                    return ERROR;
                }
            } else {
                platform = new EcommercePlatform();
            }
            return SUCCESS;
        } catch (Exception e) {
            logger.error("Error loading platform details", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String save() {
        try {
            if (platformType == null || platformType.isEmpty()) {
                addActionError("Tipo piattaforma obbligatorio");
                return ERROR;
            }

            if (storeName == null || storeName.isEmpty()) {
                addActionError("Nome negozio obbligatorio");
                return ERROR;
            }

            if (apiKey == null || apiKey.isEmpty()) {
                addActionError("API Key obbligatoria");
                return ERROR;
            }

            if (id != null) {
                platform = platformDAO.findById(id);
            } else {
                platform = new EcommercePlatform();
            }

            platform.setPlatformType(EcommercePlatform.PlatformType.valueOf(platformType.toUpperCase()));
            platform.setStoreName(storeName);
            platform.setStoreUrl(storeUrl);
            platform.setApiKey(apiKey);
            platform.setApiSecret(apiSecret);
            platform.setIsActive(isActive);
            platform.setSyncFrequencyMinutes(syncFrequencyMinutes);

            if (id == null) {
                platformDAO.save(platform);
                addActionMessage("Piattaforma aggiunta con successo");
            } else {
                platformDAO.update(platform);
                addActionMessage("Piattaforma aggiornata con successo");
            }

            logger.info("Platform saved: " + platformType);
            return "success";
        } catch (Exception e) {
            logger.error("Error saving platform", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String delete() {
        try {
            if (id == null) {
                addActionError("ID piattaforma non fornito");
                return ERROR;
            }

            platform = platformDAO.findById(id);
            if (platform == null) {
                addActionError("Piattaforma non trovata");
                return ERROR;
            }

            platformService.removePlatform(id);
            addActionMessage("Piattaforma eliminata");
            logger.info("Platform deleted: " + id);

            return "success";
        } catch (Exception e) {
            logger.error("Error deleting platform", e);
            addActionError("Errore: " + e.getMessage());
            return ERROR;
        }
    }

    public String testConnection() {
        response = new java.util.HashMap<>();
        try {
            if (id == null) {
                response.put("success", false);
                response.put("message", "ID piattaforma non fornito");
                return "json";
            }

            boolean connected = platformService.testConnection(id);
            if (connected) {
                response.put("success", true);
                response.put("message", "Connessione riuscita");
            } else {
                response.put("success", false);
                response.put("message", "Connessione fallita");
            }

            return "json";
        } catch (Exception e) {
            logger.error("Error testing connection", e);
            response.put("success", false);
            response.put("message", "Errore: " + e.getMessage());
            return "json";
        }
    }

    // Getters and Setters
    public List<EcommercePlatform> getPlatforms() {
        return platforms;
    }

    public EcommercePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(EcommercePlatform platform) {
        this.platform = platform;
    }

    public Map<String, Object> getResponse() {
        return response;
    }

    public void setResponse(Map<String, Object> response) {
        this.response = response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlatformType() {
        return platformType;
    }

    public void setPlatformType(String platformType) {
        this.platformType = platformType;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public String getStoreUrl() {
        return storeUrl;
    }

    public void setStoreUrl(String storeUrl) {
        this.storeUrl = storeUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getSyncFrequencyMinutes() {
        return syncFrequencyMinutes;
    }

    public void setSyncFrequencyMinutes(Integer syncFrequencyMinutes) {
        this.syncFrequencyMinutes = syncFrequencyMinutes;
    }
}
