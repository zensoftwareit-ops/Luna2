package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.EcommerceOrderDAO;
import it.zensoftware.luna2.dao.OrdineDAO;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.Ordine;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class EcommercePaymentReconciliationService {

    private static final Logger logger = LogManager.getLogger(EcommercePaymentReconciliationService.class);

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

    public enum PaymentStatus {
        PENDING,
        AUTHORIZED,
        CHARGED,
        PARTIALLY_REFUNDED,
        REFUNDED,
        FAILED,
        CANCELLED
    }

    public void reconcileOrderPayment(EcommerceOrder ecommerceOrder) {
        try {
            logger.info("Reconciling payment for eCommerce order: " + ecommerceOrder.getExternalOrderId());

            // Find corresponding Luna2 order
            Ordine luna2Order = findLuna2OrderForEcommerceOrder(ecommerceOrder);
            if (luna2Order == null) {
                logger.warn("Luna2 order not found for eCommerce order: " + ecommerceOrder.getExternalOrderId());
                return;
            }

            // Determine payment status from eCommerce order
            PaymentStatus paymentStatus = determinePaymentStatus(ecommerceOrder);

            // Log payment information
            logPaymentTransaction(ecommerceOrder, luna2Order, paymentStatus);

            // Update Luna2 order state based on payment status
            updateOrderStateBasedOnPayment(luna2Order, paymentStatus, ecommerceOrder);

        } catch (Exception e) {
            logger.error("Error reconciling payment for eCommerce order: " + ecommerceOrder.getExternalOrderId(), e);
        }
    }

    public PaymentStatus determinePaymentStatus(EcommerceOrder ecommerceOrder) {
        try {
            String orderStatus = ecommerceOrder.getOrderStatus();

            if (orderStatus == null) {
                return PaymentStatus.PENDING;
            }

            orderStatus = orderStatus.toLowerCase();

            // Platform-specific status mapping
            if (orderStatus.contains("refunded")) {
                return PaymentStatus.REFUNDED;
            } else if (orderStatus.contains("partial") && orderStatus.contains("refund")) {
                return PaymentStatus.PARTIALLY_REFUNDED;
            } else if (orderStatus.contains("failed") || orderStatus.contains("cancelled")) {
                return PaymentStatus.FAILED;
            } else if (orderStatus.contains("completed") || orderStatus.contains("processing") ||
                    orderStatus.contains("charged") || orderStatus.contains("paid")) {
                return PaymentStatus.CHARGED;
            } else if (orderStatus.contains("authorized")) {
                return PaymentStatus.AUTHORIZED;
            } else if (orderStatus.contains("pending")) {
                return PaymentStatus.PENDING;
            }

            return PaymentStatus.PENDING;
        } catch (Exception e) {
            logger.warn("Error determining payment status, defaulting to PENDING", e);
            return PaymentStatus.PENDING;
        }
    }

    private void logPaymentTransaction(EcommerceOrder ecommerceOrder, Ordine luna2Order, PaymentStatus status) {
        try {
            String logEntry = String.format(
                    "Payment Transaction: eCommerce Order %s (Luna2 Order %s) - Status: %s - Amount: %s %s - Timestamp: %s",
                    ecommerceOrder.getExternalOrderId(),
                    luna2Order.getNumero(),
                    status,
                    ecommerceOrder.getTotalAmount(),
                    ecommerceOrder.getCurrency(),
                    new Date()
            );
            logger.info(logEntry);
        } catch (Exception e) {
            logger.error("Error logging payment transaction", e);
        }
    }

    private void updateOrderStateBasedOnPayment(Ordine luna2Order, PaymentStatus paymentStatus, EcommerceOrder ecommerceOrder) {
        try {
            switch (paymentStatus) {
                case CHARGED:
                case AUTHORIZED:
                    // Order is paid, move to processing
                    if (luna2Order.getStato() == Ordine.Stato.CONFERMATO) {
                        luna2Order.setStato(Ordine.Stato.IN_LAVORAZIONE);
                        getOrdineDAO().update(luna2Order);
                        logger.info("Updated Luna2 order " + luna2Order.getNumero() + " to IN_LAVORAZIONE (payment confirmed)");
                    }
                    break;

                case REFUNDED:
                case PARTIALLY_REFUNDED:
                    // Order has been refunded, may need to cancel/adjust
                    logger.info("Refund detected for order " + luna2Order.getNumero() + ": " + paymentStatus);
                    // Could trigger credit note generation here if needed
                    break;

                case FAILED:
                    // Payment failed, order may need to be cancelled
                    if (luna2Order.getStato() != Ordine.Stato.ANNULLATO) {
                        luna2Order.setStato(Ordine.Stato.ANNULLATO);
                        getOrdineDAO().update(luna2Order);
                        logger.info("Updated Luna2 order " + luna2Order.getNumero() + " to ANNULLATO (payment failed)");
                    }
                    break;

                case PENDING:
                    logger.debug("Payment still pending for order " + luna2Order.getNumero());
                    break;
            }
        } catch (Exception e) {
            logger.error("Error updating order state based on payment", e);
        }
    }

    public void reconcileAllPendingPayments() {
        try {
            logger.info("Starting payment reconciliation for all orders");

            // Get all eCommerce orders that are in payment-sensitive states
            List<EcommerceOrder> orders = ecommerceOrderDAO.findAll();

            for (EcommerceOrder order : orders) {
                try {
                    reconcileOrderPayment(order);
                } catch (Exception e) {
                    logger.error("Error reconciling payment for order: " + order.getExternalOrderId(), e);
                }
            }

            logger.info("Payment reconciliation completed for all orders");
        } catch (Exception e) {
            logger.error("Error during payment reconciliation batch", e);
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

    public boolean verifyPaymentAmount(EcommerceOrder ecommerceOrder, Ordine luna2Order) {
        try {
            BigDecimal ecommerceAmount = ecommerceOrder.getTotalAmount() != null ? ecommerceOrder.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal luna2Amount = luna2Order.getTotale() != null ? luna2Order.getTotale() : BigDecimal.ZERO;

            if (ecommerceAmount.compareTo(luna2Amount) != 0) {
                logger.warn("Payment amount mismatch for order " + ecommerceOrder.getExternalOrderId() +
                        " - eCommerce: " + ecommerceAmount + " Luna2: " + luna2Amount);
                return false;
            }

            logger.info("Payment amount verified for order " + ecommerceOrder.getExternalOrderId() +
                    ": " + ecommerceAmount + " " + ecommerceOrder.getCurrency());
            return true;
        } catch (Exception e) {
            logger.error("Error verifying payment amount", e);
            return false;
        }
    }
}
