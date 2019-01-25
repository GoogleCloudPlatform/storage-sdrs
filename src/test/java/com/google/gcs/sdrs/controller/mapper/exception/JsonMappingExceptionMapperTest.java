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

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gcs.sdrs.controller.mapper.exception.JsonMappingExceptionMapper;
import com.google.gcs.sdrs.controller.pojo.ErrorResponse;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonMappingExceptionMapperTest {

  private JsonMappingExceptionMapper mapper;

  @Before
  public void setUp() {
    mapper = new JsonMappingExceptionMapper();
  }

  @Test
  public void toResponseReturnsResponseWithFieldsSet() {
    JsonMappingException.Reference referenceMock = mock(JsonMappingException.Reference.class);
    List pathListMock = mock(List.class);
    JsonMappingException exceptionMock = mock(JsonMappingException.class);
    when(exceptionMock.getPath()).thenReturn(pathListMock);
    when(pathListMock.get(0)).thenReturn(referenceMock);
    when(referenceMock.getFieldName()).thenReturn("fieldName");
    when(exceptionMock.getLocation()).thenReturn(new JsonLocation(null, 0, 1, 1));

    Response response = mapper.toResponse(exceptionMock);

    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("Invalid"));
  }

  @Test
  public void createExceptionResponseMessageProvidesLocation() {
    JsonMappingException.Reference referenceMock = mock(JsonMappingException.Reference.class);
    List pathListMock = mock(List.class);
    JsonMappingException exceptionMock = mock(JsonMappingException.class);
    when(exceptionMock.getPath()).thenReturn(pathListMock);
    when(pathListMock.get(0)).thenReturn(referenceMock);
    when(referenceMock.getFieldName()).thenReturn("fieldName");
    when(exceptionMock.getLocation()).thenReturn(new JsonLocation(null, 0, 11, 12));

    String message = mapper.createExceptionResponseMessage(exceptionMock);

    assertTrue(message.contains("fieldName"));
    assertTrue(message.contains("11"));
    assertTrue(message.contains("12"));
  }
}
