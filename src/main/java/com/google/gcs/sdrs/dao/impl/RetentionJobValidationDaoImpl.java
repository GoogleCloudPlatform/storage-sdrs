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

import com.google.gcs.sdrs.common.RetentionJobStatusType;
import com.google.gcs.sdrs.dao.RetentionJobValidationDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Hibernate based Generic Dao implementation */
public class RetentionJobValidationDaoImpl extends GenericDao<RetentionJobValidation, Integer>
    implements RetentionJobValidationDao {

  private static final Logger logger = LoggerFactory.getLogger(RetentionJobValidationDaoImpl.class);

  public RetentionJobValidationDaoImpl() {
    super(RetentionJobValidation.class);
  }

  /**
   * Get a Collection of {@link RetentionJob}s that are still in a pending state.
   *
   * <p>Immediately-run. Any immediately-run jobs in the retention_job but not in the
   * retention_job_validation or status is pending
   *
   * <p>Daily. Any daily job in the retention_job but does not have a record in the
   * retention_job_validation or status is pending for today
   *
   * @return a Collection of {@link RetentionJob}s
   */
  @Override
  public List<RetentionJob> findAllPendingRetentionJobs() {
    // Using a set means if a duplicate job is added from one of the query results, it will be
    // skipped
    Set<RetentionJob> results = new HashSet<>();

    results.addAll(findAllSingleRunPendingJobs());
    results.addAll(findAllSingleRunJobsWithNoStatus());
    return new ArrayList<>(results);
  }

  /**
   * Get all DATASET/USER RetentionJobs with a RetentionJobValidation.status of pending
   *
   * @return a list of RententionJob
   */
  private List<RetentionJob> findAllSingleRunPendingJobs() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.INNER);
    jobValidation.on(builder.equal(jobValidation.get("status"), RetentionJobStatusType.PENDING));
    List<RetentionJob> results = session.createQuery(query).getResultList();
    closeSession(session);
    return results;
  }

  /**
   * Get all DATASET/USER RetentionJobs without a corresponding RetentionJobValidation record
   *
   * @return a list of RententionJob
   */
  private List<RetentionJob> findAllSingleRunJobsWithNoStatus() {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.LEFT);
    query.where(builder.isNull(jobValidation.get("id")));
    List<RetentionJob> results = session.createQuery(query).getResultList();
    closeSession(session);
    return results;
  }

  /**
   * Get all of the retentionValidationJob objects that match the passed in list of job operation
   * names.
   *
   * @param retentionJobNames a list of jobOperationNames
   * @return a list of RetentionJobValidation objects
   */
  @Override
  public List<RetentionJobValidation> findAllByRetentionJobNames(List<String> retentionJobNames) {
    Session session = openSession();
    CriteriaBuilder builder = session.getCriteriaBuilder();
    CriteriaQuery<RetentionJobValidation> query = builder.createQuery(RetentionJobValidation.class);
    Root<RetentionJobValidation> root = query.from(RetentionJobValidation.class);

    query.where(root.get("jobOperationName").in(retentionJobNames));

    List<RetentionJobValidation> results = session.createQuery(query).getResultList();
    closeSession(session);
    return results;
  }
}
