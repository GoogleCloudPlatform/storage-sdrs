package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.rule.RuleExecutor;
import com.google.gcs.sdrs.service.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/** A worker definition for creating a default job */
public class CreateDefaultJobWorker extends BaseWorker {

  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();
  RetentionJobDao retentionJobDao = SingletonDao.getRetentionJobDao();

  private RetentionRule globalRuleToUpdate;
  private String projectToUpdate;
  private RuleExecutor executor;
  private ZoneId executionTimezone;

  private final String DEFAULT_TIMEZONE = "America/Los_Angeles";
  private final Logger logger = LoggerFactory.getLogger(UpdateDefaultJobWorker.class);

  /**
   * A constructor for the Create Default Job Worker
   * @param globalRule the global rule that needs to be created
   * @param projectId the project ID where the global rule needs to be created
   */
  public CreateDefaultJobWorker(RetentionRule globalRule, String projectId) {
    super();

    executionTimezone = ZoneId.of(SdrsApplication.getAppConfigProperty(
        "scheduler.task.ruleExecution.timezone",
        DEFAULT_TIMEZONE));

    globalRuleToUpdate = globalRule;
    projectToUpdate = projectId;
    executor = StsRuleExecutor.getInstance();
  }

  /** The function that will be executed when the worker is submitted */
  @Override
  public void doWork(){
    List<RetentionRule> childRules = ruleDao.findDatasetRulesByProjectId(projectToUpdate);
    try{
      List<RetentionJob> retentionJobs = executor.executeDefaultRule(globalRuleToUpdate, childRules, atMidnight());
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
    ZonedDateTime midnight = LocalDate.now().atStartOfDay().plusDays(1).atZone(executionTimezone);
    return midnight;
  }
}
