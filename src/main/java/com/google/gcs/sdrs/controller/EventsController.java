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
import com.google.gcs.sdrs.controller.pojo.NotificationEventRequest;
import com.google.gcs.sdrs.controller.validation.FieldValidations;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.controller.validation.ValidationResult;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.service.EventsService;
import com.google.gcs.sdrs.service.impl.EventsServiceImpl;
import com.google.gcs.sdrs.util.GcsHelper;
import com.google.gcs.sdrs.util.RetentionUtil;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Controller for exposing event based behavior */
@Path("/events")
public class EventsController extends BaseController {

  EventsService service = new EventsServiceImpl();

  /** Accepts a request to invoke a policy or process a manual delete */
  @POST
  @Path("/execution")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response executeEvent(ExecutionEventRequest request) {
    try {
      validateExecutionEvent(request);
      EventResponse eventResponse =
          generateResponse("Execution event received and being processed");
      service.processExecutionEvent(request, getCorrelationId());
      return successResponse(eventResponse);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /** Accepts a request to invoke a validation service run */
  @POST
  @Path("/validation")
  @Produces(MediaType.APPLICATION_JSON)
  public Response executeValidation() {
    try {
      EventResponse eventResponse =
          generateResponse("Validation event received and being processed");
      service.processValidationEvent(getCorrelationId());
      return successResponse(eventResponse);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /** Accepts a request to invoke a delete notification service */
  @POST
  @Path("/notification")
  @Produces(MediaType.APPLICATION_JSON)
  public Response executeDeleteNotification(NotificationEventRequest request) {
    try {
      validateNotificationEvent(request);
      EventResponse eventResponse =
          generateResponse("Delete notification event received and being processed");
      service.processDeleteNotificationEvent(request, getCorrelationId());
      return successResponse(eventResponse);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
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
          partialValidations.addAll(
              validateUserCommandedExecutionEvent(request.getTarget(), request.getProjectId()));
          break;
        case POLICY: // fall through
        default:
          break;
      }
    }

    ValidationResult result = ValidationResult.compose(partialValidations);
    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }

  /**
   * Runs validation checks against the "Notification" event request type
   *
   * @param request a NotificationEventRequest
   * @throws ValidationException when the request is invalid
   */
  private void validateNotificationEvent(NotificationEventRequest request)
      throws ValidationException {
    Collection<ValidationResult> partialValidations = new HashSet<>();

    partialValidations.add(
        FieldValidations.validateFieldFollowsBucketNamingStructure(
            "deletedObject", request.getDeletedObject()));

    if (request.getProjectId() == null) {
      partialValidations.add(ValidationResult.fromString("projectId must be provided."));
    }

    if (request.getDeletedAt() == null) {
      partialValidations.add(ValidationResult.fromString("deletedAt must be provided."));
    } else {
      try {
        Instant.parse(request.getDeletedAt());
      } catch (DateTimeParseException e) {
        partialValidations.add(ValidationResult.fromString("deletedAt is not ISO 8601 format."));
      }
    }

    ValidationResult result = ValidationResult.compose(partialValidations);
    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }

  private EventResponse generateResponse(String eventMessage) {
    EventResponse response = new EventResponse();
    response.setMessage(eventMessage);
    return response;
  }

  private Collection<ValidationResult> validateUserCommandedExecutionEvent(
      String target, String projectId) {
    Collection<ValidationResult> validations = new HashSet<>();

    ValidationResult validateTargetNameResult =
        FieldValidations.validateFieldFollowsBucketNamingStructure("target", target);
    validations.add(validateTargetNameResult);

    if (validateTargetNameResult.isValid) {
      if (!RetentionUtil.isValidDeleteMarker(target)) {
        validations.add(
            ValidationResult.fromString(
                String.format(
                    "The target %s does not have a valid delete marker. The delete marker needs to match the pattern %s",
                    target, RetentionUtil.DM_REGEX_PATTERN)));
      } else {
        String datasetPath = RetentionUtil.getDmDatasetPath(target);
        if (datasetPath == null) {
          validations.add(
              ValidationResult.fromString(
                  String.format(
                      "The target %s is intended to delete a bucket. Can not delete a bucket",
                      target)));
        } else {
          String bucketName = RetentionUtil.getBucketName(target);
          if (!GcsHelper.getInstance(projectId).doesBucketExist(bucketName, projectId)) {
            validations.add(
                ValidationResult.fromString(
                    String.format("The bucket %s/%s does not exist", projectId, bucketName)));
          } else {
            String dataStorageName =
                ValidationConstants.STORAGE_PREFIX
                    + bucketName
                    + ValidationConstants.STORAGE_SEPARATOR
                    + datasetPath;
            List<DmRequest> dmRequests =
                SingletonDao.getDmQueueDao().getPendingDmRequestByName(dataStorageName, projectId);
            if (dmRequests != null && !dmRequests.isEmpty()) {
              validations.add(
                  ValidationResult.fromString(
                      String.format(
                          "The target %s for project %s already exist.", target, projectId)));
            }
          }
        }
      }
    }

    if (projectId == null) {
      validations.add(ValidationResult.fromString("projectId must be provided if type is USER"));
    }

    return validations;
  }
}
