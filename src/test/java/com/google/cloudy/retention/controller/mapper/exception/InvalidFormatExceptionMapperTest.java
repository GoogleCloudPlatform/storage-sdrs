package com.google.cloudy.retention.controller.mapper.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.google.cloudy.retention.controller.pojo.request.RetentionRuleCreateRequest;
import com.google.cloudy.retention.controller.pojo.response.ErrorResponse;
import com.google.cloudy.retention.enums.RetentionRuleTypes;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

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
    assertEquals(response.getStatus(), 400);
    assertTrue(((ErrorResponse) response.getEntity()).getMessage().contains("value"));
  }

  @Test
  public void createExceptionResponseMessageWhenEnumExpectedReturnsOptions() {
    InvalidFormatException exception =
        new InvalidFormatException(null, "message", "value", RetentionRuleTypes.class);
    String message = mapper.createExceptionResponseMessage(exception);
    assertTrue(message.contains(RetentionRuleTypes.GLOBAL.toString()));
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
