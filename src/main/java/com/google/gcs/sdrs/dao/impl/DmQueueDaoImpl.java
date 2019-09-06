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

import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_PROCESSING;
import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_READY;
import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_READY_RETRY;

import com.google.gcs.sdrs.dao.DMQueueDao;
import com.google.gcs.sdrs.dao.model.DMQueue;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hibernate based  DmQueue implementation */
public class DmQueueDaoImpl extends GenericDao<DMQueue, Integer> implements DMQueueDao {

  private static final Logger logger = LoggerFactory.getLogger(DmQueueDaoImpl.class);

  public DmQueueDaoImpl() {
    super(DMQueue.class);
  }

  @Override
  public List<DMQueue> getAllAvailableQueueForProcessingSTSJobs() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    Transaction transaction = session.beginTransaction();

    CriteriaQuery<DMQueue> query = builder.createQuery(DMQueue.class);
    Root<DMQueue> root = query.from(DMQueue.class);

    Predicate ready_status = (builder.equal(root.get("status"), DMQUEUE_STATUS_READY));
    Predicate ready_retry_status = (builder.equal(root.get("status"), DMQUEUE_STATUS_READY_RETRY));
    Predicate ready_or_ready_retry = builder.or(ready_status, ready_retry_status);

    List<Order> orderList = new ArrayList();
    orderList.add(builder.desc(root.get("priority")));
    orderList.add(builder.desc(root.get("numberOfRetry")));
    orderList.add(builder.asc(root.get("createdAt")));

    query.where(ready_or_ready_retry);
    query.orderBy(orderList);

    List<DMQueue> results = session.createQuery(query).getResultList();
    closeSessionWithTransaction(session, transaction);

    return results;
  }

  @Override
  public List<DMQueue> getQueueEntryForSTSLock() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    Transaction transaction = session.beginTransaction();

    CriteriaQuery<DMQueue> query = builder.createQuery(DMQueue.class);
    Root<DMQueue> root = query.from(DMQueue.class);

    Predicate ready_status = (builder.equal(root.get("status"), DMQUEUE_STATUS_PROCESSING));
    query.where(ready_status);

    List<DMQueue> results = session.createQuery(query).getResultList();
    closeSessionWithTransaction(session, transaction);

    return results;
  }
}
