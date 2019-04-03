package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import com.google.gcs.sdrs.service.worker.rule.RuleExecutor;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A worker definition for creating a default job */
public class CreateDefaultJobWorker extends BaseWorker {

  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();
  RetentionJobDao retentionJobDao = SingletonDao.getRetentionJobDao();

  private RetentionRule globalRuleToUpdate;
  private String projectToUpdate;
  private RuleExecutor executor;

  private final Logger logger = LoggerFactory.getLogger(UpdateDefaultJobWorker.class);

  /**
   * A constructor for the Create Default Job Worker
   *
   * @param globalRule the global rule that needs to be created
   * @param projectId the project ID where the global rule needs to be created
   */
  public CreateDefaultJobWorker(RetentionRule globalRule, String projectId) {
    super();

    globalRuleToUpdate = globalRule;
    projectToUpdate = projectId;
    executor = StsRuleExecutor.getInstance();
  }

  /** The function that will be executed when the worker is submitted */
  @Override
  public void doWork() {
    List<RetentionRule> childRules = ruleDao.findDatasetRulesByProjectId(projectToUpdate);
    List<RetentionRule> defaultRules = ruleDao.findDefaultRulesByProjectId(projectToUpdate);
    RetentionRule globalDefaultRule = ruleDao.findGlobalRuleByProjectId(projectToUpdate);
    try {
      List<RetentionJob> retentionJobs =
          executor.executeDefaultRule(
              globalDefaultRule, defaultRules, childRules, atMidnight(), projectToUpdate);
      for (RetentionJob retentionJob : retentionJobs) {
        retentionJobDao.save(retentionJob);
      }
      workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
    } catch (IOException ex) {
      logger.error(String.format("Error creating global job: %s", ex.getMessage()));
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
  }

  private ZonedDateTime atMidnight() {
    return ZonedDateTime.now(Clock.systemUTC()).with(LocalTime.MIDNIGHT).plusDays(1);
  }
}
