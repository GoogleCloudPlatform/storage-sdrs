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
 */

package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.RetentionRuleType;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import com.google.gcs.sdrs.service.worker.rule.RuleExecutor;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.util.RetentionUtil;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A worker class for executing dataset retention jobs */
public class ExecuteRetentionWorker extends BaseWorker {

  private final ExecutionEventRequest executionEvent;
  private final Logger logger = LoggerFactory.getLogger(ExecuteRetentionWorker.class);

  RetentionRuleDao retentionRuleDao = SingletonDao.getRetentionRuleDao();
  RetentionJobDao retentionJobDao = SingletonDao.getRetentionJobDao();
  RuleExecutor ruleExecutor;

  /**
   * The Execute Retention Worker construct
   *
   * @param executionEvent the {@link ExecutionEventRequest} to execute
   */
  public ExecuteRetentionWorker(ExecutionEventRequest executionEvent) {
    super();

    this.executionEvent = executionEvent;
    ruleExecutor = StsRuleExecutor.getInstance();
  }

  /** The function that will be executed when the worker is submitted */
  @Override
  public void doWork() {
    String dataStorageName = getDataStorageName(executionEvent.getTarget());
    String projectId = executionEvent.getProjectId();
    RetentionRule rule;
    try {
      switch (executionEvent.getExecutionEventType()) {
        case USER_COMMANDED:
          executeUserCommandedRule(dataStorageName, projectId);
          break;
        case POLICY:
          boolean dataStorageExists = !dataStorageName.isEmpty();
          boolean projectIdExists = projectId != null && !projectId.isEmpty();

          if (projectIdExists && dataStorageExists) {
            rule = retentionRuleDao.findDatasetRuleByBusinessKey(projectId, dataStorageName);
            if (rule != null) {
              List<RetentionRule> datasetRules = new ArrayList<>();
              datasetRules.add(rule);
              executeDatasetRules(datasetRules, projectId);
            } else {
              String message =
                  String.format(
                      "No policy found for target project: %s, target: %s",
                      projectId, dataStorageName);
              logger.error(message);
              throw new UnsupportedOperationException(message);
            }

          } else if (projectIdExists) {
            executePolicyByProject(executionEvent.getProjectId());
          } else {
            executePolicy();
          }
          break;
        default:
          logger.warn(
              String.format(
                  "%s is not a supported retention execution type",
                  executionEvent.getExecutionEventType().toString()));
      }
      workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
    } catch (IOException | IllegalArgumentException | UnsupportedOperationException | NullPointerException ex) {
      logger.error(String.format("Error executing rule: %s", ex.getMessage()));
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
  }

  private void executePolicy() throws IOException {
    List<String> projectIds = retentionRuleDao.getAllDatasetRuleProjectIds();
    for (String projectId : projectIds) {
      executePolicyByProject(projectId);
    }
  }

  private void executePolicyByProject(String projectId) throws IOException {
    List<RetentionRule> datasetRules = retentionRuleDao.findDatasetRulesByProjectId(projectId);
    List<RetentionRule> defaultRules = retentionRuleDao.findDefaultRulesByProjectId(projectId);
    RetentionRule globalDefaultRule = retentionRuleDao.findGlobalRuleByProjectId(projectId);

    List<RetentionJob> retentionJobs = ruleExecutor.executeDatasetRule(datasetRules, projectId);
    if (retentionJobs != null) {
      for (RetentionJob job : retentionJobs) {
        retentionJobDao.save(job);
      }
    }

    retentionJobs = ruleExecutor.executeDefaultRule(
            globalDefaultRule, defaultRules, datasetRules, atMidnight(), projectId);
    if (retentionJobs != null) {
      for (RetentionJob job : retentionJobs) {
        retentionJobDao.save(job);
      }
    }
  }

  private void executeDatasetRules(List<RetentionRule> rules, String projectId) throws IOException {
    List<RetentionJob> jobs = ruleExecutor.executeDatasetRule(rules, projectId);
    if (jobs != null) {
      for (RetentionJob job : jobs) {
        retentionJobDao.save(job);
      }
    }
  }

  private void executeUserCommandedRule(String target, String projectId) throws IOException {
    List<RetentionRule> userRules = new ArrayList<>();
    userRules.add(buildUserCommandedRule(target, projectId));
    List<RetentionJob> jobs = ruleExecutor.executeUserCommandedRule(userRules, projectId);
    if (jobs != null) {
      for (RetentionJob job : jobs) {
        retentionJobDao.save(job);
      }
    }
  }

  private RetentionRule buildUserCommandedRule(String target, String projectId) {
    RetentionRule rule = new RetentionRule();
    String dataStorageName = getDataStorageName(target);

    rule.setDataStorageName(dataStorageName);
    rule.setDatasetName(RetentionUtil.getDatasetPath(dataStorageName));
    rule.setProjectId(projectId);
    rule.setType(RetentionRuleType.USER);

    return rule;
  }

  private String getDataStorageName(String target) {
    if (target == null) {
      target = "";
    }

    return target;
  }

  private ZonedDateTime atMidnight() {
    return ZonedDateTime.now(Clock.systemUTC()).with(LocalTime.MIDNIGHT).plusDays(1);
  }
}
