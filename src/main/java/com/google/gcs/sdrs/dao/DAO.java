package com.google.gcs.sdrs.dao;

import java.io.Serializable;

/**
 * Interface for Data Access Object pattern 
 * 
 * @author salguerod
 *
 * @param <T>
 * @param <Id>
 */
public interface DAO<T, Id extends Serializable> {

	Id persist(T entity);

	void update(T object);

	T findById(Id id);

	void delete(T object);

}
