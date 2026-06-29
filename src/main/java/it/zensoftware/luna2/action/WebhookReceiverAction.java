package it.zensoftware.luna2.action;

import it.zensoftware.luna2.model.EcommercePlatform;
import it.zensoftware.luna2.service.WebhookHandlerService;
import it.zensoftware.luna2.dao.EcommercePlatformDAO;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.opensymphony.xwork2.ActionSupport;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletInputStream;

public class WebhookReceiverAction extends ActionSupport {

    private static final Logger logger = LogManager.getLogger(WebhookReceiverAction.class);

    private WebhookHandlerService webhookHandler;
    private EcommercePlatformDAO platformDAO;

    private String platform;
    private String payload;
    private Map<String, String> response = new HashMap<>();
    private int httpStatusCode = 200;

    private WebhookHandlerService getWebhookHandler() {
        if (webhookHandler == null) {
            webhookHandler = new WebhookHandlerService();
        }
        return webhookHandler;
    }

    private EcommercePlatformDAO getPlatformDAO() {
        if (platformDAO == null) {
            platformDAO = new EcommercePlatformDAO();
        }
        return platformDAO;
    }

    public String handleWooCommerce() {
        try {
            logger.info("Received WooCommerce webhook");

            String signature = getHeaderValue("X-WC-Webhook-Signature");
            if (signature == null) {
                logger.warn("Missing WooCommerce webhook signature");
                response.put("success", "false");
                response.put("error", "Missing signature");
                httpStatusCode = 401;
                return "json";
            }

            java.util.List<EcommercePlatform> platforms = getPlatformDAO().findByPlatformType(
                    EcommercePlatform.PlatformType.WOOCOMMERCE);

            if (platforms == null || platforms.isEmpty()) {
                logger.warn("WooCommerce platform not configured or inactive");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not found");
                return "json";
            }

            EcommercePlatform platform = platforms.get(0);
            if (!platform.getIsActive()) {
                logger.warn("WooCommerce platform is inactive");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not active");
                return "json";
            }

            boolean handled = getWebhookHandler().handleWooCommerceWebhook(
                    payload, signature, platform.getApiSecret());

            if (handled) {
                response.put("success", "true");
                response.put("message", "Webhook processed successfully");
                httpStatusCode = 200;
            } else {
                response.put("success", "false");
                response.put("error", "Signature verification failed");
                httpStatusCode = 401;
            }
        } catch (Exception e) {
            logger.error("Error handling WooCommerce webhook", e);
            response.put("success", "false");
            response.put("error", e.getMessage());
            httpStatusCode = 500;
        }

        return "json";
    }

    public String handleShopify() {
        try {
            logger.info("Received Shopify webhook");

            String signature = getHeaderValue("X-Shopify-Hmac-SHA256");
            if (signature == null) {
                logger.warn("Missing Shopify webhook signature");
                response.put("success", "false");
                response.put("error", "Missing signature");
                httpStatusCode = 401;
                return "json";
            }

            String platformName = getHeaderValue("X-Shopify-Shop-Api-Access-Token");
            EcommercePlatform platform = getPlatformDAO().findByApiKey(platformName);

            if (platform == null || !platform.getIsActive()) {
                logger.warn("Shopify platform not found for token");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not found");
                return "json";
            }

            boolean handled = getWebhookHandler().handleShopifyWebhook(
                    payload, signature, platform.getApiSecret());

            if (handled) {
                response.put("success", "true");
                response.put("message", "Webhook processed successfully");
                httpStatusCode = 200;
            } else {
                response.put("success", "false");
                response.put("error", "Signature verification failed");
                httpStatusCode = 401;
            }
        } catch (Exception e) {
            logger.error("Error handling Shopify webhook", e);
            response.put("success", "false");
            response.put("error", e.getMessage());
            httpStatusCode = 500;
        }

        return "json";
    }

    public String handleAmazon() {
        try {
            logger.info("Received Amazon webhook");

            String signature = getHeaderValue("x-amzn-EventSignature");
            String certificateUrl = getHeaderValue("x-amzn-CertificateUrl");

            if (signature == null || certificateUrl == null) {
                logger.warn("Missing Amazon webhook signature or certificate URL");
                response.put("success", "false");
                response.put("error", "Missing signature or certificate");
                httpStatusCode = 401;
                return "json";
            }

            java.util.List<EcommercePlatform> platforms = getPlatformDAO().findByPlatformType(
                    EcommercePlatform.PlatformType.AMAZON);

            if (platforms == null || platforms.isEmpty()) {
                logger.warn("Amazon platform not configured or inactive");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not found");
                return "json";
            }

            EcommercePlatform platform = platforms.get(0);
            if (!platform.getIsActive()) {
                logger.warn("Amazon platform is inactive");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not active");
                return "json";
            }

            boolean handled = getWebhookHandler().handleAmazonWebhook(
                    payload, signature, platform.getApiSecret());

            if (handled) {
                response.put("success", "true");
                response.put("message", "Webhook processed successfully");
                httpStatusCode = 200;
            } else {
                response.put("success", "false");
                response.put("error", "Signature verification failed");
                httpStatusCode = 401;
            }
        } catch (Exception e) {
            logger.error("Error handling Amazon webhook", e);
            response.put("success", "false");
            response.put("error", e.getMessage());
            httpStatusCode = 500;
        }

        return "json";
    }

    public String handleEbay() {
        try {
            logger.info("Received eBay webhook");

            String signature = getHeaderValue("X-EBAY-SIGNATURE");
            if (signature == null) {
                logger.warn("Missing eBay webhook signature");
                response.put("success", "false");
                response.put("error", "Missing signature");
                httpStatusCode = 401;
                return "json";
            }

            java.util.List<EcommercePlatform> platforms = getPlatformDAO().findByPlatformType(
                    EcommercePlatform.PlatformType.EBAY);

            if (platforms == null || platforms.isEmpty()) {
                logger.warn("eBay platform not configured or inactive");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not found");
                return "json";
            }

            EcommercePlatform platform = platforms.get(0);
            if (!platform.getIsActive()) {
                logger.warn("eBay platform is inactive");
                httpStatusCode = 404;
                response.put("success", "false");
                response.put("error", "Platform not active");
                return "json";
            }

            boolean handled = getWebhookHandler().handleEbayWebhook(
                    payload, signature, platform.getApiSecret());

            if (handled) {
                response.put("success", "true");
                response.put("message", "Webhook processed successfully");
                httpStatusCode = 200;
            } else {
                response.put("success", "false");
                response.put("error", "Signature verification failed");
                httpStatusCode = 401;
            }
        } catch (Exception e) {
            logger.error("Error handling eBay webhook", e);
            response.put("success", "false");
            response.put("error", e.getMessage());
            httpStatusCode = 500;
        }

        return "json";
    }

    private String getHeaderValue(String headerName) {
        javax.servlet.http.HttpServletRequest request =
                (javax.servlet.http.HttpServletRequest) org.apache.struts2.ServletActionContext.getRequest();
        return request.getHeader(headerName);
    }

    @Override
    public String execute() throws Exception {
        try {
            ServletInputStream inputStream =
                    ((javax.servlet.http.HttpServletRequest) org.apache.struts2.ServletActionContext.getRequest())
                    .getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            payload = stringBuilder.toString();

            if (payload == null || payload.isEmpty()) {
                logger.warn("Empty webhook payload received");
                response.put("success", "false");
                response.put("error", "Empty payload");
                httpStatusCode = 400;
                return "json";
            }

            if (platform == null || platform.isEmpty()) {
                logger.warn("Missing platform parameter in webhook request");
                response.put("success", "false");
                response.put("error", "Missing platform parameter");
                httpStatusCode = 400;
                return "json";
            }

            switch (platform.toLowerCase()) {
                case "woocommerce":
                    return handleWooCommerce();
                case "shopify":
                    return handleShopify();
                case "amazon":
                    return handleAmazon();
                case "ebay":
                    return handleEbay();
                default:
                    logger.warn("Unknown platform: " + platform);
                    response.put("success", "false");
                    response.put("error", "Unknown platform");
                    httpStatusCode = 400;
                    return "json";
            }
        } catch (Exception e) {
            logger.error("Error receiving webhook", e);
            response.put("success", "false");
            response.put("error", e.getMessage());
            httpStatusCode = 500;
            return "json";
        }
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public Map<String, String> getResponse() {
        return response;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }
}
