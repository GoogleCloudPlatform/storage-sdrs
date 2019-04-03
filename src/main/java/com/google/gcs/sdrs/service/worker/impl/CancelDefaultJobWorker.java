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

package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.worker.rule.RuleExecutor;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class CancelDefaultJobWorker extends BaseWorker {

  private RetentionRule globalRuleToCancel;
  private String projectToUpdate;
  private RuleExecutor executor;
  RetentionJobDao jobDao = SingletonDao.getRetentionJobDao();

  private final Logger logger = LoggerFactory.getLogger(CancelDefaultJobWorker.class);

  public CancelDefaultJobWorker(RetentionRule globalRule, String projectId) {
    super();

    globalRuleToCancel = globalRule;
    projectToUpdate = projectId;
    executor = StsRuleExecutor.getInstance();
  }

  /** The function that will be executed when the worker is submitted */
  @Override
  public void doWork(){
    List<RetentionJob> jobs = jobDao
        .findJobsByRuleIdAndProjectId(globalRuleToCancel.getId(), projectToUpdate);
    if (jobs != null && jobs.size() > 0) {
      try{
        List<RetentionJob> cancelledJobs = executor.cancelDefaultJobs(jobs, globalRuleToCancel);
        for (RetentionJob job : cancelledJobs) {
          //TODO reassess if this is an update or a delete
          jobDao.update(job);
        }
        workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
      } catch (IOException ex) {
        logger.error(String.format("Error executing rule: %s", ex.getMessage()));
        workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
      }
    }
  }
}
