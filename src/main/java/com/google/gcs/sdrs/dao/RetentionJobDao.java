package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.RetentionJob;
import java.util.List;

public interface RetentionJobDao extends Dao<RetentionJob, Integer> {

  List<RetentionJob> findJobsByRuleId(int ruleId);

  List<RetentionJob> findJobsByRuleIdAndProjectId(int ruleId, String projectId);

  RetentionJob findLatestDefaultJob(String dataStroageName);
}
