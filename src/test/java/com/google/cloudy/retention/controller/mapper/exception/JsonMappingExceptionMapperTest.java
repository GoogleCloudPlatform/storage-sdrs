package com.google.cloudy.retention.controller.mapper.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.cloudy.retention.controller.pojo.response.ErrorResponse;
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
