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
 *
 */

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

/** A worker class for updating external instances of retention jobs */
public class UpdateDefaultJobWorker extends BaseWorker {

  private RetentionRule globalRuleToUpdate;
  private String projectToUpdate;
  private RuleExecutor executor;
  RetentionJobDao jobDao = SingletonDao.getRetentionJobDao();
  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();

  private final Logger logger = LoggerFactory.getLogger(UpdateDefaultJobWorker.class);

  /**
   * A constructor for the External Job Update Worker
   * @param globalRule the global rule that needs to be updated
   * @param projectId the project ID where the global rule needs to be updated
   */
  public UpdateDefaultJobWorker(RetentionRule globalRule, String projectId) {
    super();

    globalRuleToUpdate = globalRule;
    projectToUpdate = projectId;
    executor = StsRuleExecutor.getInstance();
  }

  /** The function that will be executed when the worker is submitted */
  @Override
  public void doWork(){
    List<RetentionJob> defaultJobs = jobDao
        .findJobsByRuleIdAndProjectId(globalRuleToUpdate.getId(), projectToUpdate);
    if (defaultJobs.size() > 0) {
      List<RetentionRule> childRules = ruleDao.findDatasetRulesByProjectId(projectToUpdate);
      try{
        List<RetentionJob> executedJobs = executor.updateDefaultRule(
            defaultJobs, globalRuleToUpdate, childRules);
        for (RetentionJob retentionJob : executedJobs) {
          jobDao.update(retentionJob);
        }
        workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
      } catch (IOException ex) {
        logger.error(String.format("Error executing rule: %s", ex.getMessage()));
        workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
      }
    }
  }
}
