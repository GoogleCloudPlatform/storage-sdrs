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

package com.google.gcs.sdrs.controller;

import com.google.gcs.sdrs.controller.filter.ContainerContextProperties;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.BaseHttpResponse;
import com.google.gcs.sdrs.controller.pojo.ErrorResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Abstract base class for Controllers. */
public abstract class BaseController {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  @Context() ContainerRequestContext context;

  protected Response generateExceptionResponse(HttpException exception) {
    ErrorResponse errorResponse = new ErrorResponse();
    errorResponse.setMessage(exception.getMessage());

    return Response.status(exception.getStatusCode()).entity(errorResponse).build();
  }

  protected UserInfo getUserInfo() {
    return (UserInfo) context.getProperty(ContainerContextProperties.USER_INFO.toString());
  }

  protected Response successResponse(BaseHttpResponse responseBody) {
    return Response.status(HttpStatus.OK_200).entity(responseBody).build();
  }

  protected Response errorResponse(Exception exception) {
    if (exception instanceof HttpException) {
      return generateExceptionResponse((HttpException) exception);
    } else {
      this.logger.error(String.format("Unhandled internal error: %s", exception.getMessage()));
      logger.error(String.format("Caused by: %s", exception.getCause().getMessage()));
      return generateExceptionResponse(new InternalServerException(exception));
    }
  }
}
