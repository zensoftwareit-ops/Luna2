package it.zensoftware.luna2.service;

import it.zensoftware.luna2.dao.ClienteDAO;
import it.zensoftware.luna2.dao.OrdineDAO;
import it.zensoftware.luna2.dao.ProdottoDAO;
import it.zensoftware.luna2.model.Cliente;
import it.zensoftware.luna2.model.EcommerceOrder;
import it.zensoftware.luna2.model.EcommerceProduct;
import it.zensoftware.luna2.model.Ordine;
import it.zensoftware.luna2.model.OrdineRiga;
import it.zensoftware.luna2.model.Prodotto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.Date;

public class EcommerceOrderIntegrationService {

    private static final Logger logger = LogManager.getLogger(EcommerceOrderIntegrationService.class);

    private OrdineDAO ordineDAO;
    private ClienteDAO clienteDAO;
    private ProdottoDAO prodottoDAO;
    private ObjectMapper objectMapper = new ObjectMapper();

    private OrdineDAO getOrdineDAO() {
        if (ordineDAO == null) {
            ordineDAO = new OrdineDAO();
        }
        return ordineDAO;
    }

    private ClienteDAO getClienteDAO() {
        if (clienteDAO == null) {
            clienteDAO = new ClienteDAO();
        }
        return clienteDAO;
    }

    private ProdottoDAO getProdottoDAO() {
        if (prodottoDAO == null) {
            prodottoDAO = new ProdottoDAO();
        }
        return prodottoDAO;
    }

    public void integrateEcommerceOrderToLuna2(EcommerceOrder ecommerceOrder) {
        try {
            logger.info("Integrating eCommerce order to Luna2: " + ecommerceOrder.getExternalOrderId());

            // Find or create customer
            Cliente cliente = findOrCreateCliente(ecommerceOrder);
            if (cliente == null) {
                logger.warn("Unable to create or find customer for eCommerce order: " + ecommerceOrder.getExternalOrderId());
                return;
            }

            // Find existing Luna2 order linked to this eCommerce order
            Ordine ordine = findLuna2OrderByEcommerceId(ecommerceOrder.getExternalOrderId());

            if (ordine == null) {
                // Create new Luna2 order
                ordine = createLuna2Order(ecommerceOrder, cliente);
                getOrdineDAO().save(ordine);
                logger.info("Created new Luna2 order: " + ordine.getNumero() + " for eCommerce order: " + ecommerceOrder.getExternalOrderId());
            } else {
                // Update existing Luna2 order
                updateLuna2Order(ordine, ecommerceOrder);
                getOrdineDAO().update(ordine);
                logger.info("Updated Luna2 order: " + ordine.getNumero() + " for eCommerce order: " + ecommerceOrder.getExternalOrderId());
            }

            // Process line items
            processLineItems(ordine, ecommerceOrder);

        } catch (Exception e) {
            logger.error("Error integrating eCommerce order to Luna2: " + ecommerceOrder.getExternalOrderId(), e);
        }
    }

    private Cliente findOrCreateCliente(EcommerceOrder ecommerceOrder) {
        try {
            // Search for existing customer by email
            if (ecommerceOrder.getCustomerEmail() != null && !ecommerceOrder.getCustomerEmail().isEmpty()) {
                Cliente cliente = getClienteDAO().findByEmail(ecommerceOrder.getCustomerEmail());
                if (cliente != null) {
                    logger.info("Found existing customer by email: " + ecommerceOrder.getCustomerEmail());
                    return cliente;
                }
            }

            // Create new customer
            Cliente cliente = new Cliente();
            cliente.setRagioneSociale(ecommerceOrder.getCustomerName() != null ? ecommerceOrder.getCustomerName() : "Cliente eCommerce");
            cliente.setEmail(ecommerceOrder.getCustomerEmail());
            cliente.setNote("Creato da ordine eCommerce: " + ecommerceOrder.getExternalOrderId());

            getClienteDAO().save(cliente);
            logger.info("Created new customer: " + cliente.getRagioneSociale());
            return cliente;
        } catch (Exception e) {
            logger.error("Error finding or creating customer for eCommerce order: " + ecommerceOrder.getExternalOrderId(), e);
            return null;
        }
    }

    private Ordine findLuna2OrderByEcommerceId(String ecommerceOrderId) {
        try {
            // Search by reference number (will store eCommerce order ID in the reference field)
            return getOrdineDAO().findByRiferimentoCliente(ecommerceOrderId);
        } catch (Exception e) {
            logger.debug("No existing Luna2 order found for eCommerce ID: " + ecommerceOrderId);
            return null;
        }
    }

    private Ordine createLuna2Order(EcommerceOrder ecommerceOrder, Cliente cliente) {
        Ordine ordine = new Ordine();

        String orderNumber = generateOrderNumber(ecommerceOrder);
        ordine.setNumero(orderNumber);
        ordine.setDataOrdine(ecommerceOrder.getOrderDate() != null ? ecommerceOrder.getOrderDate() : new Date());
        ordine.setCliente(cliente);
        ordine.setRiferimentoCliente(ecommerceOrder.getExternalOrderId()); // Store eCommerce order ID
        ordine.setOggetto("Ordine da " + ecommerceOrder.getPlatformType());
        ordine.setStato(Ordine.Stato.CONFERMATO);
        ordine.setTotale(ecommerceOrder.getTotalAmount() != null ? ecommerceOrder.getTotalAmount() : BigDecimal.ZERO);

        return ordine;
    }

    private void updateLuna2Order(Ordine ordine, EcommerceOrder ecommerceOrder) {
        // Update order totals if amounts have changed
        if (ecommerceOrder.getTotalAmount() != null) {
            ordine.setTotale(ecommerceOrder.getTotalAmount());
        }

        // Update order status based on eCommerce status
        if ("completed".equalsIgnoreCase(ecommerceOrder.getOrderStatus())) {
            ordine.setStato(Ordine.Stato.EVASO);
        } else if ("cancelled".equalsIgnoreCase(ecommerceOrder.getOrderStatus())) {
            ordine.setStato(Ordine.Stato.ANNULLATO);
        } else if ("processing".equalsIgnoreCase(ecommerceOrder.getOrderStatus())) {
            ordine.setStato(Ordine.Stato.IN_LAVORAZIONE);
        }
    }

    private void processLineItems(Ordine ordine, EcommerceOrder ecommerceOrder) {
        try {
            // Clear existing lines if updating
            if (ordine.getId() != null) {
                ordine.getRighe().clear();
            }

            // Parse line items from externalJson
            if (ecommerceOrder.getLineItems() != null) {
                JsonNode lineItems = objectMapper.readTree(ecommerceOrder.getLineItems());

                int lineNumber = 1;
                if (lineItems.isArray()) {
                    for (JsonNode item : lineItems) {
                        OrdineRiga riga = createOrdineRigaFromLineItem(item, ordine, lineNumber);
                        if (riga != null) {
                            ordine.getRighe().add(riga);
                            lineNumber++;
                        }
                    }
                }
            }

            if (ordine.getRighe().isEmpty()) {
                // Create a single line item with the order total
                OrdineRiga riga = new OrdineRiga();
                riga.setOrdine(ordine);
                riga.setRigaNumero(1);
                riga.setTipoRiga(OrdineRiga.TipoRiga.PRODOTTO);
                riga.setDescrizione("Ordine: " + ecommerceOrder.getOrderNumber());
                riga.setQuantita(BigDecimal.ONE);
                riga.setPrezzoUnitario(ecommerceOrder.getTotalAmount() != null ? ecommerceOrder.getTotalAmount() : BigDecimal.ZERO);
                riga.setImponibileRiga(ecommerceOrder.getTotalAmount() != null ? ecommerceOrder.getTotalAmount() : BigDecimal.ZERO);
                riga.setTotaleRiga(ecommerceOrder.getTotalAmount() != null ? ecommerceOrder.getTotalAmount() : BigDecimal.ZERO);

                ordine.getRighe().add(riga);
                logger.info("Created single line item for eCommerce order: " + ecommerceOrder.getExternalOrderId());
            }

        } catch (Exception e) {
            logger.error("Error processing line items for eCommerce order: " + ecommerceOrder.getExternalOrderId(), e);
        }
    }

    private OrdineRiga createOrdineRigaFromLineItem(JsonNode item, Ordine ordine, int lineNumber) {
        try {
            OrdineRiga riga = new OrdineRiga();
            riga.setOrdine(ordine);
            riga.setRigaNumero(lineNumber);
            riga.setTipoRiga(OrdineRiga.TipoRiga.PRODOTTO);

            // Extract product information
            String productName = item.has("name") ? item.get("name").asText() : "Prodotto";
            String sku = item.has("sku") ? item.get("sku").asText() : "";

            // Try to find product by SKU
            if (!sku.isEmpty()) {
                try {
                    Prodotto prodotto = getProdottoDAO().findBySku(sku);
                    if (prodotto != null) {
                        riga.setProdotto(prodotto);
                        riga.setDescrizione(prodotto.getNome());
                    } else {
                        riga.setDescrizione(productName + " (SKU: " + sku + ")");
                    }
                } catch (Exception e) {
                    riga.setDescrizione(productName + " (SKU: " + sku + ")");
                }
            } else {
                riga.setDescrizione(productName);
            }

            // Extract quantity and price
            BigDecimal quantity = item.has("quantity") ? new BigDecimal(item.get("quantity").asText()) : BigDecimal.ONE;
            BigDecimal price = item.has("price") ? new BigDecimal(item.get("price").asText()) : BigDecimal.ZERO;

            riga.setQuantita(quantity);
            riga.setPrezzoUnitario(price);
            riga.setImponibileRiga(quantity.multiply(price));
            riga.setTotaleRiga(quantity.multiply(price));

            return riga;
        } catch (Exception e) {
            logger.error("Error creating OrdineRiga from line item", e);
            return null;
        }
    }

    private String generateOrderNumber(EcommerceOrder ecommerceOrder) {
        // Generate order number from platform and external ID
        String prefix = ecommerceOrder.getPlatformType().toString().substring(0, 3).toUpperCase();
        String externalId = ecommerceOrder.getExternalOrderId().replaceAll("[^0-9]", "");
        if (externalId.length() > 6) {
            externalId = externalId.substring(externalId.length() - 6);
        }
        return prefix + "-" + externalId + "-" + System.currentTimeMillis() % 1000;
    }

    public Ordine getLuna2OrderByEcommerceId(String ecommerceOrderId) {
        return findLuna2OrderByEcommerceId(ecommerceOrderId);
    }
}
