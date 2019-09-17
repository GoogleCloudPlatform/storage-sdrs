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

import com.google.gcs.sdrs.dao.model.DistributedLock;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.PooledStsJob;
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
 * @param <T> an annotated entity object
 * @param <Id> the id type for the entity
 */
public abstract class BaseDao<T, Id extends Serializable> implements Dao<T, Id> {

  private static final Logger logger = LoggerFactory.getLogger(BaseDao.class);
  private static final String HIBERNATE_CONNECTION_URL_ENV = "HIBERNATE_CONNECTION_URL";
  private static final String HIBERNATE_CONNECTION_URL_PROPERTY_KEY = "hibernate.connection.url";
  private static final String HIBERNATE_CONNECTION_USER_ENV = "HIBERNATE_CONNECTION_USER";
  private static final String HIBERNATE_CONNECTION_USER_PROPERTY_KEY =
      "hibernate.connection.username";
  private static final String HIBERNATE_CONNECTION_PASSWORD_ENV = "HIBERNATE_CONNECTION_PASSWORD";
  private static final String HIBERNATE_CONNECTION_PASSWORD_PROPERTY_KEY =
      "hibernate.connection.password";

  private static StandardServiceRegistry registry;
  private static SessionFactory sessionFactory;

  /** A class reference for the entity type */
  protected final Class<T> type;

  /** Constructs a DAO object with the entity class type */
  public BaseDao(final Class<T> type) {
    this.type = type;
  }

  protected Session openSession() {
    return getSessionFactory().openSession();
  }

  protected void closeSession(Session session) {
    try {
      if (session != null && session.isOpen()) {
        session.close();
      }
    } catch (Exception e) {
      logger.error("Error closing Hibernate session.", e);
    }
  }

  protected void closeSessionWithTransaction(Session session, Transaction transaction) {
    try {
      if (transaction != null && transaction.isActive()) {
        transaction.commit();
      }
      closeSession(session);
    } catch (Exception e) {
      logger.error("Error closing hibernate session with transaction.", e);
    }
  }

  /** Gets the session factory */
  protected static SessionFactory getSessionFactory() {
    if (sessionFactory == null) {
      try {
        // Create registry
        StandardServiceRegistryBuilder registryBuilder =
            new StandardServiceRegistryBuilder().configure();
        registryBuilder.applySetting(
            HIBERNATE_CONNECTION_URL_PROPERTY_KEY, System.getenv(HIBERNATE_CONNECTION_URL_ENV));
        registryBuilder.applySetting(
            HIBERNATE_CONNECTION_USER_PROPERTY_KEY, System.getenv(HIBERNATE_CONNECTION_USER_ENV));
        registryBuilder.applySetting(
            HIBERNATE_CONNECTION_PASSWORD_PROPERTY_KEY,
            System.getenv(HIBERNATE_CONNECTION_PASSWORD_ENV));
        registry = registryBuilder.build();

        // TODO - refactor to remove this hardcoded strategy
        Metadata metadata =
            new MetadataSources(registry)
                .addAnnotatedClass(RetentionRule.class)
                .addAnnotatedClass(RetentionJob.class)
                .addAnnotatedClass(RetentionJobValidation.class)
                .addAnnotatedClass(PooledStsJob.class)
                .addAnnotatedClass(DmRequest.class)
                .addAnnotatedClass(DistributedLock.class)
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

  public static boolean isSessionFactoryAvailable() {
    if (sessionFactory == null) {
      return false;
    } else {
      return sessionFactory.isOpen();
    }
  }
}
