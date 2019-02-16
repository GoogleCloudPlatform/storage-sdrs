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
import com.google.gcs.sdrs.dao.Dao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import com.google.gcs.sdrs.rule.RuleExecutor;
import com.google.gcs.sdrs.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.util.RetentionUtil;
import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.WorkerResult;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecuteRetentionWorker extends BaseWorker {

  private final ExecutionEventRequest executionEvent;
  private final Logger logger = LoggerFactory.getLogger(ExecuteRetentionWorker.class);
  private final ZoneId DEFAULT_TIMEZONE = ZoneId.of("America/Los_Angeles");
  private ZoneId executionTimezone;

  RetentionRuleDao retentionRuleDao = SingletonDao.getRetentionRuleDao();
  Dao<RetentionJob, Integer> retentionJobDao = SingletonDao.getRetentionJobDao();
  RuleExecutor ruleExecutor;

  public ExecuteRetentionWorker(ExecutionEventRequest executionEvent) {
    super();

    this.executionEvent = executionEvent;

    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      executionTimezone = ZoneId.of(config.getString("scheduler.task.ruleExecution.timezone"));
    } catch (ConfigurationException ex) {
      logger.error(
          String.format(
              "Configuration could not be read. Using default values: %s", ex.getMessage()));
      executionTimezone = DEFAULT_TIMEZONE;
    }

    ruleExecutor = StsRuleExecutor.getInstance();
  }

  @Override
  public void doWork() {
    String dataStorageName = getDataStorageName(executionEvent.getTarget());
    RetentionRule rule;
    switch (executionEvent.getExecutionEventType()) {
      case USER_COMMANDED:
        rule = getEventDefinedRule();
        break;
      case POLICY:
        // If the projectId is provided, the target can be assumed to be a Dataset rule.
        if (executionEvent.getProjectId() != null) {
          rule =
              retentionRuleDao.findDatasetRuleByBusinessKey(
                  executionEvent.getProjectId(),
                  dataStorageName);
        } else {
          rule = retentionRuleDao.findGlobalRuleByTarget(dataStorageName);
        }

        if (rule == null) {
          workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
          throw new UnsupportedOperationException("No rule found matching description");
        }
        break;
      default:
        workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
        throw new UnsupportedOperationException("Unknown execution event type");
    }

    try {
      RetentionJob job;
      switch (rule.getType()) {
        case DATASET:
          job = ruleExecutor.executeDatasetRule(rule);
          break;
        case GLOBAL:
          Collection<RetentionRule> rules =
              retentionRuleDao.findAllDatasetRulesInDataStorage(dataStorageName);

          job = ruleExecutor.executeDefaultRule(rule, rules, atMidnight());
          break;
        default:
          workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
          throw new UnsupportedOperationException("Unknown retention rule type");
      }
      retentionJobDao.save(job);
      workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
    } catch (IOException exception) {
      logger.error(String.format("Error executing rule: %s", exception.getMessage()));
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
  }

  private ZonedDateTime atMidnight() {
    ZonedDateTime midnight = LocalDate.now().atStartOfDay().plusDays(1).atZone(executionTimezone);
    return midnight;
  }

  private RetentionRule getEventDefinedRule() {
    RetentionRule rule = new RetentionRule();

    String dataStorageName = getDataStorageName(executionEvent.getTarget());

    rule.setDataStorageName(dataStorageName);
    rule.setDatasetName(RetentionUtil.getDatasetPath(dataStorageName));

    rule.setRetentionPeriodInDays(0);
    rule.setProjectId(executionEvent.getProjectId());
    rule.setType(RetentionRuleType.DATASET);

    return rule;
  }

  private String getDataStorageName(String target){
    if (target == null) {
      target = "";
    }

    return target;
  }
}
