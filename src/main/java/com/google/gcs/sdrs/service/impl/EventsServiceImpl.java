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

package com.google.gcs.sdrs.service.impl;

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.controller.pojo.NotificationEventRequest;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.manager.JobManager;
import com.google.gcs.sdrs.service.EventsService;
import com.google.gcs.sdrs.worker.Worker;
import com.google.gcs.sdrs.worker.impl.CreateDefaultJobWorker;
import com.google.gcs.sdrs.worker.impl.DeleteNotificationWorker;
import com.google.gcs.sdrs.worker.impl.ExecuteRetentionWorker;
import com.google.gcs.sdrs.worker.impl.ValidationWorker;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service implementation for event related behaviors. */
public class EventsServiceImpl implements EventsService {

  JobManager jobManager;

  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();
  RetentionJobDao jobDao = SingletonDao.getRetentionJobDao();
  private static final String DEFAULT_PROJECT_ID = "global-default";
  private static String defaultProjectId;

  private static final Logger logger = LoggerFactory.getLogger(EventsServiceImpl.class);

  public EventsServiceImpl(){
    defaultProjectId = SdrsApplication.getAppConfigProperty(
        "sts.defaultProjectId",
        DEFAULT_PROJECT_ID);

    jobManager = JobManager.getInstance();
  }

  @Override
  public void processExecutionEvent(ExecutionEventRequest request) {
    Worker worker = new ExecuteRetentionWorker(request);
    jobManager.submitJob(worker);
    createDefaultJobIfNonExistent();
  }

  /**
   * Submits a validation job to the JobManager.
   */
  @Override
  public void processValidationEvent() {
    Worker worker = new ValidationWorker();
    jobManager.submitJob(worker);
  }

  /**
   * Submits a notification job to the JobManager
   *
   * @param request
   * @param correlationId
   */
  @Override
  public void processDeleteNotificationEvent(NotificationEventRequest request,
                                             String correlationId) {
    Worker worker = new DeleteNotificationWorker(request, correlationId);
    JobManager.getInstance().submitJob(worker);
  }

  void createDefaultJobIfNonExistent(){
    RetentionRule globalRule = ruleDao.findGlobalRuleByProjectId(defaultProjectId);

    //check that a global default rule exists
    if (globalRule != null) {
      List<String> projectIds = ruleDao.getAllDatasetRuleProjectIds();

      // check if a global default job exists within each project
      for (String projectId : projectIds) {
        List<RetentionJob> globalJobs = jobDao.findJobsByRuleIdAndProjectId(globalRule.getId(), projectId);

        // if the jobs don't exist, create them
        if (globalJobs == null || globalJobs.size() == 0) {
          logger.info(String.format(
              "Job doesn't exist for global rule id: %s, projectId: %s. Creating now...",
              globalRule.getId(),
              projectId));
          Worker createDefaultWorker = new CreateDefaultJobWorker(globalRule, projectId);
          jobManager.submitJob(createDefaultWorker);
        }
      }
    }
  }
}
