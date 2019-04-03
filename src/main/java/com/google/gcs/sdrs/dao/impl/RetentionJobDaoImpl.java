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
 *
 */

package com.google.gcs.sdrs.dao.impl;

import com.google.gcs.sdrs.RetentionRuleType;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.hibernate.query.Query;

public class RetentionJobDaoImpl extends GenericDao<RetentionJob, Integer>
    implements RetentionJobDao {

  public RetentionJobDaoImpl() {
    super(RetentionJob.class);
  }

  @Override
  public List<RetentionJob> findJobsByRuleId(int ruleId) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionJob> criteria = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> root = criteria.from(RetentionJob.class);

    criteria.select(root).where(builder.equal(root.get("retentionRuleId"), ruleId));

    Query<RetentionJob> query = session.createQuery(criteria);
    List<RetentionJob> result = query.getResultList();
    closeSession(session);
    return result;
  }

  public List<RetentionJob> findJobsByRuleIdAndProjectId(int ruleId, String projectId) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionJob> criteria = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> root = criteria.from(RetentionJob.class);

    criteria
        .select(root)
        .where(
            builder.equal(root.get("retentionRuleId"), ruleId),
            builder.equal(root.get("retentionRuleProjectId"), projectId));

    Query<RetentionJob> query = session.createQuery(criteria);
    List<RetentionJob> result = query.getResultList();
    closeSession(session);
    return result;
  }

  @Override
  public RetentionJob findLatestDefaultJob(String dataStroageName) {
    RetentionJob retentionJob = null;
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionJob> criteria = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> root = criteria.from(RetentionJob.class);
    criteria
        .select(root)
        .where(
            builder.equal(root.get("retentionRuleDataStorageName"), dataStroageName),
            builder.or(
                builder.equal(root.get("retentionRuleType"), RetentionRuleType.DEFAULT),
                builder.equal(root.get("retentionRuleType"), RetentionRuleType.GLOBAL)))
        .orderBy(builder.desc(root.get("createdAt")));

    Query<RetentionJob> query = session.createQuery(criteria);
    List<RetentionJob> result = query.getResultList();
    if (!result.isEmpty()) {
      retentionJob = result.get(0);
    }

    return retentionJob;
  }
}
