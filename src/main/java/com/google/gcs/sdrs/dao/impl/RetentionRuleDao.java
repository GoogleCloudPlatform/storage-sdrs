package com.google.gcs.sdrs.dao.impl;

import java.io.Serializable;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.query.Query;

import com.google.gcs.sdrs.dao.model.RetentionRule;

public class RetentionRuleDao<T, Id extends Serializable> extends GenericDao {

  public RetentionRuleDao(final Class<T> type) {
    super(type);
    // TODO Auto-generated constructor stub
  }

  public RetentionRule findByBusinessKey(String project, String bucket, String dataSet) {

    CriteriaBuilder builder = openCurrentSession().getCriteriaBuilder();
    CriteriaQuery<RetentionRule> query = builder.createQuery(RetentionRule.class).distinct(true);
    Root<RetentionRule> root = query.from(RetentionRule.class);
    query
        .select(root)
        .where(builder.equal(root.get("projectId"), project))
        .where(builder.equal(root.get("dataStorageName"), bucket))
        .where(builder.equal(root.get("datasetName"), dataSet));

    Query<RetentionRule> result = getCurrentSession().createQuery(query);
    if (result.getResultList().isEmpty()) {
      return null;
    } else {
      return result.getResultList().get(0);
    }
  }
}
