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

import com.google.gcs.sdrs.JobManager.JobManager;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.worker.Worker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EventsServiceImplTest {

  private EventsServiceImpl service = new EventsServiceImpl();
  private List<String> projectIds = new ArrayList<>();
  private RetentionRule globalRule;

  @Before
  public void initialize(){
    globalRule = new RetentionRule();
    globalRule.setId(10);
    globalRule.setProjectId("global-default");
    globalRule.setDataStorageName("global");
    globalRule.setRetentionPeriodInDays(365);
    service.jobManager.shutDownJobManagerNow();
    service.jobManager = mock(JobManager.class);
    service.ruleDao = mock(RetentionRuleDao.class);
    service.jobDao = mock(RetentionJobDao.class);
    projectIds.add("projectId");
  }

  @Test
  public void GlobalRuleDoesNotExist() {
    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(null);

    service.createDefaultJobIfNonExistent();

    verifyZeroInteractions(service.jobManager);
  }

  @Test
  public void GlobalJobExistsForProject() {

    RetentionJob existingJob = new RetentionJob();

    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(service.jobDao.findJobByRuleIdAndProjectId(anyInt(), any())).thenReturn(existingJob);

    service.createDefaultJobIfNonExistent();

    verifyZeroInteractions(service.jobManager);
  }

  @Test
  public void GlobalJobDoesNotExistForProject() {
    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(service.jobDao.findJobByRuleIdAndProjectId(anyInt(), any())).thenReturn(null);

    service.createDefaultJobIfNonExistent();

    ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

    verify(service.jobManager).submitJob(workerCaptor.capture());
  }

}
