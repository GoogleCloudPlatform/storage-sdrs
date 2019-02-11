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
 */

package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.RetentionExecution;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import java.io.Serializable;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hibernate based DAO base class
 *
 * @param <T>
 * @param <Id>
 */
public abstract class BaseDao<T, Id extends Serializable> implements Dao<T, Id> {

  private static final Logger logger = LoggerFactory.getLogger(BaseDao.class);

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  private Session currentSession;
  private Transaction currentTransaction;

  protected final Class<T> type;

  public BaseDao(final Class<T> type) {
    this.type = type;
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

  public void closeCurrentSessionWithTransaction() {
    currentTransaction.commit();
    currentSession.close();
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

  protected static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        // Create registry
        registry = new StandardServiceRegistryBuilder().configure().build();

        // Create Metadata
        Metadata metadata =
            new MetadataSources(registry)
                .addAnnotatedClass(RetentionRule.class)
                .addAnnotatedClass(RetentionExecution.class)
                .addAnnotatedClass(RetentionJob.class)
                .addAnnotatedClass(RetentionJobValidation.class)
                .getMetadataBuilder()
                .build();

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
}
