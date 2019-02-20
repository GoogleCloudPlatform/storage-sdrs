package com.google.gcs.sdrs.dao.impl;

import com.google.gcs.sdrs.dao.RetentionJobValidationDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.enums.RetentionJobStatusType;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;
import org.hibernate.query.Query;
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

    // Every immediate RetentionJob with a RetentionJobValidation.status of pending
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.INNER);
    jobValidation.on(builder.equal(jobValidation.get("status"), RetentionJobStatusType.PENDING));
    query.where(builder.equal(job.get("retentionRuleType"), RetentionRuleType.MARKER));

    Query<RetentionJob> result = getCurrentSession().createQuery(query);
    results.addAll(result.getResultList());

    // Every immediate RetentionJob without a corresponding RetentionJobValidation record
    query = builder.createQuery(RetentionJob.class);
    job = query.from(RetentionJob.class);
    jobValidation = job.join("jobValidations", JoinType.LEFT);
    query.where(
        builder.equal(job.get("retentionRuleType"), RetentionRuleType.MARKER),
        builder.isNull(jobValidation.get("id")));

    result = getCurrentSession().createQuery(query);
    results.addAll(result.getResultList());

    // Every daily RetentionJob with a RetentionJobValidation.status of pending in the last 24h
    query = builder.createQuery(RetentionJob.class);
    job = query.from(RetentionJob.class);
    jobValidation = job.join("jobValidations", JoinType.INNER);
    jobValidation.on(builder.equal(jobValidation.get("status"), RetentionJobStatusType.PENDING));
    Date oneDayAgo = Date.valueOf(LocalDate.now().atStartOfDay().toLocalDate());
    query.where(
        builder.notEqual(job.get("retentionRuleType"), RetentionRuleType.MARKER),
        builder.greaterThanOrEqualTo(jobValidation.get("updatedAt"), oneDayAgo));

    result = getCurrentSession().createQuery(query);
    results.addAll(result.getResultList());

    // Every daily RetentionJob without a matching RetentionJobValidation record in the last 24h
    query = builder.createQuery(RetentionJob.class);
    job = query.from(RetentionJob.class);
    jobValidation = job.join("jobValidations", JoinType.LEFT);
    jobValidation.on(builder.greaterThanOrEqualTo(jobValidation.get("updatedAt"), oneDayAgo));
    query.where(
        builder.notEqual(job.get("retentionRuleType"), RetentionRuleType.MARKER),
        builder.isNull(jobValidation.get("id")));

    result = getCurrentSession().createQuery(query);
    results.addAll(result.getResultList());

    return new ArrayList<>(results);
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
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJobValidation> query = builder.createQuery(RetentionJobValidation.class);
    Root<RetentionJobValidation> root = query.from(RetentionJobValidation.class);

    query.where(root.get("jobOperationName").in(retentionJobNames));

    Query<RetentionJobValidation> result = getCurrentSession().createQuery(query);
    return result.getResultList();
  }
}
