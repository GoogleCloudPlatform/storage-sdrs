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

import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.dao.impl.RetentionRuleDaoImpl;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.manager.JobManager;
import com.google.gcs.sdrs.service.worker.Worker;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.util.GcsHelper;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StsRuleExecutor.class, GcsHelper.class})
@PowerMockIgnore("javax.management.*")
public class RetentionRulesServiceImplTest {

  private RetentionRulesServiceImpl service = new RetentionRulesServiceImpl();
  private RetentionRule globalRule;
  private List<String> projectIds = new ArrayList<>();
  private GcsHelper mockGcsHelper;

  @Before
  public void setup() {
    service.ruleDao = mock(RetentionRuleDaoImpl.class);
    service.jobManager.shutDownJobManagerNow();
    service.jobManager = mock(JobManager.class);
    mockGcsHelper = mock(GcsHelper.class);
    globalRule = new RetentionRule();
    globalRule.setId(10);
    globalRule.setProjectId("global-default");
    globalRule.setDataStorageName("global");
    globalRule.setRetentionValue("365:day");
    String projectId = "test";
    projectIds.add(projectId);

    PowerMockito.mockStatic(StsRuleExecutor.class);
    when(StsRuleExecutor.getInstance()).thenReturn(null);

    PowerMockito.mockStatic(GcsHelper.class);
    when(GcsHelper.getInstance()).thenReturn(mockGcsHelper);
    when(mockGcsHelper.doesBucketExist(any(), any())).thenReturn(true);
  }

  @Test
  public void createRulePersistsDatasetEntity() throws SQLException, IOException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setRetentionPeriodUnit("day");
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

    RetentionRule input = captor.getValue();
    assertEquals(1, (int) input.getId());
    assertEquals(RetentionRuleType.DATASET, input.getType());
    assertEquals("123:day", input.getRetentionValue());
    assertEquals(true, input.getIsActive());
    assertEquals("projectId", input.getProjectId());
    assertEquals("gs://b/d", input.getDataStorageName());
    assertEquals("dataset", input.getDatasetName());
    assertEquals(1, (int) input.getVersion());
  }

  @Test
  public void createRuleUsesBucketForDatasetWhenNoDataset() throws SQLException, IOException {
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
  public void createRuleUsesDataStorageDatasetForDataset() throws SQLException, IOException {
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
  public void createRulePersistsGlobalEntity() throws SQLException, IOException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.GLOBAL);
    createRule.setRetentionPeriod(123);
    createRule.setRetentionPeriodUnit("day");

    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);
    when(service.ruleDao.save(any())).thenReturn(1);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

    verify(service.ruleDao).save(captor.capture());

    RetentionRule input = captor.getValue();
    assertEquals(1, (int) input.getId());
    assertEquals(RetentionRuleType.GLOBAL, input.getType());
    assertEquals("123:day", input.getRetentionValue());
    assertEquals(true, input.getIsActive());
    assertEquals("global-default", input.getProjectId());
    assertEquals(1, (int) input.getVersion());
    assertEquals("gs://global", input.getDataStorageName());
    assertNull(input.getDatasetName());
  }

  @Test
  public void createRuleOverwritesDeletedEntity() throws SQLException, IOException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDatasetName("dataset");
    createRule.setDataStorageName("gs://b/d");
    createRule.setProjectId("projectId");
    RetentionRule existingRule = new RetentionRule();
    existingRule.setDataStorageName("gs://b/d");
    existingRule.setProjectId("projectId");
    existingRule.setRetentionValue("1:day");
    existingRule.setIsActive(false);
    existingRule.setVersion(2);

    List<RetentionRule> bucketRules = new ArrayList<>();
    bucketRules.add(existingRule);

    when(service.ruleDao.findRulesByDataStorageRoot(any(), any(), any(), any()))
        .thenReturn(bucketRules);

    service.createRetentionRule(createRule, new UserInfo());

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.ruleDao).update(captor.capture());
    RetentionRule input = captor.getValue();
    assertNull(input.getId());
    assertEquals(RetentionRuleType.DATASET, input.getType());
    assertEquals("123:day", input.getRetentionValue());
    assertEquals(true, input.getIsActive());
    assertEquals(input.getProjectId(), "projectId");
    assertEquals(input.getDataStorageName(), "gs://b/d");
    assertEquals(input.getDatasetName(), "dataset");
    assertEquals(3, (int) input.getVersion());
  }

  @Test()
  public void createRuleBucketNotExist() throws SQLException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDatasetName("dataset");
    createRule.setDataStorageName("gs://bucket-not-exist/d");
    createRule.setProjectId("projectId");

    when(mockGcsHelper.doesBucketExist("bucket-not-exist", "projectId")).thenReturn(false);

    try {
      service.createRetentionRule(createRule, new UserInfo());
    } catch (IOException e) {
      Assert.assertTrue(e.getMessage().contains("Bucket bucket-not-exist does not exist"));
    }
  }

  @Test
  public void createRuleBuckeNestedRuleViolation() throws IOException {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setRetentionRuleType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDatasetName("dataset");
    createRule.setDataStorageName("gs://b/d/c");
    createRule.setProjectId("projectId");
    RetentionRule existingRule = new RetentionRule();
    existingRule.setDataStorageName("gs://b/d");
    existingRule.setProjectId("projectId");
    existingRule.setRetentionValue("1:day");
    existingRule.setIsActive(true);
    existingRule.setType(RetentionRuleType.DATASET);
    existingRule.setVersion(2);

    List<RetentionRule> bucketRules = new ArrayList<>();
    bucketRules.add(existingRule);

    when(service.ruleDao.findRulesByDataStorageRoot(any(), any(), any(), any()))
        .thenReturn(bucketRules);
    try {
      service.createRetentionRule(createRule, new UserInfo());
    } catch (SQLException e) {
      Assert.assertTrue(e.getMessage().contains("violating non-nesting rule"));
    }
  }

  @Test
  public void getRuleByBusinessKeyReturnsMappedValues() {
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(2);
    existingRule.setRetentionValue("12:day");
    existingRule.setProjectId("projectId");
    existingRule.setDataStorageName("gs://bucket");
    when(service.ruleDao.findByBusinessKey(anyString(), anyString(), any(), any()))
        .thenReturn(existingRule);

    RetentionRuleResponse result =
        service.getRetentionRuleByBusinessKey("any", "any", RetentionRuleType.DATASET);
    assertEquals(12, (int) result.getRetentionPeriod());
    assertEquals("projectId", result.getProjectId());
    assertEquals("gs://bucket", result.getDataStorageName());
  }

  @Test(expected = EntityNotFoundException.class)
  public void getRuleByBusinessKeyThrowsErrorWhenNull() {
    when(service.ruleDao.findByBusinessKey(anyString(), anyString(), any(), any()))
        .thenReturn(null);

    service.getRetentionRuleByBusinessKey("any", "any", RetentionRuleType.DATASET);
  }

  @Test
  public void GetRuleByRuleIdReturnsMappedValue() throws SQLException {
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(5);
    existingRule.setRetentionValue("3:version");
    existingRule.setProjectId("projectId");
    existingRule.setDataStorageName("gs://bucket");
    existingRule.setIsActive(true);
    when(service.ruleDao.findById(5)).thenReturn(existingRule);

    RetentionRule rule = service.getRetentionRuleByRuleId(5);
    assertEquals("3:version", rule.getRetentionValue());
    assertEquals("projectId", rule.getProjectId());
    assertEquals("gs://bucket", rule.getDataStorageName());
  }

  @Test
  public void updateDatasetRuleFetchesAndUpdatesEntity() throws SQLException {
    RetentionRuleUpdateRequest request = new RetentionRuleUpdateRequest();
    request.setRetentionPeriod(123);
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(2);
    existingRule.setRetentionValue("12:day");
    existingRule.setVersion(3);
    existingRule.setType(RetentionRuleType.DATASET);
    existingRule.setIsActive(true);
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
    existingRule.setRetentionValue("12:day");
    existingRule.setVersion(3);
    existingRule.setType(RetentionRuleType.GLOBAL);
    existingRule.setIsActive(true);
    when(service.ruleDao.findById(2)).thenReturn(existingRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);

    RetentionRuleResponse result = service.updateRetentionRule(2, request);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    ArgumentCaptor<Worker> workerCaptor = ArgumentCaptor.forClass(Worker.class);

    verify(service.ruleDao).update(captor.capture());

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
    datasetRule.setRetentionValue("123:day");
    datasetRule.setVersion(2);
    datasetRule.setType(RetentionRuleType.DATASET);

    RetentionRule globalRule = new RetentionRule();
    globalRule.setId(2);
    globalRule.setRetentionValue("12:day");
    globalRule.setVersion(3);
    globalRule.setType(RetentionRuleType.GLOBAL);

    when(service.ruleDao.findByBusinessKey(any(), any(), any(), any())).thenReturn(datasetRule);
    when(service.ruleDao.findGlobalRuleByProjectId(any())).thenReturn(globalRule);

    service.deleteRetentionRuleByBusinessKey("project", "storage", RetentionRuleType.DATASET);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.ruleDao).softDelete(captor.capture());
  }

  @Test
  public void deleteGlobalRule() {

    RetentionRule datasetRule = new RetentionRule();
    datasetRule.setId(3);
    datasetRule.setRetentionValue("123:day");
    datasetRule.setVersion(2);
    datasetRule.setType(RetentionRuleType.DATASET);

    RetentionRule globalRule = new RetentionRule();
    globalRule.setId(2);
    globalRule.setRetentionValue("12:day");
    globalRule.setVersion(3);
    globalRule.setType(RetentionRuleType.GLOBAL);

    when(service.ruleDao.findByBusinessKey(any(), any(), any(), any())).thenReturn(globalRule);
    when(service.ruleDao.getAllDatasetRuleProjectIds()).thenReturn(projectIds);

    service.deleteRetentionRuleByBusinessKey("project", "storage", RetentionRuleType.DATASET);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.ruleDao).softDelete(captor.capture());
  }
}
