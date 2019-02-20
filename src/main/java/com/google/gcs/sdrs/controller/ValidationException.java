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

import com.google.gcs.sdrs.controller.validation.ValidationResult;
import java.util.Collection;
import javax.ws.rs.core.Response;

/**
 * Exception thrown in case of validation errors. Supports messages including multiple identified
 * errors.
 */
public class ValidationException extends HttpException {

  private Collection<String> validationMessages;

  /** Constructs a ValidationException based off of values within a ValidationResult object */
  public ValidationException(ValidationResult validationResult) {
    validationMessages = validationResult.validationMessages;
  }

  /** Gets the error message including all tracked errors */
  @Override
  public String getMessage() {
    String messages = String.join(", ", validationMessages);
    return String.format("Invalid input: %s", messages);
  }

  /** Gets the validation error HTTP status code */
  @Override
  public int getStatusCode() {
    return Response.Status.BAD_REQUEST.getStatusCode();
  }
}
