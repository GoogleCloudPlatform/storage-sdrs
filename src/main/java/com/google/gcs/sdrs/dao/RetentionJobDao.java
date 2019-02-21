package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.RetentionJob;
import java.util.List;

public interface RetentionJobDao extends Dao<RetentionJob, Integer> {

  List<RetentionJob> findJobsByRuleId(int ruleId);

  RetentionJob findJobByRuleIdAndProjectId(int ruleId, String projectId);
}
