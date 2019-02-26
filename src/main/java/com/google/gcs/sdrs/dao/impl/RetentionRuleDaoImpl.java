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

import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
   * @param projectId a {@link String} with the GCP project ID
   * @param dataStorage a {@link String} of the form 'gs://bucketName'
   *
   * @return a {@link RetentionRule} record
   */
  @Override
  public RetentionRule findDatasetRuleByBusinessKey(String projectId, String dataStorage) {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = query.from(RetentionRule.class);

    query
        .select(root)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DATASET),
            builder.equal(root.get("projectId"), projectId),
            builder.equal(root.get("dataStorageName"), dataStorage));

    return getSingleRecordWithCriteriaQuery(query);
  }

  /**
   * Finds the RetentionRule uniquely identified by the provided values
   *
   * @return a {@link RetentionRule}
   */
  @Override
  public RetentionRule findByBusinessKey(String projectId, String dataStorageName) {
    return findByBusinessKey(projectId, dataStorageName, false);
  }

  /**
   * Finds the RetentionRule uniquely identified by the provided values
   *
   * @param projectId associated with the rule
   * @param dataStorageName associated with the rule
   * @param includeDeactivated flag to prevent including inactive records in the response
   * @return a {@link RetentionRule}
   */
  @Override
  public RetentionRule findByBusinessKey(
      String projectId, String dataStorageName, Boolean includeDeactivated) {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = query.from(RetentionRule.class);

    List<Predicate> predicates = new LinkedList<>();
    predicates.add(builder.equal(root.get("projectId"), projectId));
    predicates.add(builder.equal(root.get("dataStorageName"), dataStorageName));
    if (!includeDeactivated) {
      predicates.add(builder.equal(root.get("isActive"), true));
    }
    Predicate[] predicateArray = new Predicate[predicates.size()];
    predicateArray = predicates.toArray(predicateArray);

    query.select(root).where(predicateArray);
    return getSingleRecordWithCriteriaQuery(query);
  }

  /**
   * Sets isActive to false for the provided RetentionRule
   *
   * @param entity the rule to deactivate
   */
  @Override
  public Integer softDelete(RetentionRule entity) {
    entity.setIsActive(false);
    update(entity);
    return entity.getId();
  }

  /**
   * Gets all project ids associated with dataset rules
   *
   * @return a {@link List} of {@link String} project ids
   */
  @Override
  public List<String> getAllDatasetRuleProjectIds() {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<String> criteria = builder.createQuery(String.class);
    Root<RetentionRule> root = criteria.from(RetentionRule.class);

    criteria
        .select(root.get("projectId"))
        .distinct(true)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DATASET));

    Query<String> query = getCurrentSession().createQuery(criteria);
    List<String> result = query.getResultList();
    closeCurrentSession();
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
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionRule> criteria = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = criteria.from(RetentionRule.class);

    criteria
        .select(root)
        .where(
            builder.equal(root.get("isActive"), true),
            builder.equal(root.get("type"), RetentionRuleType.DATASET),
            builder.equal(root.get("projectId"), projectId));

    Query<RetentionRule> query = getCurrentSession().createQuery(criteria);
    List<RetentionRule> result = query.getResultList();
    closeCurrentSession();
    return result;
  }

  private RetentionRule getSingleRecordWithCriteriaQuery(CriteriaQuery<RetentionRule> query) {
    Query<RetentionRule> queryResults = getCurrentSession().createQuery(query);
    List<RetentionRule> list = queryResults.getResultList();

    RetentionRule foundEntity = null;
    if (!list.isEmpty()) {
      foundEntity = list.get(0);
    }

    closeCurrentSession();
    return foundEntity;
  }
}
