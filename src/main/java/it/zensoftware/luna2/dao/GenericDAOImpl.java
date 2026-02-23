package it.zensoftware.luna2.dao;

import it.zensoftware.luna2.util.HibernateUtil;
import it.zensoftware.luna2.util.ValidationUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Serializable;
import java.util.List;

/**
 * Generic DAO implementation with common CRUD operations
 * Features input validation to prevent HQL injection and other attacks
 */
public class GenericDAOImpl<T, ID extends Serializable> implements GenericDAO<T, ID> {
    
    private static final Logger logger = LogManager.getLogger(GenericDAOImpl.class);
    private Class<T> persistentClass;

    public GenericDAOImpl(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    @Override
    public T save(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(entity);
            transaction.commit();
            logger.debug("Saved entity: " + entity.getClass().getSimpleName());
            return entity;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error saving entity", e);
            throw new RuntimeException("Error saving entity", e);
        }
    }

    @Override
    public void update(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.update(entity);
            transaction.commit();
            logger.debug("Updated entity: " + entity.getClass().getSimpleName());
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error updating entity", e);
            throw new RuntimeException("Error updating entity", e);
        }
    }

    @Override
    public void delete(T entity) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.delete(entity);
            transaction.commit();
            logger.debug("Deleted entity: " + entity.getClass().getSimpleName());
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            logger.error("Error deleting entity", e);
            throw new RuntimeException("Error deleting entity", e);
        }
    }

    @Override
    public T findById(ID id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            T entity = session.get(persistentClass, id);
            logger.debug("Found entity by ID: " + id);
            return entity;
        } catch (Exception e) {
            logger.error("Error finding entity by ID", e);
            throw new RuntimeException("Error finding entity by ID", e);
        }
    }

    @Override
    public List<T> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Query<T> query = session.createQuery("FROM " + persistentClass.getName(), persistentClass);
            List<T> list = query.list();
            logger.debug("Found " + list.size() + " entities");
            return list;
        } catch (Exception e) {
            logger.error("Error finding all entities", e);
            throw new RuntimeException("Error finding all entities", e);
        }
    }

    @Override
    public List<T> findByProperty(String propertyName, Object value) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            // SECURITY: Validate property name against whitelist to prevent HQL injection
            ValidationUtil.validatePropertyName(persistentClass, propertyName);
            
            String hql = "FROM " + persistentClass.getName() + " WHERE " + propertyName + " = :value";
            Query<T> query = session.createQuery(hql, persistentClass);
            query.setParameter("value", value);
            
            List<T> results = query.list();
            
            // Log validation success for audit trail
            ValidationUtil.logValidationEvent("PROPERTY_SEARCH", persistentClass.getSimpleName(), 
                                             true, "propertyName=" + propertyName);
            
            logger.debug("Found " + results.size() + " entities with property: " + propertyName);
            return results;
        } catch (SecurityException e) {
            // Property name validation failed
            logger.error("Security violation: Invalid property name: " + propertyName, e);
            ValidationUtil.logValidationEvent("PROPERTY_SEARCH", persistentClass.getSimpleName(), 
                                             false, "Invalid property: " + propertyName);
            throw new RuntimeException("Invalid search property", e);
        } catch (Exception e) {
            logger.error("Error finding by property", e);
            throw new RuntimeException("Error finding by property", e);
        }
    }

    @Override
    public long count() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            String hql = "SELECT COUNT(*) FROM " + persistentClass.getName();
            Query<Long> query = session.createQuery(hql, Long.class);
            return query.uniqueResult();
        } catch (Exception e) {
            logger.error("Error counting entities", e);
            throw new RuntimeException("Error counting entities", e);
        }
    }

    protected Session getSession() {
        return HibernateUtil.getSessionFactory().openSession();
    }
}
