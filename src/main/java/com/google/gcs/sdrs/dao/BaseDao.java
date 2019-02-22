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
import java.util.Map;
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
 * @param <T> an annotated entity object
 * @param <Id> the id type for the entity
 */
public abstract class BaseDao<T, Id extends Serializable> implements Dao<T, Id> {

  private static final Logger logger = LoggerFactory.getLogger(BaseDao.class);
  private static final String HIBERNATE_CONNECTION_URL_ENV = "HIBERNATE_CONNECTION_URL";
  private static final String HIBERNATE_CONNECTION_URL_PROPERTY_KEY = "hibernate.connection.url";
  private static final String HIBERNATE_CONNECTION_USER_ENV = "HIBERNATE_CONNECTION_USER";
  private static final String HIBERNATE_CONNECTION_USER_PROPERTY_KEY = "hibernate.connection.username";
  private static final String HIBERNATE_CONNECTION_PASSWORD_ENV = "HIBERNATE_CONNECTION_PASSWORD";
  private static final String HIBERNATE_CONNECTION_PASSWORD_PROPERTY_KEY = "hibernate.connection.password";

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;
  private Session currentSession;
  private Transaction currentTransaction;

  /** A class reference for the entity type */
  protected final Class<T> type;

  /** Constructs a DAO object with the entity class type */
  public BaseDao(final Class<T> type) {
    this.type = type;
  }

  /** Opens and returns a session */
  protected Session openCurrentSession() {
    currentSession = getSessionFactory().openSession();
    return currentSession;
  }

  /** Opens and returns a session with a transaction */
  protected Session openCurrentSessionWithTransaction() {
    currentSession = getSessionFactory().openSession();
    currentTransaction = currentSession.beginTransaction();
    return currentSession;
  }

  /** Closes the currently open session */
  protected void closeCurrentSession() {
    currentSession.close();
  }

  /** Commits the current transaction and closes the currently open session */
  protected void closeCurrentSessionWithTransaction() {
    currentTransaction.commit();
    currentSession.close();
  }

  /** Gets the current session */
  protected Session getCurrentSession() {
    return currentSession;
  }

  /** Sets the current session */
  protected void setCurrentSession(Session currentSession) {
    this.currentSession = currentSession;
  }

  /** Gets the current transaction */
  protected Transaction getCurrentTransaction() {
    return currentTransaction;
  }

  /** Sets the current session */
  protected void setCurrentTransaction(Transaction currentTransaction) {
    this.currentTransaction = currentTransaction;
  }

  /** Gets the session factory */
  protected static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        // Create registry
        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder().configure();
        registryBuilder.applySetting(HIBERNATE_CONNECTION_URL_PROPERTY_KEY, System.getenv(HIBERNATE_CONNECTION_URL_ENV));
        registryBuilder.applySetting(HIBERNATE_CONNECTION_USER_PROPERTY_KEY, System.getenv(HIBERNATE_CONNECTION_USER_ENV));
        registryBuilder.applySetting(HIBERNATE_CONNECTION_PASSWORD_PROPERTY_KEY, System.getenv(HIBERNATE_CONNECTION_PASSWORD_ENV));
        registry = registryBuilder.build();


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
