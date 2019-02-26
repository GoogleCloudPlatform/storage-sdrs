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

    results.addAll(findAllSingleRunPendingJobs());
    results.addAll(findAllSingleRunJobsWithNoStatus());
    results.addAll(findAllDailyPendingJobs());
    results.addAll(findAllDailyJobsWithNoStatus());

    return new ArrayList<>(results);
  }

  /**
   * Get all DATASET/USER RetentionJobs with a RetentionJobValidation.status of pending
   *
   * @return a list of RententionJob
   */
  private List<RetentionJob> findAllSingleRunPendingJobs() {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.INNER);
    jobValidation.on(builder.equal(jobValidation.get("status"), RetentionJobStatusType.PENDING));
    query.where(builder.notEqual(job.get("retentionRuleType"), RetentionRuleType.GLOBAL));
    List<RetentionJob> results = getCurrentSession().createQuery(query).getResultList();
    closeCurrentSession();
    return results;
  }

  /**
   * Get all DATASET/USER RetentionJobs without a corresponding RetentionJobValidation record
   *
   * @return a list of RententionJob
   */
  private List<RetentionJob> findAllSingleRunJobsWithNoStatus() {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.LEFT);
    query.where(
        builder.notEqual(job.get("retentionRuleType"), RetentionRuleType.GLOBAL),
        builder.isNull(jobValidation.get("id")));
    List<RetentionJob> results = getCurrentSession().createQuery(query).getResultList();
    closeCurrentSession();
    return results;
  }

  /**
   * Get all GLOBAL RetentionJobs with a RetentionJobValidation.status of pending in the last 24h
   *
   * @return a list of RententionJob
   */
  private List<RetentionJob> findAllDailyPendingJobs() {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.INNER);
    jobValidation.on(builder.equal(jobValidation.get("status"), RetentionJobStatusType.PENDING));
    Date oneDayAgo = Date.valueOf(LocalDate.now().atStartOfDay().toLocalDate());
    query.where(
        builder.equal(job.get("retentionRuleType"), RetentionRuleType.GLOBAL),
        builder.greaterThanOrEqualTo(jobValidation.get("updatedAt"), oneDayAgo));
    List<RetentionJob> results = getCurrentSession().createQuery(query).getResultList();
    closeCurrentSession();
    return results;
  }

  /**
   * Get all GLOBAL RetentionJobs without a matching RetentionJobValidation record in the last 24h
   *
   * @return a list of RententionJob
   */
  private List<RetentionJob> findAllDailyJobsWithNoStatus() {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> job = query.from(RetentionJob.class);
    Join<RetentionJob, RetentionJobValidation> jobValidation =
        job.join("jobValidations", JoinType.LEFT);
    Date oneDayAgo = Date.valueOf(LocalDate.now().atStartOfDay().toLocalDate());
    jobValidation.on(builder.greaterThanOrEqualTo(jobValidation.get("updatedAt"), oneDayAgo));
    query.where(
        builder.equal(job.get("retentionRuleType"), RetentionRuleType.GLOBAL),
        builder.isNull(jobValidation.get("id")));
    List<RetentionJob> results = getCurrentSession().createQuery(query).getResultList();
    closeCurrentSession();
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
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJobValidation> query = builder.createQuery(RetentionJobValidation.class);
    Root<RetentionJobValidation> root = query.from(RetentionJobValidation.class);

    query.where(root.get("jobOperationName").in(retentionJobNames));

    List<RetentionJobValidation> results = getCurrentSession().createQuery(query).getResultList();
    closeCurrentSession();
    return results;
  }
}
