package com.google.gcs.sdrs.controller.mapper.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.google.gcs.sdrs.controller.pojo.response.ErrorResponse;
import javax.ws.rs.core.Response;
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

    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("Unable"));
    assertEquals(((ErrorResponse) response.getEntity()).getRequestUuid().length(), 36);
  }

  @Test
  public void createExceptionResponseMessageReturnsSomething() {
    String message = mapper.createExceptionResponseMessage(null);

    assertTrue(message.contains("JSON"));
  }
}
