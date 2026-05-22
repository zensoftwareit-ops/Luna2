package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.EcommerceOrderDAO;
import it.zensoftware.luna2.dao.OrdineDAO;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.Ordine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.Date;

public class EcommerceShipmentTrackingService {

    private static final Logger logger = LogManager.getLogger(EcommerceShipmentTrackingService.class);

    private EcommerceOrderDAO ecommerceOrderDAO;
    private OrdineDAO ordineDAO;

    private EcommerceOrderDAO getEcommerceOrderDAO() {
        if (ecommerceOrderDAO == null) {
            ecommerceOrderDAO = new EcommerceOrderDAO();
        }
        return ecommerceOrderDAO;
    }

    private OrdineDAO getOrdineDAO() {
        if (ordineDAO == null) {
            ordineDAO = new OrdineDAO();
        }
        return ordineDAO;
    }

    public enum ShipmentStatus {
        NOT_SHIPPED,
        PARTIALLY_SHIPPED,
        SHIPPED,
        IN_TRANSIT,
        OUT_FOR_DELIVERY,
        DELIVERED,
        DELIVERY_FAILED,
        RETURNED
    }

    public void trackShipment(EcommerceOrder ecommerceOrder) {
        try {
            logger.info("Tracking shipment for eCommerce order: " + ecommerceOrder.getExternalOrderId());

            // Find corresponding Luna2 order
            Ordine luna2Order = findLuna2OrderForEcommerceOrder(ecommerceOrder);
            if (luna2Order == null) {
                logger.debug("Luna2 order not found for eCommerce order: " + ecommerceOrder.getExternalOrderId());
                return;
            }

            // Determine shipment status from eCommerce order
            ShipmentStatus shipmentStatus = determineShipmentStatus(ecommerceOrder);

            // Update Luna2 order state based on shipment status
            updateOrderStateBasedOnShipment(luna2Order, shipmentStatus, ecommerceOrder);

            // Log shipment information
            logShipmentEvent(ecommerceOrder, luna2Order, shipmentStatus);

        } catch (Exception e) {
            logger.error("Error tracking shipment for eCommerce order: " + ecommerceOrder.getExternalOrderId(), e);
        }
    }

    public ShipmentStatus determineShipmentStatus(EcommerceOrder ecommerceOrder) {
        try {
            String orderStatus = ecommerceOrder.getOrderStatus();

            if (orderStatus == null) {
                return ShipmentStatus.NOT_SHIPPED;
            }

            orderStatus = orderStatus.toLowerCase();

            if (orderStatus.contains("delivered") || orderStatus.contains("completato")) {
                return ShipmentStatus.DELIVERED;
            } else if (orderStatus.contains("out for delivery") || orderStatus.contains("consegna")) {
                return ShipmentStatus.OUT_FOR_DELIVERY;
            } else if (orderStatus.contains("transit") || orderStatus.contains("transito")) {
                return ShipmentStatus.IN_TRANSIT;
            } else if (orderStatus.contains("shipped") || orderStatus.contains("spedito")) {
                return ShipmentStatus.SHIPPED;
            } else if (orderStatus.contains("partial") && orderStatus.contains("ship")) {
                return ShipmentStatus.PARTIALLY_SHIPPED;
            } else if (orderStatus.contains("failed") && orderStatus.contains("delivery")) {
                return ShipmentStatus.DELIVERY_FAILED;
            } else if (orderStatus.contains("returned") || orderStatus.contains("reso")) {
                return ShipmentStatus.RETURNED;
            }

            return ShipmentStatus.NOT_SHIPPED;
        } catch (Exception e) {
            logger.warn("Error determining shipment status, defaulting to NOT_SHIPPED", e);
            return ShipmentStatus.NOT_SHIPPED;
        }
    }

    private void updateOrderStateBasedOnShipment(Ordine luna2Order, ShipmentStatus shipmentStatus, EcommerceOrder ecommerceOrder) {
        try {
            switch (shipmentStatus) {
                case SHIPPED:
                case IN_TRANSIT:
                case OUT_FOR_DELIVERY:
                    // Order is being shipped
                    if (luna2Order.getStato() == Ordine.Stato.IN_LAVORAZIONE) {
                        luna2Order.setStato(Ordine.Stato.PARZIALMENTE_EVASO);
                        getOrdineDAO().update(luna2Order);
                        logger.info("Updated Luna2 order " + luna2Order.getNumero() + " to PARZIALMENTE_EVASO (shipment in progress)");
                    }
                    break;

                case DELIVERED:
                    // Order is delivered, mark as evaso (fulfilled)
                    if (luna2Order.getStato() != Ordine.Stato.EVASO) {
                        luna2Order.setStato(Ordine.Stato.EVASO);
                        getOrdineDAO().update(luna2Order);
                        logger.info("Updated Luna2 order " + luna2Order.getNumero() + " to EVASO (delivered)");
                    }
                    break;

                case DELIVERY_FAILED:
                case RETURNED:
                    logger.warn("Issue detected for order " + luna2Order.getNumero() + ": " + shipmentStatus +
                            " - May require manual intervention");
                    // Could trigger alert or ticket creation
                    break;

                case NOT_SHIPPED:
                case PARTIALLY_SHIPPED:
                    logger.debug("Order " + luna2Order.getNumero() + " status: " + shipmentStatus);
                    break;
            }
        } catch (Exception e) {
            logger.error("Error updating order state based on shipment status", e);
        }
    }

    private void logShipmentEvent(EcommerceOrder ecommerceOrder, Ordine luna2Order, ShipmentStatus status) {
        try {
            String logEntry = String.format(
                    "Shipment Event: eCommerce Order %s (Luna2 Order %s) - Status: %s - Timestamp: %s",
                    ecommerceOrder.getExternalOrderId(),
                    luna2Order.getNumero(),
                    status,
                    new Date()
            );
            logger.info(logEntry);
        } catch (Exception e) {
            logger.error("Error logging shipment event", e);
        }
    }

    public void trackAllShipments() {
        try {
            logger.info("Starting shipment tracking for all orders");

            java.util.List<EcommerceOrder> orders = ecommerceOrderDAO.findAll();

            for (EcommerceOrder order : orders) {
                try {
                    trackShipment(order);
                } catch (Exception e) {
                    logger.error("Error tracking shipment for order: " + order.getExternalOrderId(), e);
                }
            }

            logger.info("Shipment tracking completed for all orders");
        } catch (Exception e) {
            logger.error("Error during shipment tracking batch", e);
        }
    }

    private Ordine findLuna2OrderForEcommerceOrder(EcommerceOrder ecommerceOrder) {
        try {
            return getOrdineDAO().findByRiferimentoCliente(ecommerceOrder.getExternalOrderId());
        } catch (Exception e) {
            logger.debug("Could not find Luna2 order for eCommerce order: " + ecommerceOrder.getExternalOrderId());
            return null;
        }
    }

    public String getTrackingNumber(EcommerceOrder ecommerceOrder) {
        try {
            // Extract tracking number from externalJson if available
            String externalJson = ecommerceOrder.getExternalJson();
            if (externalJson != null && !externalJson.isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(externalJson);

                if (json.has("tracking_number")) {
                    return json.get("tracking_number").asText();
                } else if (json.has("trackingNumber")) {
                    return json.get("trackingNumber").asText();
                } else if (json.has("shipping_number")) {
                    return json.get("shipping_number").asText();
                } else if (json.has("carrier_code")) {
                    return json.get("carrier_code").asText();
                }
            }

            return null;
        } catch (Exception e) {
            logger.debug("Could not extract tracking number from eCommerce order: " + ecommerceOrder.getExternalOrderId());
            return null;
        }
    }

    public String getCarrierName(EcommerceOrder ecommerceOrder) {
        try {
            String externalJson = ecommerceOrder.getExternalJson();
            if (externalJson != null && !externalJson.isEmpty()) {
                com.fasterxml.jackson.databind.JsonNode json = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(externalJson);

                if (json.has("carrier")) {
                    return json.get("carrier").asText();
                } else if (json.has("shipping_carrier")) {
                    return json.get("shipping_carrier").asText();
                } else if (json.has("carrier_name")) {
                    return json.get("carrier_name").asText();
                }
            }

            return "Unknown";
        } catch (Exception e) {
            logger.debug("Could not extract carrier name from eCommerce order: " + ecommerceOrder.getExternalOrderId());
            return "Unknown";
        }
    }
}
