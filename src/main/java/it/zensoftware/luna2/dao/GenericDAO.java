package it.zensoftware.luna2.dao;

import java.io.Serializable;
import java.util.List;

/**
 * Generic DAO interface with common CRUD operations
 */
public interface GenericDAO<T, ID extends Serializable> {
    
    /**
     * Save an entity
     */
    T save(T entity);
    
    /**
     * Update an entity
     */
    void update(T entity);
    
    /**
     * Delete an entity
     */
    void delete(T entity);
    
    /**
     * Find entity by ID
     */
    T findById(ID id);
    
    /**
     * Find all entities
     */
    List<T> findAll();
    
    /**
     * Find entities by property
     */
    List<T> findByProperty(String propertyName, Object value);
    
    /**
     * Count all entities
     */
    long count();
}
