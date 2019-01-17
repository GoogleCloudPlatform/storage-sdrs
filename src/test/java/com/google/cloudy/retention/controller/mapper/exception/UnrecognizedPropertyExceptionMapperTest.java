package com.google.cloudy.retention.controller.mapper.exception;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.cloudy.retention.controller.pojo.response.ErrorResponse;
import com.google.cloudy.retention.enums.RetentionRuleTypes;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnrecognizedPropertyExceptionMapperTest {

  private UnrecognizedPropertyExceptionMapper mapper;

  @Before
  public void setUp() {
    mapper = new UnrecognizedPropertyExceptionMapper();
  }

  @Test
  public void toResponseReturnsResponseWithFieldsSet() {
    JsonMappingException.Reference referenceMock = mock(JsonMappingException.Reference.class);
    List pathListMock = mock(List.class);
    UnrecognizedPropertyException exceptionMock = mock(UnrecognizedPropertyException.class);
    when(exceptionMock.getPath()).thenReturn(pathListMock);
    when(pathListMock.get(0)).thenReturn(referenceMock);
    when(referenceMock.getFieldName()).thenReturn("fieldName");
    when(exceptionMock.getLocation()).thenReturn(new JsonLocation(null, 0, 1, 1));

    Response response = mapper.toResponse(exceptionMock);

    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("fieldName"));
  }

  @Test
  public void createExceptionResponseMessageIncludesProblemField() {
    JsonMappingException.Reference referenceMock = mock(JsonMappingException.Reference.class);
    List pathListMock = mock(List.class);
    UnrecognizedPropertyException exceptionMock = mock(UnrecognizedPropertyException.class);
    when(exceptionMock.getPath()).thenReturn(pathListMock);
    when(pathListMock.get(0)).thenReturn(referenceMock);
    when(referenceMock.getFieldName()).thenReturn("fieldName");
    when(exceptionMock.getLocation()).thenReturn(new JsonLocation(null, 0, 1, 1));

    String message = mapper.createExceptionResponseMessage(exceptionMock);

    assertTrue(message.contains("fieldName"));
  }
}
