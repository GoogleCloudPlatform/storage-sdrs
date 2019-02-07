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

import com.google.gcs.sdrs.controller.pojo.EventResponse;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.controller.validation.FieldValidations;
import com.google.gcs.sdrs.controller.validation.ValidationResult;
import com.google.gcs.sdrs.service.EventsService;
import com.google.gcs.sdrs.service.impl.EventsServiceImpl;
import java.util.Collection;
import java.util.HashSet;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Controller for exposing event based behavior */
@Path("/events")
public class EventsController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(EventsController.class);

  EventsService service = new EventsServiceImpl();

  /** Accepts a request to invoke a policy or process a manual delete */
  @POST
  @Path("/execution")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response executeEvent(ExecutionEventRequest request) {
    String requestUuid = generateRequestUuid();

    try {
      validateExecutionEvent(request);

      service.executeEvent(request);

      EventResponse response = new EventResponse();
      response.setRequestUuid(requestUuid);
      response.setMessage("Event registered and awaiting execution.");

      return Response.status(200).entity(response).build();
    } catch (HttpException exception) {
      return generateExceptionResponse(exception, requestUuid);
    }
  }

  /** Accepts a request to invoke a validation service run */
  @POST
  @Path("/validation")
  @Produces(MediaType.APPLICATION_JSON)
  public Response executeValidation() {
    String requestUuid = generateRequestUuid();

    service.executeValidationService();

    EventResponse response = new EventResponse();
    response.setRequestUuid(requestUuid);
    response.setMessage("Validation service run request registered.");

    return Response.status(200).entity(response).build();
  }

  /**
   * Runs validation checks against the "Execution" event request type
   *
   * @throws ValidationException when the request is invalid
   */
  private void validateExecutionEvent(ExecutionEventRequest request) throws ValidationException {
    Collection<ValidationResult> partialValidations = new HashSet<>();

    if (request.getExecutionEventType() == null) {
      partialValidations.add(ValidationResult.fromString("type must be provided"));
    } else {
      switch (request.getExecutionEventType()) {
        case USER_COMMANDED:
          if (request.getProjectId() == null) {
            partialValidations.add(
                ValidationResult.fromString("projectId must be provided if type is USER"));
          }
          partialValidations.add(
              FieldValidations.validateFieldFollowsBucketNamingStructure(
                  "target", request.getTarget()));
          break;
        case POLICY:
        default:
          break;
      }
    }

    ValidationResult result = ValidationResult.compose(partialValidations);
    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }
}
