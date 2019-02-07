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
import com.google.gcs.sdrs.service.RuleExecutor;
import com.google.gcs.sdrs.service.impl.StsRuleExecutor;
import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.WorkerResult;
import java.util.Collection;
import java.util.HashSet;

public class ExecuteRetentionWorker extends BaseWorker {

  private final ExecutionEventRequest executionEvent;

  RetentionRuleDao retentionRuleDao = SingletonDao.getRetentionRuleDao();
  Dao<RetentionJob, Integer> retentionJobDao = SingletonDao.getRetentionJobDao();
  // TODO: dependency injection -- but there isn't a pressing need to use singletons here yet
  RuleExecutor ruleExecutor = new StsRuleExecutor();

  public ExecuteRetentionWorker(ExecutionEventRequest executionEvent) {
    this.executionEvent = executionEvent;
  }

  @Override
  public void doWork() {
    Collection<RetentionRule> rulesToExecute = new HashSet<>();
    switch (executionEvent.getExecutionEventType()) {
      case USER_COMMANDED:
        rulesToExecute.add(getEventDefinedRule());
        break;
      case POLICY:
        if (executionEvent.getProjectId() == null) {
          String[] dataStorageAndDataset = extractDataStorageAndDataset();
          rulesToExecute.addAll(
              retentionRuleDao.getAllByDataStorageAndDataset(
                  dataStorageAndDataset[0], dataStorageAndDataset[1]));
        } else {
          rulesToExecute.add(getEventDefinedRule());
        }
        break;
      default:
        throw new UnsupportedOperationException("Unknown execution event type");
    }

    for (RetentionRule rule : rulesToExecute) {
      RetentionJob job = ruleExecutor.execute(rule);
      retentionJobDao.save(job);
    }

    workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
  }

  private RetentionRule getEventDefinedRule() {
    RetentionRule rule = new RetentionRule();

    String[] dataStorageAndDataset = extractDataStorageAndDataset();

    rule.setDataStorageName(dataStorageAndDataset[0]);
    rule.setDatasetName(dataStorageAndDataset[1]);
    rule.setProjectId(executionEvent.getProjectId());
    rule.setType(RetentionRuleType.DATASET);

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
