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

package com.google.gcs.sdrs.controller.mapper.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.gcs.sdrs.controller.pojo.ErrorResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InvalidFormatExceptionMapperTest {

  private InvalidFormatExceptionMapper mapper;

  @Before
  public void setUp() {
    mapper = new InvalidFormatExceptionMapper();
  }

  @Test
  public void toResponseReturnsResponseWithFieldsSet() {
    InvalidFormatException exception =
        new InvalidFormatException(null, "message", "value", RetentionRuleCreateRequest.class);
    Response response = mapper.toResponse(exception);
    assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST_400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("value"));
    assertEquals(((ErrorResponse) response.getEntity()).getRequestUuid().length(), 36);
  }

  @Test
  public void createExceptionResponseMessageWhenEnumExpectedReturnsOptions() {
    InvalidFormatException exception =
        new InvalidFormatException(null, "message", "value", RetentionRuleType.class);
    String message = mapper.createExceptionResponseMessage(exception);
    assertTrue(message.contains(RetentionRuleType.GLOBAL.toString()));
  }

  @Test
  public void createExceptionResponseMessageWhenPrimitiveExpectedReturnsTypeName() {
    InvalidFormatException exception =
        new InvalidFormatException(null, "message", "value", int.class);
    String message = mapper.createExceptionResponseMessage(exception);
    assertTrue(message.contains("int"));
  }

  @Test
  public void createExceptionResponseMessageWhenSomethingElseExpectedReturnsGeneralMessage() {
    InvalidFormatException exception =
        new InvalidFormatException(null, "message", "value", Object.class);
    String message = mapper.createExceptionResponseMessage(exception);
    assertTrue(message.contains("doesn't match"));
  }
}
