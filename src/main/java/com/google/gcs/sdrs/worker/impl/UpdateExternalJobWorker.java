package com.google.gcs.sdrs.worker.impl;

import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.rule.RuleExecutor;
import com.google.gcs.sdrs.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.WorkerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * A worker class for updating external instances of retention jobs
 */
public class UpdateExternalJobWorker extends BaseWorker {

  private RetentionRule globalRuleToUpdate;
  private String projectToUpdate;
  private RuleExecutor executor;
  RetentionJobDao jobDao = SingletonDao.getRetentionJobDao();
  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();

  private final Logger logger = LoggerFactory.getLogger(UpdateExternalJobWorker.class);

  /**
   * A constructor for the External Job Update Worker
   * @param globalRule the global rule that needs to be updated
   * @param projectId the project ID where the global rule needs to be updated
   */
  public UpdateExternalJobWorker(RetentionRule globalRule, String projectId) {
    super();

    globalRuleToUpdate = globalRule;
    projectToUpdate = projectId;
    executor = StsRuleExecutor.getInstance();
  }

  /**
   * The function that will be existed when the worker is submitted
   */
  public void doWork(){
    RetentionJob job = jobDao
        .findJobByRuleIdAndProjectId(globalRuleToUpdate.getId(), projectToUpdate);
    if (job != null) {
      List<RetentionRule> childRules = ruleDao.findDatasetRulesByProjectId(projectToUpdate);
      try{
        executor.updateDefaultRule(job, globalRuleToUpdate, childRules);
        workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
      } catch (IOException ex) {
        logger.error(String.format("Error executing rule: %s", ex.getMessage()));
        workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
      }
    }
  }
}
