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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gcs.sdrs.RetentionRuleType;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.dao.impl.RetentionRuleDaoImpl;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.manager.JobManager;
import com.google.gcs.sdrs.service.worker.Worker;
import com.google.gcs.sdrs.service.worker.impl.CancelDefaultJobWorker;
import com.google.gcs.sdrs.service.worker.impl.UpdateDefaultJobWorker;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StsRuleExecutor.class)
public class RetentionRulesServiceImplTest {

  private RetentionRulesServiceImpl service = new RetentionRulesServiceImpl();
  private RetentionRule globalRule;
  private List<String> projectIds = new ArrayList<>();

  @Before
  public void setup() {
    service.ruleDao = mock(RetentionRuleDaoImpl.class);
    service.jobManager.shutDownJobManagerNow();
    service.jobManager = mock(JobManager.class);
    globalRule = new RetentionRule();
    globalRule.setId(10);
    globalRule.setProjectId("global-default");
    globalRule.setDataStorageName("global");
    globalRule.setRetentionPeriodInDays(365);
    String projectId = "test";
    projectIds.add(projectId);

    PowerMockito.mockStatic(StsRuleExecutor.class);
    when(StsRuleExecutor.getInstance()).thenReturn(null);
  }

  @Test
  public void createRulePersistsDatasetEntity() throws SQLException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDatasetName("dataset");
    createRule.setDataStorageName("gs://b/d");
    createRule.setProjectId("projectId");

    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(service.ruleDao.save(any())).thenReturn(1);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

    verify(service.ruleDao).save(captor.capture());
    // *** eshenlog ***
    // verify(service.jobManager).submitJob(workerCaptor.capture());
    RetentionRule input = captor.getValue();
    assertEquals(1, (int) input.getId());
    assertEquals(RetentionRuleType.DATASET, input.getType());
    assertEquals(123, (int) input.getRetentionPeriodInDays());
    assertEquals(true, input.getIsActive());
    assertEquals("projectId", input.getProjectId());
    assertEquals("gs://b/d", input.getDataStorageName());
    assertEquals("dataset", input.getDatasetName());
    assertEquals(1, (int) input.getVersion());
  }

  @Test
  public void createRuleUsesBucketForDatasetWhenNoDataset() throws SQLException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDataStorageName("gs://b");
    createRule.setProjectId("projectId");

    when(service.ruleDao.save(any())).thenReturn(1);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.ruleDao).save(captor.capture());
    RetentionRule input = captor.getValue();
    assertEquals("gs://b", input.getDataStorageName());
    assertEquals("b", input.getDatasetName());
  }

  @Test
  public void createRuleUsesDataStorageDatasetForDataset() throws SQLException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDataStorageName("gs://b/d");
    createRule.setProjectId("projectId");

    when(service.ruleDao.save(any())).thenReturn(1);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.ruleDao).save(captor.capture());
    RetentionRule input = captor.getValue();
    assertEquals("gs://b/d", input.getDataStorageName());
    assertEquals("d", input.getDatasetName());
  }

  @Test
  public void createRulePersistsGlobalEntity() throws SQLException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.GLOBAL);
    createRule.setRetentionPeriod(123);

    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(service.ruleDao.save(any())).thenReturn(1);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

    verify(service.ruleDao).save(captor.capture());
    // *** eshenlog
    // verify(service.jobManager).submitJob(workerCaptor.capture());
    RetentionRule input = captor.getValue();
    assertEquals(1, (int) input.getId());
    assertEquals(RetentionRuleType.GLOBAL, input.getType());
    assertEquals(123, (int) input.getRetentionPeriodInDays());
    assertEquals(true, input.getIsActive());
    assertEquals("global-default", input.getProjectId());
    assertEquals(1, (int) input.getVersion());
    assertEquals("global", input.getDataStorageName());
    assertNull(input.getDatasetName());
  }

  @Test
  public void createRuleOverwritesDeletedEntity() throws SQLException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDatasetName("dataset");
    createRule.setDataStorageName("gs://b/d");
    createRule.setProjectId("projectId");
    RetentionRule existingRule = new RetentionRule();
    existingRule.setDataStorageName("dataStorageName");
    existingRule.setProjectId("projectId");
    existingRule.setRetentionPeriodInDays(1);
    existingRule.setIsActive(false);
    existingRule.setVersion(2);
    when(service.ruleDao.findByBusinessKey("projectId", "gs://b/d", true)).thenReturn(existingRule);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.ruleDao).update(captor.capture());
    RetentionRule input = captor.getValue();
    assertNull(input.getId());
    assertEquals(RetentionRuleType.DATASET, input.getType());
    assertEquals(123, (int) input.getRetentionPeriodInDays());
    assertEquals(true, input.getIsActive());
    assertEquals(input.getProjectId(), "projectId");
    assertEquals(input.getDataStorageName(), "gs://b/d");
    assertEquals(input.getDatasetName(), "dataset");
    assertEquals(3, (int) input.getVersion());
  }

  @Test
  public void getRuleByBusinessKeyReturnsMappedValues() {
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(2);
    existingRule.setRetentionPeriodInDays(12);
    existingRule.setProjectId("projectId");
    existingRule.setDataStorageName("gs://bucket");
    when(service.ruleDao.findByBusinessKey(anyString(), anyString())).thenReturn(existingRule);

    RetentionRuleResponse result = service.getRetentionRuleByBusinessKey("any", "any");
    assertEquals(12, (int) result.getRetentionPeriod());
    assertEquals("projectId", result.getProjectId());
    assertEquals("gs://bucket", result.getDataStorageName());
  }

  @Test(expected = EntityNotFoundException.class)
  public void getRuleByBusinessKeyThrowsErrorWhenNull() {
    when(service.ruleDao.findByBusinessKey(anyString(), anyString())).thenReturn(null);

    service.getRetentionRuleByBusinessKey("any", "any");
  }

  @Test
  public void updateDatasetRuleFetchesAndUpdatesEntity() throws SQLException {
    RetentionRuleUpdateRequest request = new RetentionRuleUpdateRequest();
    request.setRetentionPeriod(123);
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(2);
    existingRule.setRetentionPeriodInDays(12);
    existingRule.setVersion(3);
    existingRule.setType(RetentionRuleType.DATASET);
    when(service.ruleDao.findById(2)).thenReturn(existingRule);

    RetentionRuleResponse result = service.updateRetentionRule(2, request);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    verify(service.ruleDao).update(captor.capture());
    RetentionRule input = captor.getValue();
    assertEquals(4, (int) input.getVersion());
    assertEquals(RetentionRuleType.DATASET, result.getType());
    assertEquals(2, (int) result.getRuleId());
    assertEquals(123, (int) result.getRetentionPeriod());
  }

  @Test
  public void updateGlobalRuleFetchesAndUpdatesEntity() throws SQLException {
    RetentionRuleUpdateRequest request = new RetentionRuleUpdateRequest();
    request.setRetentionPeriod(123);
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(2);
    existingRule.setRetentionPeriodInDays(12);
    existingRule.setVersion(3);
    existingRule.setType(RetentionRuleType.GLOBAL);
    when(service.ruleDao.findById(2)).thenReturn(existingRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);

    RetentionRuleResponse result = service.updateRetentionRule(2, request);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

    verify(service.ruleDao).update(captor.capture());
    // *** eshenlog
    // verify(service.jobManager).submitJob(workerCaptor.capture());
    RetentionRule input = captor.getValue();
    assertEquals(4, (int) input.getVersion());
    assertEquals(RetentionRuleType.GLOBAL, result.getType());
    assertEquals(2, (int) result.getRuleId());
    assertEquals(123, (int) result.getRetentionPeriod());
  }

  @Test
  public void deleteDatasetRule() {

    RetentionRule datasetRule = new RetentionRule();
    datasetRule.setId(3);
    datasetRule.setRetentionPeriodInDays(123);
    datasetRule.setVersion(2);
    datasetRule.setType(RetentionRuleType.DATASET);

    RetentionRule globalRule = new RetentionRule();
    globalRule.setId(2);
    globalRule.setRetentionPeriodInDays(12);
    globalRule.setVersion(3);
    globalRule.setType(RetentionRuleType.GLOBAL);

    when(service.ruleDao.findByBusinessKey(any(), any())).thenReturn(datasetRule);
    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);

    service.deleteRetentionRuleByBusinessKey("project", "storage");

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<UpdateDefaultJobWorker> workerCaptor =
        ArgumentCaptor.forClass(UpdateDefaultJobWorker.class);

    verify(service.ruleDao).softDelete(captor.capture());
    // *** eshenlog
    // verify(service.jobManager).submitJob(workerCaptor.capture());
  }

  @Test
  public void deleteGlobalRule() {

    RetentionRule datasetRule = new RetentionRule();
    datasetRule.setId(3);
    datasetRule.setRetentionPeriodInDays(123);
    datasetRule.setVersion(2);
    datasetRule.setType(RetentionRuleType.DATASET);

    RetentionRule globalRule = new RetentionRule();
    globalRule.setId(2);
    globalRule.setRetentionPeriodInDays(12);
    globalRule.setVersion(3);
    globalRule.setType(RetentionRuleType.GLOBAL);

    when(service.ruleDao.findByBusinessKey(any(), any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);

    service.deleteRetentionRuleByBusinessKey("project", "storage");

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<CancelDefaultJobWorker> workerCaptor =
        ArgumentCaptor.forClass(CancelDefaultJobWorker.class);

    verify(service.ruleDao).softDelete(captor.capture());
    // ***eshenlog
    // verify(service.jobManager).submitJob(workerCaptor.capture());
  }
}
