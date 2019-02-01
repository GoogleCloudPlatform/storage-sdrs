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

package com.google.gcs.sdrs.controller;

import com.google.gcs.sdrs.controller.pojo.ErrorResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateResponse;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import com.google.gcs.sdrs.service.impl.RetentionRulesServiceImpl;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RetentionRulesControllerTest {

  private RetentionRulesController controller;

  @Before()
  public void setup() {
    controller = new RetentionRulesController();
    controller.service = mock(RetentionRulesServiceImpl.class);
  }

  @Test
  public void generateRequestUuidOutputs36Characters() {
    String uuid = controller.generateRequestUuid();
    assertEquals(uuid.length(), 36);
  }

  @Test
  public void generateRequestUuidOutputsNewResults() {
    String uuid1 = controller.generateRequestUuid();
    String uuid2 = controller.generateRequestUuid();
    assertNotSame(uuid1, uuid2);
  }

  @Test
  public void generateExceptionResponseWithValidInputReturnsResponseWithFields() {
    HttpException testException = new ValidationException();
    Response response = controller.generateExceptionResponse(testException, "requestUuid");
    assertEquals(response.getStatus(), 400);
    assertEquals(((ErrorResponse) response.getEntity()).getMessage(), "Invalid input: ");
  }

  @Test
  public void createRuleWhenSuccessfulIncludesResponseFields() {
    when(controller.service.createRetentionRule(any(RetentionRuleCreateRequest.class)))
        .thenReturn(543);

    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.GLOBAL);
    rule.setRetentionPeriod(1);

    Response response = controller.create(rule);

    assertEquals(200, response.getStatus());
    assertEquals(543, ((RetentionRuleCreateResponse) response.getEntity()).getRuleId());
    assertNotNull(((RetentionRuleCreateResponse) response.getEntity()).getRequestUuid());
  }

  @Test
  public void createRuleMissingTypeFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("type"));
  }

  @Test
  public void createGlobalRuleWithPeriodSucceeds() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.GLOBAL);
    rule.setRetentionPeriod(1);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 200);
  }

  @Test
  public void createGlobalRuleWithNegativePeriodFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.GLOBAL);
    rule.setRetentionPeriod(-1);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("retentionPeriod"));
  }

  @Test
  public void createGlobalRuleWithLargePeriodFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.GLOBAL);
    rule.setRetentionPeriod(1000000);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("retentionPeriod"));
  }

  @Test
  public void createDatasetRuleMissingPeriodFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("retentionPeriod"));
  }

  @Test
  public void createDatasetRuleMissingProjectFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.DATASET);
    rule.setDatasetName("datasetName");
    rule.setDataStorageName("gs://bucket/dataset");
    rule.setRetentionPeriod(123);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("projectId"));
  }

  @Test
  public void createDatasetRuleMissingDataStorageFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.DATASET);
    rule.setDatasetName("datasetName");
    rule.setRetentionPeriod(123);
    rule.setProjectId("projectId");
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("dataStorageName"));
  }

  @Test
  public void createDatasetRuleMissingDataStoragePrefixFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.DATASET);
    rule.setDatasetName("datasetName");
    rule.setDataStorageName("bucket/dataset");
    rule.setRetentionPeriod(123);
    rule.setProjectId("projectId");
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("dataStorageName"));
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("gs://"));
  }

  @Test
  public void createDatasetRuleMissingDataStorageBucketFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.DATASET);
    rule.setDatasetName("datasetName");
    rule.setDataStorageName("gs:///dataset");
    rule.setRetentionPeriod(123);
    rule.setProjectId("projectId");
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("dataStorageName"));
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("bucket"));
  }

  @Test
  public void createDatasetRuleWithValidFieldsSucceeds() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleType.DATASET);
    rule.setDatasetName("datasetName");
    rule.setDataStorageName("gs://bucket/dataset");
    rule.setRetentionPeriod(123);
    rule.setProjectId("projectId");
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 200);
  }

  @Test
  public void updateRuleWithValidFieldsSucceeds() {
    RetentionRuleUpdateRequest request = new RetentionRuleUpdateRequest();
    request.setRetentionPeriod(123);
    RetentionRuleResponse serviceResponse = new RetentionRuleResponse();
    serviceResponse.setDatasetName("dataset");
    serviceResponse.setDataStorageName("gs://bucket/dataset");
    serviceResponse.setProjectId("projectId");
    serviceResponse.setRetentionPeriod(123);
    serviceResponse.setRuleId(1);
    serviceResponse.setType(RetentionRuleType.DATASET);
    when(controller.service.updateRetentionRule(anyInt(), any(RetentionRuleUpdateRequest.class)))
        .thenReturn(serviceResponse);

    Response response = controller.update(1, request);

    assertEquals(response.getStatus(), 200);
    RetentionRuleResponse body = (RetentionRuleResponse) response.getEntity();
    assertEquals(body.getDatasetName(), "dataset");
    assertEquals(body.getDataStorageName(), "gs://bucket/dataset");
    assertEquals(body.getProjectId(), "projectId");
    assertEquals((int) body.getRetentionPeriod(), 123);
    assertEquals((int) body.getRuleId(), 1);
    assertEquals(body.getType(), RetentionRuleType.DATASET);
  }

  @Test
  public void updateRuleWithInvalidRetentionRuleFails() {
    RetentionRuleUpdateRequest rule = new RetentionRuleUpdateRequest();
    rule.setRetentionPeriod(-1);

    Response response = controller.update(1, rule);

    assertEquals(response.getStatus(), 400);
    ErrorResponse body = (ErrorResponse) response.getEntity();
    assertTrue(body.getMessage().contains("retentionPeriod"));
  }
}
