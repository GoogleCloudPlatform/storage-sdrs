package com.google.gcs.sdrs.dao.impl;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import com.google.gcs.sdrs.dao.DMQueueTableDao;
import com.google.gcs.sdrs.dao.model.DMQueueTableEntry;
import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_READY;
import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_READY_RETRY;
import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_PROCESSING;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DMQueueTableDaoImpl extends GenericDao<DMQueueTableEntry, Integer> implements DMQueueTableDao {


  private static final Logger logger = LoggerFactory.getLogger(DMQueueTableDaoImpl.class);

  public DMQueueTableDaoImpl() {
    super(DMQueueTableEntry.class);
  }
  


  @Override
  public List<DMQueueTableEntry> getAllAvailableQueueForProcessingSTSJobs() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    Transaction transaction = session.beginTransaction();

    CriteriaQuery<DMQueueTableEntry> query = builder.createQuery(DMQueueTableEntry.class);
    Root<DMQueueTableEntry> root = query.from(DMQueueTableEntry.class);

    Predicate ready_status = (builder.equal(root.get("status"), DMQUEUE_STATUS_READY));
    Predicate ready_retry_status = (builder.equal(root.get("status"), DMQUEUE_STATUS_READY_RETRY));
    Predicate ready_or_ready_retry = builder.or(ready_status, ready_retry_status);

    List<Order> orderList = new ArrayList();
    orderList.add(builder.desc(root.get("priority")));
    orderList.add(builder.desc(root.get("numberOfRetry")));
    orderList.add(builder.asc(root.get("createdAt")));

    query.where(ready_or_ready_retry);
    query.orderBy(orderList);

    List<DMQueueTableEntry> results = session.createQuery(query).getResultList();
    closeSessionWithTransaction(session, transaction);

    return results;
  }

  @Override
  public List<DMQueueTableEntry> getQueueEntryForSTSLock() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    Transaction transaction = session.beginTransaction();

    CriteriaQuery<DMQueueTableEntry> query = builder.createQuery(DMQueueTableEntry.class);
    Root<DMQueueTableEntry> root = query.from(DMQueueTableEntry.class);

    Predicate ready_status = (builder.equal(root.get("status"), DMQUEUE_STATUS_PROCESSING));
    query.where(ready_status);

    List<DMQueueTableEntry> results = session.createQuery(query).getResultList();
    closeSessionWithTransaction(session, transaction);

    return results;
  }



}
