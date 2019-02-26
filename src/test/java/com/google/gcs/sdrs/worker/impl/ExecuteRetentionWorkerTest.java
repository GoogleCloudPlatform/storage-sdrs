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
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.impl.GenericDao;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.ExecutionEventType;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import com.google.gcs.sdrs.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.worker.WorkerResult;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExecuteRetentionWorkerTest {

  private StsRuleExecutor ruleExecutorMock;
  private RetentionRuleDao retentionRuleDaoMock;
  private RetentionJobDao retentionJobDaoMock;

  @Before
  public void setup() throws IOException {
    retentionRuleDaoMock = mock(RetentionRuleDao.class);
    retentionJobDaoMock = mock(RetentionJobDao.class);
    ruleExecutorMock = mock(StsRuleExecutor.class);
    when(ruleExecutorMock.executeDatasetRule(any())).thenReturn(null);
  }

  @Test
  public void doWorkSuccessfullyHandlesUserRequests() {
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(createBasicRequest());
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    worker.doWork();

    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkRunsAllWhenPolicyOnly(){
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    request.setProjectId(null);
    request.setTarget(null);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;
    List<String> projectIds = new ArrayList<>();
    String projectId = "testproject";
    projectIds.add(projectId);
    when(retentionRuleDaoMock.getAllDatasetRuleProjectIds()).thenReturn(projectIds);

    worker.doWork();

    verify(retentionRuleDaoMock).findDatasetRulesByProjectId(projectId);
    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkRunsProjectWhenSpecified(){
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    request.setTarget(null);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    worker.doWork();

    verify(retentionRuleDaoMock).findDatasetRulesByProjectId(request.getProjectId());
    assertEquals(WorkerResult.WorkerResultStatus.SUCCESS, worker.getWorkerResult().getStatus());
  }

  @Test
  public void doWorkRunsRuleWhenProjectAndTargetSpecified(){
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request);
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

  @Test(expected = UnsupportedOperationException.class)
  public void doWorkErrorsWhenRuleNotFound(){
    ExecutionEventRequest request = createBasicRequest();
    request.setExecutionEventType(ExecutionEventType.POLICY);
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;
    RetentionRule rule = new RetentionRule();
    when(retentionRuleDaoMock.findDatasetRuleByBusinessKey(any(), any())).thenReturn(null);

    worker.doWork();
  }

  @Test
  public void doWorkSavesJobs() {
    ExecutionEventRequest request = createBasicRequest();
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request);
    worker.ruleExecutor = ruleExecutorMock;
    worker.retentionJobDao = retentionJobDaoMock;
    worker.retentionRuleDao = retentionRuleDaoMock;

    worker.doWork();

    verify(retentionJobDaoMock).save(any());
  }

  @Test
  public void callSetsEndTimeAndReturnsResult() {
    ExecutionEventRequest request = createBasicRequest();
    ExecuteRetentionWorker worker = new ExecuteRetentionWorker(request);
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
