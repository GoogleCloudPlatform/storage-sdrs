package com.google.gcs.sdrs.controller;

import org.eclipse.jetty.http.HttpStatus;

public class PersistenceException extends HttpException {

  private String message;

  /**
   * An exception type for any persistence layer errors
   * @param ex The root exception to surface
   */
  PersistenceException(Exception ex){
    message = String.format("A persistence error occurred: %s", ex.getMessage());
  }

  /**
   * Gets the message to return
   * @return the exception message
   */
  @Override
  public String getMessage() {
    return message;
  }

  /** Gets the validation error HTTP status code */
  @Override
  public int getStatusCode() {
    return HttpStatus.BAD_REQUEST_400;
  }
}
