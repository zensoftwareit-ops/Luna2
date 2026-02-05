package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Preventivo;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class PreventivoDAO extends GenericDAOImpl<Preventivo, Long> {
    
    private static final Logger logger = LogManager.getLogger(PreventivoDAO.class);

    public PreventivoDAO() {
        super(Preventivo.class);
    }

    public List<Preventivo> findByClienteId(Long clienteId) {
        try (Session session = getSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo WHERE cliente.id = :clienteId ORDER BY dataPreventivo DESC", 
                Preventivo.class);
            query.setParameter("clienteId", clienteId);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by cliente", e);
            throw new RuntimeException(e);
        }
    }

    public List<Preventivo> findByStato(Preventivo.Stato stato) {
        try (Session session = getSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo WHERE stato = :stato ORDER BY dataPreventivo DESC", 
                Preventivo.class);
            query.setParameter("stato", stato);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by stato", e);
            throw new RuntimeException(e);
        }
    }

    public List<Preventivo> findByAnno(Integer anno) {
        try (Session session = getSession()) {
            Query<Preventivo> query = session.createQuery(
                "FROM Preventivo WHERE anno = :anno ORDER BY numero DESC", 
                Preventivo.class);
            query.setParameter("anno", anno);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding preventivi by anno", e);
            throw new RuntimeException(e);
        }
    }

    public String getNextNumero(Integer anno) {
        try (Session session = getSession()) {
            Query<Long> query = session.createQuery(
                "SELECT MAX(CAST(numero AS long)) FROM Preventivo WHERE anno = :anno", 
                Long.class);
            query.setParameter("anno", anno);
            Long maxNumero = query.uniqueResult();
            long nextNumero = (maxNumero != null) ? maxNumero + 1 : 1;
            return String.format("%d/%04d", anno, nextNumero);
        } catch (Exception e) {
            logger.error("Error getting next numero", e);
            return anno + "/0001";
        }
    }
}
