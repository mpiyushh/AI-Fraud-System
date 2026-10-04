package com.sentinel.dao;

import com.sentinel.exception.DatabaseException;
import java.util.List;

/**
 * Generic Data Access Object (DAO) interface.
 * Demonstrates Generics and Interfaces in OOP.
 *
 * @param <T> entity type
 * @param <ID> primary key identifier type
 */
public interface GenericDAO<T, ID> {

    /**
     * Inserts a new entity record into the database.
     */
    void save(T entity) throws DatabaseException;

    /**
     * Retrieves an entity by its primary key.
     */
    T findById(ID id) throws DatabaseException;

    /**
     * Returns all records for this entity type.
     */
    List<T> findAll() throws DatabaseException;

    /**
     * Updates an existing entity record in the database.
     */
    void update(T entity) throws DatabaseException;

    /**
     * Deletes an entity record by ID.
     */
    void delete(ID id) throws DatabaseException;
}
