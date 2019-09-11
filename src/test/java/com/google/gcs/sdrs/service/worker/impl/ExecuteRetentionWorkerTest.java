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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gcs.sdrs.common.ExecutionEventType;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import com.google.gcs.sdrs.service.worker.WorkerResult.WorkerResultStatus;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StsRuleExecutor.class)
@PowerMockIgnore("javax.management.*")
public class ExecuteRetentionWorkerTest {

  private StsRuleExecutor ruleExecutorMock;
  private RetentionRuleDao retentionRuleDaoMock;
  private RetentionJobDao retentionJobDaoMock;
  private String uuid;

  @Before
  public void setup() {
    retentionRuleDaoMock = mock(RetentionRuleDao.class);
    retentionJobDaoMock = mock(RetentionJobDao.class);
    ruleExecutorMock = mock(StsRuleExecutor.class);
    when(ruleExecutorMock.executeDatasetRule(any(), any())).thenReturn(null);
    PowerMockito.mockStatic(StsRuleExecutor.class);
    when(StsRuleExecutor.getInstance()).thenReturn(null);
    uuid = UUID.randomUUID().toString();
  }

  @Test
  public void doWorkSuccessfullyHandlesUserRequests() {
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(createBasicRequest(), uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    worker.doWork();

    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkRunsAllWhenPolicyOnly() {
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    request.setProjectId(null);
    request.setTarget(null);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request, uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;
    List<String> projectIds = new ArrayList<>();
    String projectId = "testproject";
    projectIds.add(projectId);
    List<RetentionJob> datasetJobs = new ArrayList<>();
    RetentionJob datasetJob = new RetentionJob();
    datasetJob.setName("datasetJob");
    datasetJobs.add(datasetJob);

    List<RetentionJob> defaultJobs = new ArrayList<>();
    RetentionJob defaultJob = new RetentionJob();
    defaultJob.setName("defaultJob");
    defaultJobs.add(datasetJob);

    List<RetentionRule> datasetRules = new ArrayList<>();
    List<RetentionRule> defaultRules = new ArrayList<>();
    RetentionRule globalDefaultRule = new RetentionRule();

    when(retentionRuleDaoMock.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(retentionRuleDaoMock.findDatasetRulesByProjectId(any())).thenReturn(datasetRules);
    when(retentionRuleDaoMock.findDefaultRulesByProjectId(any())).thenReturn(defaultRules);
    when(retentionRuleDaoMock.findGlobalRuleByProjectId(any())).thenReturn(globalDefaultRule);
    when(ruleExecutorMock.executeDefaultRule(any(), any(), any(), any(), any()))
        .thenReturn(defaultJobs);
    when(ruleExecutorMock.executeDatasetRule(any(), any())).thenReturn(datasetJobs);

    worker.doWork();

    verify(retentionRuleDaoMock).findDatasetRulesByProjectId(projectId);
    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkRunsProjectWhenSpecified() {
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    request.setTarget(null);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request, uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    List<String> projectIds = new ArrayList<>();
    List<RetentionJob> datasetJobs = new ArrayList<>();
    RetentionJob datasetJob = new RetentionJob();
    datasetJob.setName("datasetJob");
    datasetJobs.add(datasetJob);

    List<RetentionJob> defaultJobs = new ArrayList<>();
    RetentionJob defaultJob = new RetentionJob();
    defaultJob.setName("defaultJob");
    defaultJobs.add(datasetJob);

    List<RetentionRule> datasetRules = new ArrayList<>();
    List<RetentionRule> defaultRules = new ArrayList<>();
    RetentionRule globalDefaultRule = new RetentionRule();

    when(retentionRuleDaoMock.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(retentionRuleDaoMock.findDatasetRulesByProjectId(any())).thenReturn(datasetRules);
    when(retentionRuleDaoMock.findDefaultRulesByProjectId(any())).thenReturn(defaultRules);
    when(retentionRuleDaoMock.findGlobalRuleByProjectId(any())).thenReturn(globalDefaultRule);
    when(ruleExecutorMock.executeDefaultRule(any(), any(), any(), any(), any()))
        .thenReturn(defaultJobs);
    when(ruleExecutorMock.executeDatasetRule(any(), any())).thenReturn(datasetJobs);

    worker.doWork();

    verify(retentionRuleDaoMock).findDatasetRulesByProjectId(request.getProjectId());
    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkRunsRuleWhenProjectAndTargetSpecified() {
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request, uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;
    RetentionRule rule = new RetentionRule();
    when(retentionRuleDaoMock.findDatasetRuleByBusinessKey(any(), any())).thenReturn(rule);

    worker.doWork();

    verify(retentionRuleDaoMock)
        .findDatasetRuleByBusinessKey(request.getProjectId(), request.getTarget());
    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkErrorsWhenRuleNotFound() {
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request, uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;
    RetentionRule rule = new RetentionRule();
    when(retentionRuleDaoMock.findDatasetRuleByBusinessKey(any(), any())).thenReturn(null);

    worker.doWork();
    assertEquals(WorkerResultStatus.FAILED, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkSavesJobs() {
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request, uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    List<RetentionJob> retentionJobs = new ArrayList<>();
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName("retentionJob");
    retentionJobs.add(retentionJob);
    RetentionRule rule = new RetentionRule();
    when(retentionRuleDaoMock.findDatasetRuleByBusinessKey(any(), any())).thenReturn(rule);
    when(ruleExecutorMock.executeDatasetRule(any(), any())).thenReturn(retentionJobs);

    worker.doWork();

    verify(retentionJobDaoMock).save(any());
  }

  @Test
  public void callSetsEndTimeAndReturnsResult() {
    ExecutionEventRequest request = createBasicRequest();
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request, uuid);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    WorkerResult result = worker.call();

    assertNotNull(result.getEndTime());
  }

  private ExecutionEventRequest createBasicRequest() {
    ExecutionEventRequest request = new ExecutionEventRequest();
    request.setExecutionEventType(ExecutionEventType.USER_COMMANDED);
    request.setProjectId("someId");
    request.setTarget("gs://bucket/dataset");
    return request;
  }
}
