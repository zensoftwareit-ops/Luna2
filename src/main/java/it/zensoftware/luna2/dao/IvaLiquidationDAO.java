package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.model.IvaLiquidation;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class IvaLiquidationDAO extends GenericDAOImpl<IvaLiquidation, Long> {

    private static final Logger logger = LogManager.getLogger(IvaLiquidationDAO.class);

    public IvaLiquidationDAO() {
        super(IvaLiquidation.class);
    }

    public IvaLiquidation findByPeriod(String period) {
        try (Session session = getSession()) {
            Query<IvaLiquidation> query = session.createQuery(
                "FROM IvaLiquidation WHERE liquidationPeriod = :period",
                IvaLiquidation.class);
            query.setParameter("period", period);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error finding IVA liquidation by period", e);
            return null;
        }
    }

    public List<IvaLiquidation> findByYear(Integer year) {
        try (Session session = getSession()) {
            Query<IvaLiquidation> query = session.createQuery(
                "FROM IvaLiquidation WHERE YEAR(liquidationDate) = :year ORDER BY liquidationDate DESC",
                IvaLiquidation.class);
            query.setParameter("year", year);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding IVA liquidations by year", e);
            return java.util.Collections.emptyList();
        }
    }

    public List<IvaLiquidation> findUnpaid() {
        try (Session session = getSession()) {
            Query<IvaLiquidation> query = session.createQuery(
                "FROM IvaLiquidation WHERE status IN (:statuses) ORDER BY dueDate ASC",
                IvaLiquidation.class);
            query.setParameter("statuses", java.util.Arrays.asList(
                IvaLiquidation.LiquidationStatus.CALCULATED,
                IvaLiquidation.LiquidationStatus.SUBMITTED,
                IvaLiquidation.LiquidationStatus.OVERDUE
            ));
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding unpaid IVA liquidations", e);
            return java.util.Collections.emptyList();
        }
    }

    public List<IvaLiquidation> findByYearAndStatus(Integer year, IvaLiquidation.LiquidationStatus status) {
        try (Session session = getSession()) {
            Query<IvaLiquidation> query = session.createQuery(
                "FROM IvaLiquidation WHERE YEAR(liquidationDate) = :year AND status = :status ORDER BY liquidationDate DESC",
                IvaLiquidation.class);
            query.setParameter("year", year);
            query.setParameter("status", status);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding IVA liquidations by year and status", e);
            return java.util.Collections.emptyList();
        }
    }

    public List<IvaLiquidation> findOverdue() {
        try (Session session = getSession()) {
            Query<IvaLiquidation> query = session.createQuery(
                "FROM IvaLiquidation WHERE dueDate < CURRENT_DATE AND status != :paidStatus ORDER BY dueDate ASC",
                IvaLiquidation.class);
            query.setParameter("paidStatus", IvaLiquidation.LiquidationStatus.PAID);
            return query.list();
        } catch (Exception e) {
            logger.error("Error finding overdue IVA liquidations", e);
            return java.util.Collections.emptyList();
        }
    }
}
