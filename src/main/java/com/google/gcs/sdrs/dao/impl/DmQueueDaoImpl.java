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

import com.google.gcs.sdrs.dao.DMQueueDao;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hibernate based DmQueue implementation */
public class DmQueueDaoImpl extends GenericDao<DmRequest, Integer> implements DMQueueDao {

  private static final Logger logger = LoggerFactory.getLogger(DmQueueDaoImpl.class);

  public DmQueueDaoImpl() {
    super(DmRequest.class);
  }

  @Override
  public List<DmRequest> getAllAvailableRequestsByPriority() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    Transaction transaction = session.beginTransaction();

    CriteriaQuery<DmRequest> query = builder.createQuery(DmRequest.class);
    Root<DmRequest> root = query.from(DmRequest.class);

    Predicate pending_status = (builder.equal(root.get("status"), DatabaseConstants.DM_REQUEST_STATUS_PENDING));
    Predicate retry_status = (builder.equal(root.get("status"), DatabaseConstants.DM_REQUEST_STATIUS_RETRY));
    Predicate pending_or_retry = builder.or(pending_status, retry_status);

    List<Order> orderList = new ArrayList();
    orderList.add(builder.desc(root.get("priority")));
    orderList.add(builder.desc(root.get("numberOfRetry")));
    orderList.add(builder.asc(root.get("createdAt")));

    query.where(pending_or_retry);
    query.orderBy(orderList);

    List<DmRequest> results = session.createQuery(query).getResultList();
    closeSessionWithTransaction(session, transaction);

    return results;
  }

  @Override
  public void createRetentionJobUdpateDmStatus(
      RetentionJob retentionJob, List<DmRequest> dmRequests) throws IOException {
    Session session = null;
    Transaction transaction = null;

    try {
      session = openSession();
      transaction = session.beginTransaction();
      Integer retentionJobId = (Integer) session.save(retentionJob);

      dmRequests.stream()
          .forEach(
              request -> {
                request.setRetentionJobId(retentionJobId);
                request.setStatus(DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
              });
      int i = 0;

      // database batch update
      for (DmRequest dmRequest : dmRequests) {
        session.saveOrUpdate(dmRequest);

        if (++i % 20 == 0) {
          session.flush();
          session.clear();
        }
      }
      transaction.commit();
      session.close();
    } catch (PersistenceException e) {
      // catch unchecked exceptions to rollback and throw application level IOException
      logger.error("Runtime exception.", e);
      if (transaction != null && transaction.isActive()) {
        transaction.rollback();
      }
      if (session != null && session.isOpen()) {
        session.close();
      }

      throw new IOException(
          String.format(
              "Failed to create retention job and update DM requests. bucket: %s; STS: %s.",
              retentionJob.getDataStorageRoot(), retentionJob.getName()));
    }
  }
}
