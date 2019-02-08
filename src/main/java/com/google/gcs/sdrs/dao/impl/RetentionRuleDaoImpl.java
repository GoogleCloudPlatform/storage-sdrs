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
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
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
   * Get a {@link List} of {@link RetentionRule}s with the provided dataStorage and dataSet
   *
   * @param dataStorage a {@link String} of the form 'gs://bucketName'
   * @return a {@link List} of {@link RetentionRule}s
   */
  public List<RetentionRule> getAllDatasetRulesInDataStorage(String dataStorage) {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class);
    Root<RetentionRule> root = query.from(RetentionRule.class);

    query
        .select(root)
        // These string values correspond to the entity field names
        .where(builder.equal(root.get("dataStorageName"), dataStorage))
        .where(builder.equal(root.get("type"), RetentionRuleType.DATASET));

    Query<RetentionRule> result = getCurrentSession().createQuery(query);
    return result.getResultList();
  }
}
