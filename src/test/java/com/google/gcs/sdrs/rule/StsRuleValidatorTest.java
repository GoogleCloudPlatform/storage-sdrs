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

package com.google.gcs.sdrs.rule;

import com.google.api.services.storagetransfer.v1.model.Operation;
import com.google.api.services.storagetransfer.v1.model.Status;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.enums.RetentionJobStatusType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StsRuleValidatorTest {

  private StsRuleValidator objectUnderTest;

  private String stsJobId = "12345";
  private int jobId = 123;

  @Before
  public void initialize(){
    objectUnderTest = StsRuleValidator.getInstance();
  }

  @Test
  public void extractJobIdSuccess(){
    String operationName = String.format("transferOperation/transferJob-%s-67890", stsJobId);
    String result = objectUnderTest.extractStsJobId(operationName);

    assertEquals(stsJobId, result);
  }

  @Test
  public void extractJobIdFailure(){
    String operationName = String.format("transferOperation/transferJob-%s", stsJobId);
    try {
      String result = objectUnderTest.extractStsJobId(operationName);
      Assert.fail();
    } catch (StringIndexOutOfBoundsException ex) {
      assertTrue(true);
    }
  }

  @Test
  public void convertOperationToJobValidationPending(){
    Operation operation = new Operation();
    operation.setName("testOperation");
    operation.setDone(false);
    RetentionJobValidation validation =
        objectUnderTest.convertOperationToJobValidation(operation, jobId);

    assertEquals(validation.getJobOperationName(), operation.getName());
    assertEquals((int)validation.getRetentionJobId(), jobId);
    assertEquals(validation.getStatus(), RetentionJobStatusType.PENDING);
  }

  @Test
  public void convertOperationToJobValidationSuccess(){
    Operation operation = new Operation();
    operation.setName("testOperation");
    operation.setDone(true);

    Map<String, Object> responseMap = new HashMap<>();
    responseMap.put("type", "transferOperation");
    operation.setResponse(responseMap);

    RetentionJobValidation validation =
        objectUnderTest.convertOperationToJobValidation(operation, jobId);

    assertEquals(validation.getJobOperationName(), operation.getName());
    assertEquals((int)validation.getRetentionJobId(), jobId);
    assertEquals(validation.getStatus(), RetentionJobStatusType.SUCCESS);
  }

  @Test
  public void convertOperationToJobValidationFailure(){
    Operation operation = new Operation();
    operation.setName("testOperation");
    operation.setDone(true);

    Status status = new Status();
    status.setCode(500);

    operation.setError(status);

    RetentionJobValidation validation =
        objectUnderTest.convertOperationToJobValidation(operation, jobId);

    assertEquals(validation.getJobOperationName(), operation.getName());
    assertEquals((int)validation.getRetentionJobId(), jobId);
    assertEquals(validation.getStatus(), RetentionJobStatusType.ERROR);
  }
}
