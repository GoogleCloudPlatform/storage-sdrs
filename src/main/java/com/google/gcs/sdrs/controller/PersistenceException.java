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

package com.google.gcs.sdrs.controller;

import javax.ws.rs.core.Response;

public class PersistenceException extends HttpException {

  private String message;

  /**
   * An exception type for any persistence layer errors
   * @param ex The root exception to surface
   */
  public PersistenceException(Exception ex){
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
    return Response.Status.BAD_REQUEST.getStatusCode();
  }
}
