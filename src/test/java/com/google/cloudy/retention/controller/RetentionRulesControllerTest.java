package com.google.cloudy.retention.controller;

import com.google.cloudy.retention.controller.pojo.request.RetentionRuleCreateRequest;
import com.google.cloudy.retention.controller.pojo.response.ErrorResponse;
import com.google.cloudy.retention.controller.pojo.response.RetentionRuleCreateResponse;
import com.google.cloudy.retention.enums.RetentionRuleTypes;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;

public class RetentionRulesControllerTest {

  private RetentionRulesController controller;

  @Before()
  public void setup() {
    controller = new RetentionRulesController();
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
    Response response =
        controller.generateExceptionResponse(testException, "requestUuid");
    assertEquals(response.getStatus(), 400);
    assertEquals(((ErrorResponse) response.getEntity()).getMessage(), "Invalid input: ");
  }

  @Test
  public void createRuleWhenSuccessfulIncludesResponseFields() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleTypes.GLOBAL);
    rule.setRetentionPeriod(1);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 200);
    assertEquals(((RetentionRuleCreateResponse) response.getEntity()).getRuleId(), 1);
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
    rule.setType(RetentionRuleTypes.GLOBAL);
    rule.setRetentionPeriod(1);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 200);
  }

  @Test
  public void createGlobalRuleWithNegativePeriodFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleTypes.GLOBAL);
    rule.setRetentionPeriod(-1);
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("retentionPeriod"));
  }

  @Test
  public void createGlobalRuleWithLargePeriodFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleTypes.GLOBAL);
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
    rule.setType(RetentionRuleTypes.DATASET);
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
    rule.setType(RetentionRuleTypes.DATASET);
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
    rule.setType(RetentionRuleTypes.DATASET);
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
    rule.setType(RetentionRuleTypes.DATASET);
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
  public void createDatasetRuleMissingDataStorageDatasetFails() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleTypes.DATASET);
    rule.setDatasetName("datasetName");
    rule.setDataStorageName("gs://bucket");
    rule.setRetentionPeriod(123);
    rule.setProjectId("projectId");
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("dataStorageName"));
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("dataset"));
  }

  @Test
  public void createDatasetRuleWithValidFieldsSucceeds() {
    RetentionRuleCreateRequest rule = new RetentionRuleCreateRequest();
    rule.setType(RetentionRuleTypes.DATASET);
    rule.setDatasetName("datasetName");
    rule.setDataStorageName("gs://bucket/dataset");
    rule.setRetentionPeriod(123);
    rule.setProjectId("projectId");
    Response response = controller.create(rule);
    assertEquals(response.getStatus(), 200);
  }
}
