package com.google.cloudy.retention.controller.pojo.response;

/**
 * Base class to be used by HTTP response objects
 */
public abstract class BaseHttpResponse {

  private String requestUuid;

  public String getRequestUuid() {
    return requestUuid;
  }

  public void setRequestUuid(String requestUUID) {
    this.requestUuid = requestUUID;
  }
}
