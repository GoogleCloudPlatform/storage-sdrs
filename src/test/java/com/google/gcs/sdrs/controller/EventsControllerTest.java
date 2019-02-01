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

package com.google.gcs.sdrs.controller;

import com.google.gcs.sdrs.controller.pojo.ErrorResponse;
import com.google.gcs.sdrs.controller.pojo.EventResponse;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.controller.validation.ValidationResult;
import com.google.gcs.sdrs.enums.ExecutionEventType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class EventsControllerTest {

  private EventsController controller;

  @Before()
  public void setup() {
    controller = new EventsController();
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
    HttpException testException = new ValidationException(ValidationResult.fromString("test"));
    Response response = controller.generateExceptionResponse(testException, "requestUuid");
    assertEquals(response.getStatus(), 400);
    assertEquals(((ErrorResponse) response.getEntity()).getMessage(), "Invalid input: test");
  }

  @Test
  public void executeEventWhenSuccessfulIncludesResponseFields() {
    ExecutionEventRequest request = new ExecutionEventRequest();
    request.setType(ExecutionEventType.POLICY);

    Response response = controller.executeEvent(request);

    assertEquals(response.getStatus(), 200);
    assertTrue(((EventResponse) response.getEntity()).getMessage().length() > 0);
    assertNotNull(((EventResponse) response.getEntity()).getRequestUuid());
  }

  @Test
  public void executeEventMissingTypeFails() {
    ExecutionEventRequest request = new ExecutionEventRequest();

    Response response = controller.executeEvent(request);

    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("type"));
  }

  @Test
  public void executePolicyWithValidFieldsSucceeds() {
    ExecutionEventRequest request = new ExecutionEventRequest();
    request.setType(ExecutionEventType.POLICY);

    Response response = controller.executeEvent(request);

    assertEquals(response.getStatus(), 200);
  }

  @Test
  public void executeUserEventWithValidFieldsSucceeds() {
    ExecutionEventRequest request = new ExecutionEventRequest();
    request.setType(ExecutionEventType.USER_COMMANDED);
    request.setProjectId("projectId");
    request.setTarget("gs://b/s/t");

    Response response = controller.executeEvent(request);

    assertEquals(response.getStatus(), 200);
  }

  @Test
  public void executeUserEventWithInvalidProjectIdFails() {
    ExecutionEventRequest request = new ExecutionEventRequest();
    request.setType(ExecutionEventType.USER_COMMANDED);
    request.setProjectId(null);
    request.setTarget("gs://b/s/t");

    Response response = controller.executeEvent(request);

    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("projectId"));
  }
}
