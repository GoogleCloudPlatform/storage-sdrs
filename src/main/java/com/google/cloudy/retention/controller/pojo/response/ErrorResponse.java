package com.google.cloudy.retention.controller.pojo.response;

public class ErrorResponse extends BaseHttpResponse {
  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String message;
}
