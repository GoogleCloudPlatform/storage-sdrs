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
package com.google.gcs.sdrs.dao.impl;

import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.model.DistributedLock;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.GregorianCalendar;
import java.util.UUID;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hibernate based LockDao implementation */
public class LockDaoImpl extends GenericDao<DistributedLock, Integer> implements LockDao {
  private static final Logger logger = LoggerFactory.getLogger(DmQueueDaoImpl.class);

  public LockDaoImpl() {
    super(DistributedLock.class);
  }

  /**
   * To use a distributed lock a user has to start a lock session, which is a Hibernate session in
   * this implementation
   *
   * @return a Hibernate(JPA) session
   */
  public Session getLockSession() {
    return openSession();
  }

  /**
   * Close the lock session after the lock is released.
   *
   * @param lockSession a lock session, which is a Hibernate session in this implementation
   */
  public void closeLockSession(Session lockSession) {
    if (lockSession != null && lockSession.isOpen()) {
      closeSession(lockSession);
    }
  }

  /**
   * Get a exclusive lock. The method uses JPA PESSIMISTIC_WRITE lock, which translates into an
   * exclusive row-level lock for the underlining database.
   *
   * @param lockSession a Hiberneate session.
   * @return a DistributedLock if lock is acquired or null otherwise.
   */
  public DistributedLock obtainLock(Session lockSession, int timeout, String lockId) {
    if (lockSession == null || lockId == null) {
      return null;
    }
    DistributedLock distributedLock = null;
    try {
      lockSession.beginTransaction();

      distributedLock =
          lockSession.get(
              DistributedLock.class,
              lockId,
              new LockOptions(LockMode.PESSIMISTIC_WRITE).setTimeOut(timeout));
      distributedLock.setLockToken(generateUniqueToken(Thread.currentThread().getName()));
      distributedLock.setCreatedAt(new Timestamp(System.currentTimeMillis()));
    } catch (PessimisticLockException e) {
      logger.info("Lock wait timeout exceeded.", e);
    } catch (Exception e) {
      logger.info("Failed to acquire lock.", e);
    }

    return distributedLock;
  }

  /**
   * Reelase the lock by committing the transaction.
   *
   * @param lockSession a Hibernate session.
   * @param distributedLock a DistributedLock entity
   */
  public void releaseLock(Session lockSession, DistributedLock distributedLock) {
    if (distributedLock == null
        || lockSession == null
        || !lockSession.isOpen()
        || lockSession.getTransaction() == null
        || !lockSession.getTransaction().isActive()) {
      return;
    }
    try {
      distributedLock.getCreatedAt().toInstant();
      long duration =
          Instant.now().toEpochMilli() - distributedLock.getCreatedAt().toInstant().toEpochMilli();

      distributedLock.setLockDuration(duration);

      logger.debug(
          String.format(
              "tokenName=%s, duration=%d, createdAt=%s",
              distributedLock.getLockToken(),
              distributedLock.getLockDuration(),
              distributedLock.getCreatedAt().toInstant().toString()));
      lockSession.update(distributedLock);
      closeSessionWithTransaction(lockSession, lockSession.getTransaction());
    } catch (Exception e) {
      logger.info("Failed to release lock.", e);
    }
  }

  /**
   * Distributed lock is implemented using database exclusive row-level lock. The method provisions
   * the record in the table before the distributed lock can be used.
   *
   * @param lockId a unique lock ID.
   * @return
   */
  public DistributedLock initLock(String lockId) {
    Session session = openSession();
    DistributedLock distributedLock = session.get(DistributedLock.class, lockId);
    if (distributedLock == null) {
      distributedLock = new DistributedLock();
      distributedLock.setLockToken("init-lock-token");
      distributedLock.setCreatedAt(new Timestamp(System.currentTimeMillis()));
      distributedLock.setLockDuration(0);
      distributedLock.setId(lockId);
      Transaction transaction = session.beginTransaction();
      session.save(distributedLock);
      transaction.commit();
    }
    closeSession(session);
    return distributedLock;
  }

  private String generateUniqueToken(String seed) {
    String uniqueToken = seed + "-";
    try {
      InetAddress inetAddress = InetAddress.getLocalHost();
      uniqueToken = uniqueToken + inetAddress.getAddress().toString() + "-";
      uniqueToken = uniqueToken + inetAddress.getHostName() + "-";
      uniqueToken = uniqueToken + GregorianCalendar.getInstance().getTimeInMillis();
    } catch (UnknownHostException e) {
      UUID uuid = UUID.randomUUID();
      uniqueToken = uniqueToken + uuid.toString() + "-";
      uniqueToken = uniqueToken + GregorianCalendar.getInstance().getTimeInMillis();
    }
    return uniqueToken;
  }
}
