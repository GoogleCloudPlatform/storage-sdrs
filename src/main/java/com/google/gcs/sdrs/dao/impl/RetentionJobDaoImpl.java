package com.google.gcs.sdrs.dao.impl;

import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import org.hibernate.query.Query;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

public class RetentionJobDaoImpl extends GenericDao<RetentionJob, Integer>
    implements RetentionJobDao {

  public RetentionJobDaoImpl() {
    super(RetentionJob.class);
  }

  @Override
  public List<RetentionJob> findJobsByRuleId(int ruleId) {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> root = query.from(RetentionJob.class);

    query
        .select(root)
        .where(builder.equal(root.get("retentionRuleId"), ruleId));

    Query<RetentionJob> result = getCurrentSession().createQuery(query);
    return result.getResultList();
  }

  public RetentionJob findJobByRuleIdAndProjectId(int ruleId, String projectId) {
    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionJob> query = builder.createQuery(RetentionJob.class);
    Root<RetentionJob> root = query.from(RetentionJob.class);

    query
        .select(root)
        .where(builder.equal(root.get("retentionRuleId"), ruleId),
            builder.equal(root.get("retentionRuleProjectId"), projectId));


    return getSingleRecordWithCriteriaQuery(query);
  }
}
