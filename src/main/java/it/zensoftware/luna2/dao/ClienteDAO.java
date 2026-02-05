package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Cliente;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * DAO for Cliente entity
 */
public class ClienteDAO extends GenericDAOImpl<Cliente, Long> {
    
    private static final Logger logger = LogManager.getLogger(ClienteDAO.class);

    public ClienteDAO() {
        super(Cliente.class);
    }

    /**
     * Find all active clients
     */
    public List<Cliente> findAllActive() {
        try (Session session = getSession()) {
            Query<Cliente> query = session.createQuery(
                "FROM Cliente WHERE attivo = true ORDER BY ragioneSociale", Cliente.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding active clients", e);
            throw new RuntimeException("Error finding active clients", e);
        }
    }

    /**
     * Search clients by name
     */
    public List<Cliente> searchByName(String searchTerm) {
        try (Session session = getSession()) {
            Query<Cliente> query = session.createQuery(
                "FROM Cliente WHERE LOWER(ragioneSociale) LIKE LOWER(:searchTerm) ORDER BY ragioneSociale", 
                Cliente.class);
            query.setParameter("searchTerm", "%" + searchTerm + "%");
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching clients", e);
            throw new RuntimeException("Error searching clients", e);
        }
    }

    /**
     * Find client by Partita IVA
     */
    public Cliente findByPartitaIva(String partitaIva) {
        try (Session session = getSession()) {
            Query<Cliente> query = session.createQuery(
                "FROM Cliente WHERE partitaIva = :partitaIva", Cliente.class);
            query.setParameter("partitaIva", partitaIva);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding client by Partita IVA", e);
            return null;
        }
    }

    /**
     * Find client by codice cliente
     */
    public Cliente findByCodiceCliente(String codiceCliente) {
        try (Session session = getSession()) {
            Query<Cliente> query = session.createQuery(
                "FROM Cliente WHERE codiceCliente = :codiceCliente", Cliente.class);
            query.setParameter("codiceCliente", codiceCliente);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding client by codice cliente", e);
            return null;
        }
    }

    /**
     * Generate next codice cliente (CLI001, CLI002, ...)
     */
    public String generateNextCodiceCliente() {
        try (Session session = getSession()) {
            Query<String> query = session.createQuery(
                "SELECT c.codiceCliente FROM Cliente c WHERE c.codiceCliente LIKE 'CLI%' ORDER BY c.codiceCliente DESC",
                String.class);
            query.setMaxResults(1);
            String lastCode = query.uniqueResult();
            
            if (lastCode != null && lastCode.startsWith("CLI")) {
                try {
                    int number = Integer.parseInt(lastCode.substring(3));
                    return String.format("CLI%03d", number + 1);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid codice cliente format: " + lastCode);
                }
            }
            return "CLI001";
        } catch (Exception e) {
            logger.error("Error generating codice cliente", e);
            return "CLI001";
        }
    }
}
