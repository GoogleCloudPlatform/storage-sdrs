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

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import java.util.Arrays;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles Jackson InvalidFormatExceptions with custom error messages. This can be caused by an
 * invalid enum value on the request object.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class InvalidFormatExceptionMapper extends JacksonExceptionMapper<InvalidFormatException> {

  private static final Logger logger = LoggerFactory.getLogger(InvalidFormatExceptionMapper.class);

  protected String createExceptionResponseMessage(InvalidFormatException exception) {
    if (exception.getTargetType().isEnum()) {
      return String.format(
          "Expected '%s' to be one of %s",
          exception.getValue(), Arrays.toString(exception.getTargetType().getEnumConstants()));
    }

    if (exception.getTargetType().isPrimitive()) {
      return String.format(
          "Expected '%s' to be of type %s", exception.getValue(), exception.getTargetType());
    }

    return String.format("'%s' doesn't match expected type", exception.getValue());
  }
}
