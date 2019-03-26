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
import com.google.gcs.sdrs.service.rule.RuleExecutor;
import com.google.gcs.sdrs.service.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import com.google.gcs.sdrs.util.RetentionUtil;

import java.io.IOException;
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

  /** The Execute Retention Worker construct
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
    try{
      switch (executionEvent.getExecutionEventType()) {
        case USER_COMMANDED:
          rule = getEventDefinedRule();
          RetentionJob result = ruleExecutor.executeDatasetRule(rule);
          retentionJobDao.save(result);
          workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
          break;
        case POLICY:
          boolean dataStorageExists = !dataStorageName.isEmpty();
          boolean projectIdExists = projectId != null && !projectId.isEmpty();

          if (projectIdExists && dataStorageExists) {
            rule = retentionRuleDao.findDatasetRuleByBusinessKey(projectId, dataStorageName);
            if (rule != null) {
              RetentionJob job = ruleExecutor.executeDatasetRule(rule);
              retentionJobDao.save(job);
            } else {
              String message = String.format("No policy found for target project: %s, target: %s",
                  projectId, dataStorageName);
              logger.error(message);
              throw new UnsupportedOperationException(message);
            }

          } else if (projectIdExists) {
            executeAllDatasetRulesByProject(executionEvent.getProjectId());
          } else {
            executeAllDatasetRules();
          }
          workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
      }
    } catch (IOException ex) {
      logger.error(String.format("Error executing rule: %s", ex.getMessage()));
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
  }

  private void executeAllDatasetRules() throws IOException {
    List<String> projectIds = retentionRuleDao.getAllDatasetRuleProjectIds();
    for (String projectId : projectIds) {
      executeAllDatasetRulesByProject(projectId);
    }
  }

  private void executeAllDatasetRulesByProject(String projectId) throws IOException {
    List<RetentionRule> datasetRules = retentionRuleDao.findDatasetRulesByProjectId(projectId);
    for (RetentionRule rule : datasetRules) {
      RetentionJob job = ruleExecutor.executeDatasetRule(rule);
      retentionJobDao.save(job);
    }
  }

  private RetentionRule getEventDefinedRule() {
    RetentionRule rule = new RetentionRule();

    String dataStorageName = getDataStorageName(executionEvent.getTarget());

    rule.setDataStorageName(dataStorageName);
    rule.setDatasetName(RetentionUtil.getDatasetPath(dataStorageName));

    rule.setRetentionPeriodInDays(0);
    rule.setProjectId(executionEvent.getProjectId());
    rule.setType(RetentionRuleType.USER);

    return rule;
  }

  private String getDataStorageName(String target){
    if (target == null) {
      target = "";
    }

    return target;
  }
}
