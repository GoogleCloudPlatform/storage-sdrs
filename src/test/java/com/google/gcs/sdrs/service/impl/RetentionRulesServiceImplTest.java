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

import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.dao.impl.GenericDao;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RetentionRulesServiceImplTest {

  private RetentionRulesServiceImpl service = new RetentionRulesServiceImpl();

  @Before
  public void setup() {
    service.dao = mock(GenericDao.class);
  }

  @Test
  public void createRulePersistsDatasetEntity() {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setType(RetentionRuleType.DATASET);
    createRule.setRetentionPeriod(123);
    createRule.setDatasetName("dataset");
    createRule.setDataStorageName("gs://b/d");
    createRule.setProjectId("projectId");

    service.createRetentionRule(createRule);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.dao).persist(captor.capture());
    RetentionRule input = captor.getValue();
    assertNull(input.getId());
    assertEquals(RetentionRuleType.DATASET, input.getType());
    assertEquals(123, (int) input.getRetentionPeriodInDays());
    assertEquals(true, input.getIsActive());
    assertNotNull(input.getUpdatedAt());
    assertNotNull(input.getCreatedAt());
    assertEquals(input.getProjectId(), "projectId");
    assertEquals(input.getDataStorageName(), "gs://b/d");
    assertEquals(input.getDatasetName(), "dataset");
    assertEquals(1, (int) input.getVersion());
  }

  @Test
  public void createRulePersistsGlobalEntity() {
    RetentionRuleCreateRequest createRule = new RetentionRuleCreateRequest();
    createRule.setType(RetentionRuleType.GLOBAL);
    createRule.setRetentionPeriod(123);

    service.createRetentionRule(createRule);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);

    verify(service.dao).persist(captor.capture());
    RetentionRule input = captor.getValue();
    assertNull(input.getId());
    assertEquals(RetentionRuleType.GLOBAL, input.getType());
    assertEquals(123, (int) input.getRetentionPeriodInDays());
    assertEquals(true, input.getIsActive());
    assertNotNull(input.getUpdatedAt());
    assertNotNull(input.getCreatedAt());
    assertEquals(input.getProjectId(), "global-default");
    assertEquals(1, (int) input.getVersion());
    assertNull(input.getDataStorageName());
    assertNull(input.getDatasetName());
  }

  @Test
  public void updateRuleFetchesAndUpdatesEntity() {
    RetentionRuleUpdateRequest request = new RetentionRuleUpdateRequest();
    request.setRetentionPeriod(123);
    RetentionRule existingRule = new RetentionRule();
    existingRule.setId(2);
    existingRule.setRetentionPeriodInDays(12);
    existingRule.setVersion(3);
    when(service.dao.findById(2)).thenReturn(existingRule);

    RetentionRuleResponse result = service.updateRetentionRule(2, request);

    ArgumentCaptor<RetentionRule> captor = ArgumentCaptor.forClass(RetentionRule.class);
    verify(service.dao).update(captor.capture());
    RetentionRule input = captor.getValue();
    assertEquals(4, (int) input.getVersion());

    assertEquals(2, (int) result.getRuleId());
    assertEquals(123, (int) result.getRetentionPeriod());
  }
}
