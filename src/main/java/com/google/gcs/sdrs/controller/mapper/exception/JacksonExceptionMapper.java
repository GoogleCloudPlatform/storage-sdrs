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

import com.google.gcs.sdrs.controller.pojo.response.ErrorResponse;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gcs.sdrs.controller.pojo.ErrorResponse;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for ExceptionMappers for JSON deserialization errors.
 *
 * @param <T> The specific Jackson Exception type
 */
public abstract class JacksonExceptionMapper<T extends Throwable> implements ExceptionMapper<T> {

  protected static final Logger logger = LoggerFactory.getLogger(JsonMappingExceptionMapper.class);

  @Override
  public Response toResponse(T exception) {
    logger.debug(exception.getMessage());
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setMessage(createExceptionResponseMessage(exception));
    errorResponse.setRequestUuid(UUID.randomUUID().toString());

    return Response.status(400).entity(errorResponse).build();
  }

  /**
   * Creates a user-facing message from an exception
   *
   * @param exception details on the current exception
   * @return A user friendly error message.
   */
  protected abstract String createExceptionResponseMessage(T exception);
}
