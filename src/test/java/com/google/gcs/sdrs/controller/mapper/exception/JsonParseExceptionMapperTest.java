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

package com.google.gcs.sdrs.controller.mapper.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.gcs.sdrs.controller.pojo.ErrorResponse;
import javax.ws.rs.core.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

public class JsonParseExceptionMapperTest {

  private JsonParseExceptionMapper mapper;

  @Before
  public void setUp() {
    mapper = new JsonParseExceptionMapper();
  }

  @Test
  public void toResponseReturnsResponseWithFieldsSet() {
    JsonParseException exceptionMock = mock(JsonParseException.class);

    Response response = mapper.toResponse(exceptionMock);

    assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST_400.getStatusCode());
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("Unable"));
    assertEquals(((ErrorResponse) response.getEntity()).getUuid().length(), 36);
  }

  @Test
  public void createExceptionResponseMessageReturnsSomething() {
    String message = mapper.createExceptionResponseMessage(null);

    assertTrue(message.contains("JSON"));
  }
}
