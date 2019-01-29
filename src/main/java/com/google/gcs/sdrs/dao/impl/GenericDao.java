/*
 * Copyright 2019 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 * Any software provided by Google hereunder is distributed “AS IS”,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.
 *
 */

package com.google.gcs.sdrs.dao.impl;

import java.io.Serializable;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import com.google.gcs.sdrs.dao.Dao;

/**
 * Hibernate based Generic Dao implementation
 *
 * @param <T>
 * @param <Id>
 */
public class GenericDao<T, Id extends Serializable> implements Dao<T, Id> {

  private final Class<T> type;

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;

  protected Session currentSession;
  protected Transaction currentTransaction;

  public GenericDao(final Class<T> type) {
    this.type = type;
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#persist(T)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Id persist(final T entity) {
    openCurrentSessionWithTransaction();
    Id result = (Id) getCurrentSession().save(entity);
    closeCurrentSessionwithTransaction();
    return result;
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#update(T)
   */
  @Override
  public void update(final T object) {
    openCurrentSessionWithTransaction();
    getCurrentSession().update(object);
    closeCurrentSessionwithTransaction();
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#findById(Id)
   */
  @Override
  @SuppressWarnings("unchecked")
  public T findById(Id id) {
    openCurrentSession(); // no transaction per se for a find
    Object object = getCurrentSession().get(type, id);
    closeCurrentSession();
    return (T) object;
  }

  /* (non-Javadoc)
   * @see com.google.gcs.sdrs.dao.impl.DAO#delete(T)
   */
  @Override
  public void delete(final T object) {
    openCurrentSessionWithTransaction();
    getCurrentSession().delete(object);
    closeCurrentSessionwithTransaction();
  }

  public Session openCurrentSession() {
    currentSession = getSessionFactory().openSession();
    return currentSession;
  }

  public Session openCurrentSessionWithTransaction() {
    currentSession = getSessionFactory().openSession();
    currentTransaction = currentSession.beginTransaction();
    return currentSession;
  }

  public void closeCurrentSession() {
    currentSession.close();
  }

  public void closeCurrentSessionwithTransaction() {
    currentTransaction.commit();
    currentSession.close();
  }

  public static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        // Create registry
        registry = new StandardServiceRegistryBuilder().configure().build();

        // Create MetadataSources
        MetadataSources sources = new MetadataSources(registry);

        // Create Metadata
        Metadata metadata = sources.getMetadataBuilder().build();

        // Create SessionFactory
        sessionFactory = metadata.getSessionFactoryBuilder().build();

      } catch (Exception e) {
        e.printStackTrace();
        if (registry != null) {
          StandardServiceRegistryBuilder.destroy(registry);
        }
      }
    }
    return sessionFactory;
  }

  public Session getCurrentSession() {
    return currentSession;
  }

  public void setCurrentSession(Session currentSession) {
    this.currentSession = currentSession;
  }

  public Transaction getCurrentTransaction() {
    return currentTransaction;
  }

  public void setCurrentTransaction(Transaction currentTransaction) {
    this.currentTransaction = currentTransaction;
  }
}
