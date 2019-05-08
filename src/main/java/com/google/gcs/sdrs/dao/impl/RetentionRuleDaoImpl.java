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

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hibernate based Generic Dao implementation */
public class RetentionRuleDaoImpl extends GenericDao<RetentionRule, Integer>
    implements RetentionRuleDao {

  private static final Logger logger = LoggerFactory.getLogger(RetentionRuleDaoImpl.class);

  public RetentionRuleDaoImpl() {
    super(RetentionRule.class);
  }

  /**
   * Get the dataset {@link RetentionRule} with the provided dataStorageName and projectId
   *
   * @param projectId a {@link String} with the GCP project ID
   * @param dataStorage a {@link String} of the form 'gs://bucketName'
   * @return a {@link RetentionRule} record
   */
  @Override
  public RetentionRule findDatasetRuleByBusinessKey(String projectId, String dataStorage) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = query.from(RetentionRule.class);

    query
        .select(root)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DATASET),
            builder.equal(root.get("projectId"), projectId),
            builder.equal(root.get("dataStorageName"), dataStorage));

    return getSingleRecordWithCriteriaQuery(query, session);
  }

  @Override
  public RetentionRule findByBusinessKey(
      String projectId,
      String dataStorageName,
      Boolean includeDeactivated,
      RetentionRuleType retentionRuleType) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = query.from(RetentionRule.class);

    List<Predicate> predicates = new LinkedList<>();
    predicates.add(builder.equal(root.get("projectId"), projectId));
    predicates.add(builder.equal(root.get("dataStorageName"), dataStorageName));
    predicates.add(builder.equal(root.get("type"), retentionRuleType));
    if (!includeDeactivated) {
      predicates.add(builder.equal(root.get("isActive"), true));
    }
    Predicate[] predicateArray = new Predicate[predicates.size()];
    predicateArray = predicates.toArray(predicateArray);

    query.select(root).where(predicateArray);
    return getSingleRecordWithCriteriaQuery(query, session);
  }

  /**
   * Sets isActive to false for the provided RetentionRule
   *
   * @param entity the rule to deactivate
   */
  @Override
  public Integer softDelete(RetentionRule entity) {
    Session session = openSession();
    Transaction transaction = session.beginTransaction();
    entity.setIsActive(false);
    update(entity);
    closeSessionWithTransaction(session, transaction);
    return entity.getId();
  }

  /**
   * Gets the global rule based on its project id
   *
   * @param projectId the {@link String} project id to search by. Should be "global-default"
   * @return the global {@link RetentionRule}
   */
  @Override
  public RetentionRule findGlobalRuleByProjectId(String projectId) {
    Session session = openSession();
    RetentionRule globalDefaultRule = null;
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionRule> criteria = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = criteria.from(RetentionRule.class);

    criteria
        .select(root)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.GLOBAL),
            builder.equal(root.get("projectId"), projectId));

    Query<RetentionRule> query = session.createQuery(criteria);
    List<RetentionRule> result = query.getResultList();

    if (result.isEmpty()) {
      criteria = builder.createQuery(RetentionRule.class);
      root = criteria.from(RetentionRule.class);

      criteria
          .select(root)
          .where(
              builder.equal(root.get("isActive"), true),
              builder.equal(root.get("type"), RetentionRuleType.GLOBAL),
              builder.equal(
                  root.get("projectId"),
                  SdrsApplication.getAppConfigProperty("sts.defaultProjectId")));

      query = session.createQuery(criteria);
      result = query.getResultList();
    }
    if (!result.isEmpty()) {
      globalDefaultRule = result.get(0);
    }
    closeSession(session);
    return globalDefaultRule;
  }

  /**
   * Gets all project ids associated with dataset rules
   *
   * @return a {@link List} of {@link String} project ids
   */
  @Override
  public List<String> getAllDatasetRuleProjectIds() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<String> criteria = builder.createQuery(String.class);
    Root<RetentionRule> root = criteria.from(RetentionRule.class);

    criteria
        .select(root.get("projectId"))
        .distinct(true)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DATASET));

    Query<String> query = session.createQuery(criteria);
    List<String> result = query.getResultList();
    closeSession(session);
    return result;
  }

  /**
   * Gets all dataset rules associated with a given project id
   *
   * @param projectId the {@link String} project id
   * @return a {@link List} of dataset {@link RetentionRule}s
   */
  @Override
  public List<RetentionRule> findDatasetRulesByProjectId(String projectId) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionRule> criteria = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = criteria.from(RetentionRule.class);

    criteria
        .select(root)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DATASET),
            builder.equal(root.get("projectId"), projectId));

    Query<RetentionRule> query = session.createQuery(criteria);
    List<RetentionRule> result = query.getResultList();
    closeSession(session);
    return result;
  }

  /**
   * Find all retention rules by data stroage root, i.e. GCS bucket
   *
   * @param projectId  GCP project ID
   * @param dataStorageRoot The root of the data storage. i.e. GCS bucket
   * @param retentionRuleType Retention rule type
   * @param includeDeactivated Whether or not to include deactivated rules
   * @return
   */
  @Override
  public List<RetentionRule> findRulesByDataStorageRoot(
      String projectId,
      String dataStorageRoot,
      RetentionRuleType retentionRuleType,
      Boolean includeDeactivated) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = query.from(RetentionRule.class);

    List<Predicate> predicates = new LinkedList<>();
    predicates.add(builder.equal(root.get("projectId"), projectId));
    predicates.add(builder.equal(root.get("dataStorageRoot"), dataStorageRoot));
    predicates.add(builder.equal(root.get("type"), retentionRuleType));
    if (!includeDeactivated) {
      predicates.add(builder.equal(root.get("isActive"), true));
    }
    Predicate[] predicateArray = new Predicate[predicates.size()];
    predicateArray = predicates.toArray(predicateArray);
    query.select(root).where(predicateArray);
    List<RetentionRule> result = session.createQuery(query).getResultList();
    closeSession(session);
    return result;
  }

  @Override
  public List<RetentionRule> findDefaultRulesByProjectId(String projectId) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionRule> criteria = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = criteria.from(RetentionRule.class);

    criteria
        .select(root)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DEFAULT),
            builder.equal(root.get("projectId"), projectId));

    Query<RetentionRule> query = session.createQuery(criteria);
    List<RetentionRule> result = query.getResultList();
    closeSession(session);
    return result;
  }
}
