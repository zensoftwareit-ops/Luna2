package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.Lead;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class LeadDAO extends GenericDAOImpl<Lead, Long> {
    
    private static final Logger logger = LogManager.getLogger(LeadDAO.class);

    public LeadDAO() {
        super(Lead.class);
    }

    public List<Lead> findByStato(Lead.Stato stato) {
        try (Session session = getSession()) {
            Query<Lead> query = session.createQuery(
                "FROM Lead WHERE stato = :stato ORDER BY dataCreazione DESC", Lead.class);
            query.setParameter("stato", stato);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding leads by stato", e);
            throw new RuntimeException(e);
        }
    }

    public List<Lead> findOpenLeads() {
        try (Session session = getSession()) {
            Query<Lead> query = session.createQuery(
                "FROM Lead WHERE stato NOT IN ('VINTO', 'PERSO') ORDER BY probabilitaChiusura DESC, dataCreazione DESC", 
                Lead.class);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding open leads", e);
            throw new RuntimeException(e);
        }
    }
}
