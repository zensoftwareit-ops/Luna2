package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Fornitore;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FornitoreDAO extends GenericDAOImpl<Fornitore, Long> {
    
    private static final Logger logger = LogManager.getLogger(FornitoreDAO.class);
    
    public FornitoreDAO() {
        super(Fornitore.class);
    }

    /**
     * Generate next codice fornitore (FOR001, FOR002, ...)
     */
    public String generateNextCodiceFornitore() {
        try (Session session = getSession()) {
            Query<String> query = session.createQuery(
                "SELECT f.codiceFornitore FROM Fornitore f WHERE f.codiceFornitore LIKE 'FOR%' ORDER BY f.codiceFornitore DESC",
                String.class);
            query.setMaxResults(1);
            String lastCode = query.uniqueResult();
            
            if (lastCode != null && lastCode.startsWith("FOR")) {
                try {
                    int number = Integer.parseInt(lastCode.substring(3));
                    return String.format("FOR%03d", number + 1);
                } catch (NumberFormatException e) {
                    logger.warn("Invalid codice fornitore format: " + lastCode);
                }
            }
            return "FOR001";
        } catch (Exception e) {
            logger.error("Error generating codice fornitore", e);
            return "FOR001";
        }
    }
}
