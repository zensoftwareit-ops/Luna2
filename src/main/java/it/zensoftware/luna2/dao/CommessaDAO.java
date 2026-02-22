package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Commessa;
import it.zensoftware.luna2.model.Commessa.StatoCommessa;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * DAO per la gestione delle commesse
 */
public class CommessaDAO extends GenericDAOImpl<Commessa, Long> {
    
    private static final Logger logger = LogManager.getLogger(CommessaDAO.class);

    public CommessaDAO() {
        super(Commessa.class);
    }

    /**
     * Trova tutte le commesse
     */
    public List<Commessa> findAll() {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c ORDER BY c.dataApertura DESC", 
                Commessa.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding all commesse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova commesse per stato
     */
    public List<Commessa> findByStato(StatoCommessa stato) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.stato = :stato ORDER BY c.dataApertura DESC", 
                Commessa.class);
            query.setParameter("stato", stato);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding commesse by stato", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova commessa per numero
     */
    public Commessa findByNumero(String numero) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.numero = :numero", 
                Commessa.class);
            query.setParameter("numero", numero);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding commessa by numero", e);
            return null;
        }
    }

    /**
     * Trova commessa per preventivo
     */
    public Commessa findByPreventivo(Long preventivoId) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.preventivo.id = :preventivoId", 
                Commessa.class);
            query.setParameter("preventivoId", preventivoId);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding commessa by preventivo", e);
            return null;
        }
    }

    /**
     * Trova commesse per cliente
     */
    public List<Commessa> findByCliente(Long clienteId) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.preventivo.cliente.id = :clienteId " +
                "ORDER BY c.dataApertura DESC", 
                Commessa.class);
            query.setParameter("clienteId", clienteId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding commesse by cliente", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova commesse per anno
     */
    public List<Commessa> findByAnno(Integer anno) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.anno = :anno ORDER BY c.numero DESC", 
                Commessa.class);
            query.setParameter("anno", anno);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding commesse by anno", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova commesse aperte
     */
    public List<Commessa> findAperte() {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.stato IN (:stati) " +
                "ORDER BY c.dataApertura ASC", 
                Commessa.class);
            query.setParameterList("stati", List.of(
                StatoCommessa.APERTA, 
                StatoCommessa.IN_LAVORAZIONE, 
                StatoCommessa.SOSPESA
            ));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding commesse aperte", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova commesse scadute (data prevista fine < oggi)
     */
    public List<Commessa> findScadute() {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c WHERE c.stato IN (:stati) " +
                "AND c.dataPrevistFine < :oggi " +
                "ORDER BY c.dataPrevistFine ASC", 
                Commessa.class);
            query.setParameterList("stati", List.of(
                StatoCommessa.APERTA,
                StatoCommessa.IN_LAVORAZIONE
            ));
            query.setParameter("oggi", new Date());
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding commesse scadute", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Genera il prossimo numero commessa per l'anno
     */
    public String generaNuovoNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(c) FROM Commessa c WHERE c.anno = :anno", 
                Long.class);
            query.setParameter("anno", anno);
            Long count = query.uniqueResult();
            
            int progressivo = count.intValue() + 1;
            return String.format("COM-%04d-%04d", anno, progressivo);
        } catch (Exception e) {
            logger.error("Error generating numero commessa", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Calcola valore totale commesse per stato
     */
    public BigDecimal calcolaValoreTotaleByStato(StatoCommessa stato) {
        try (Session session = getSession()) {
            Query<BigDecimal> query = session.createQuery(
                "SELECT COALESCE(SUM(c.totale), 0) FROM Commessa c WHERE c.stato = :stato", 
                BigDecimal.class);
            query.setParameter("stato", stato);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error calculating valore totale", e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Conta commesse per stato
     */
    public long countByStato(StatoCommessa stato) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT COUNT(c) FROM Commessa c WHERE c.stato = :stato", 
                Long.class);
            query.setParameter("stato", stato);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting commesse", e);
            return 0;
        }
    }

    /**
     * Cerca commesse per termine (numero, descrizione, cliente)
     */
    public List<Commessa> search(String searchTerm) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c " +
                "LEFT JOIN FETCH c.preventivo p " +
                "LEFT JOIN FETCH p.cliente cli " +
                "WHERE LOWER(c.numero) LIKE LOWER(:term) " +
                "OR LOWER(c.descrizione) LIKE LOWER(:term) " +
                "OR LOWER(cli.ragioneSociale) LIKE LOWER(:term) " +
                "ORDER BY c.dataApertura DESC", 
                Commessa.class);
            query.setParameter("term", "%" + searchTerm + "%");
            return query.list();
        } catch (Exception e) {
            logger.error("Error searching commesse", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Trova commessa con righe eagerly loaded
     */
    public Commessa findWithRighe(Long id) {
        try (Session session = getSession()) {
            Query<Commessa> query = session.createQuery(
                "FROM Commessa c " +
                "LEFT JOIN FETCH c.righe r " +
                "LEFT JOIN FETCH c.preventivo p " +
                "LEFT JOIN FETCH p.cliente " +
                "WHERE c.id = :id", 
                Commessa.class);
            query.setParameter("id", id);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding commessa with righe", e);
            return null;
        }
    }
}
