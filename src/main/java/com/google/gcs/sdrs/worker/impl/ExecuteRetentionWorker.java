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

package com.google.gcs.sdrs.worker.impl;

import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.dao.Dao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import com.google.gcs.sdrs.rule.RuleExecutor;
import com.google.gcs.sdrs.rule.StsRuleExecutor;
import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.WorkerResult;
import java.io.IOException;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteRetentionWorker extends BaseWorker {

  private final ExecutionEventRequest executionEvent;
  private final Logger logger = LoggerFactory.getLogger(ExecuteRetentionWorker.class);

  RetentionRuleDao retentionRuleDao = SingletonDao.getRetentionRuleDao();
  Dao<RetentionJob, Integer> retentionJobDao = SingletonDao.getRetentionJobDao();
  RuleExecutor ruleExecutor;

  public ExecuteRetentionWorker(ExecutionEventRequest executionEvent) {
    super();

    this.executionEvent = executionEvent;

    try {
      // TODO: dependency injection -- but there isn't a pressing need to avoid instantiation here.
      ruleExecutor = new StsRuleExecutor();
    } catch (IOException exception) {
      logger.error("Unable to create StsRuleExecutor");
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
  }

  @Override
  public void doWork() {

    RetentionRule eventDefinedRule = getEventDefinedRule();
    try {
      RetentionJob job;
      switch (eventDefinedRule.getType()) {
        case DATASET:
          job = ruleExecutor.executeDatasetRule(eventDefinedRule);
          break;
        case GLOBAL:
          String[] dataStorageAndDataset = extractDataStorageAndDataset();
          Collection<RetentionRule> rules =
              retentionRuleDao.getAllByDataStorageAndDataset(
                  dataStorageAndDataset[0], dataStorageAndDataset[1]);

          // TODO: scheduled time unknown
          job = ruleExecutor.executeDefaultRule(eventDefinedRule, rules, null);
          break;
        default:
          workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
          throw new UnsupportedOperationException("Unknown retention rule type");
      }
      retentionJobDao.save(job);
      workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
    } catch (IOException exception) {
      logger.error(String.format("Error executing rule %s", eventDefinedRule.getId()));
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
  }

  private RetentionRule getEventDefinedRule() {
    RetentionRule rule = new RetentionRule();

    String[] dataStorageAndDataset = extractDataStorageAndDataset();
    rule.setDataStorageName(dataStorageAndDataset[0]);
    rule.setDatasetName(dataStorageAndDataset[1]);

    rule.setProjectId(executionEvent.getProjectId());

    RetentionRuleType ruleType =
        executionEvent.getProjectId() == null
            ? RetentionRuleType.DATASET
            : RetentionRuleType.GLOBAL;
    rule.setType(ruleType);

    return rule;
  }

  private String[] extractDataStorageAndDataset() {
    String target = executionEvent.getTarget();
    String removedPrefix = target.substring(ValidationConstants.STORAGE_PREFIX.length());
    String[] dataStorageAndDataset = removedPrefix.split(ValidationConstants.STORAGE_SEPARATOR, 2);
    dataStorageAndDataset[0] = ValidationConstants.STORAGE_PREFIX + dataStorageAndDataset[0];
    return dataStorageAndDataset;
  }
}
