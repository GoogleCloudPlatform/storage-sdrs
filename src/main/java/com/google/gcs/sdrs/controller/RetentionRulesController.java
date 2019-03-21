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

import com.google.gcs.sdrs.RetentionRuleType;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleDeleteResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.controller.validation.FieldValidations;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.controller.validation.ValidationResult;
import com.google.gcs.sdrs.service.RetentionRulesService;
import com.google.gcs.sdrs.service.impl.RetentionRulesServiceImpl;
import java.util.Collection;
import java.util.HashSet;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/** Controller for handling /retentionrules endpoints to manage retention rules. */
@Path("/retentionrules")
public class RetentionRulesController extends BaseController {

  RetentionRulesService service = new RetentionRulesServiceImpl();

  /** CRUD create endpoint */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(RetentionRuleCreateRequest request) {
    try {
      validateCreate(request);
      UserInfo userInfo = getUserInfo();
      int result = service.createRetentionRule(request, userInfo);
      RetentionRuleCreateResponse response = new RetentionRuleCreateResponse();
      response.setRuleId(result);
      return successResponse(response);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /** CRUD get by business key endpoint */
  @GET
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response get(
      @QueryParam("type") String retentionRuleType,
      @QueryParam("projectId") String projectId,
      @QueryParam("dataStorageName") String dataStorageName) {
    try {

      Collection<ValidationResult> partialValidations =
          validateCompositeKey(retentionRuleType, projectId, dataStorageName);
      ValidationResult result = ValidationResult.compose(partialValidations);

      if (!result.isValid) {
        throw new ValidationException(result);
      }

      if (retentionRuleType.equalsIgnoreCase(RetentionRuleType.GLOBAL.toString())) {
        if (projectId == null) {
          projectId = SdrsApplication.getAppConfigProperty("sts.defaultProjectId");
        }
        if (dataStorageName == null) {
          dataStorageName = SdrsApplication.getAppConfigProperty("sts.defaultStorageName");
        }
      }
      RetentionRuleResponse response =
          service.getRetentionRuleByBusinessKey(projectId, dataStorageName);
      if (response == null) {
        throw new ResourceNotFoundException(
            String.format(
                "Retention rule doesn't exist for projectId: %s, dataStorageName: %s",
                projectId, dataStorageName));
      }
      return successResponse(response);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /** CRUD update endpoint */
  @PUT
  @Path("/{ruleId}")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response update(@PathParam("ruleId") Integer ruleId, RetentionRuleUpdateRequest request) {
    try {
      validateUpdate(request);
      RetentionRuleResponse response = service.updateRetentionRule(ruleId, request);
      return successResponse(response);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /** CRUD delete endpoint */
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteByBusinessKey(
      @QueryParam("type") String retentionRuleType,
      @QueryParam("projectId") String projectId,
      @QueryParam("dataStorageName") String dataStorageName) {
    try {

      Collection<ValidationResult> partialValidations =
          validateCompositeKey(retentionRuleType, projectId, dataStorageName);
      ValidationResult result = ValidationResult.compose(partialValidations);

      if (!result.isValid) {
        throw new ValidationException(result);
      }

      if (retentionRuleType.equalsIgnoreCase(RetentionRuleType.GLOBAL.toString())) {
        if (projectId == null) {
          projectId = SdrsApplication.getAppConfigProperty("sts.defaultProjectId");
        }
        if (dataStorageName == null) {
          dataStorageName = SdrsApplication.getAppConfigProperty("sts.defaultStorageName");
        }
      }

      Integer ruleId = service.deleteRetentionRuleByBusinessKey(projectId, dataStorageName);

      if (ruleId == null) {
        throw new ResourceNotFoundException(
            String.format(
                "Retention rule doesn't exist for projectId: %s, dataStorageName: %s",
                projectId, dataStorageName));
      } else {
        RetentionRuleDeleteResponse response = new RetentionRuleDeleteResponse();
        response.setRuleId(ruleId);
        return successResponse(response);
      }
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /**
   * Validates the object for creating a retention rule
   *
   * @param request the create request object
   * @throws ValidationException when the request is invalid
   */
  private void validateCreate(RetentionRuleCreateRequest request) throws ValidationException {
    String retentionRuleType = null;

    if (request.getRetentionRuleType() != null) {
      retentionRuleType = request.getRetentionRuleType().toString();
    }
    Collection<ValidationResult> partialValidations =
        validateCompositeKey(
            retentionRuleType, request.getProjectId(), request.getDataStorageName());
    partialValidations.add(validateRetentionPeriod(request.getRetentionPeriod()));
    ValidationResult result = ValidationResult.compose(partialValidations);

    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }

  private Collection<ValidationResult> validateCompositeKey(
      String retentionRuleType, String projectId, String dataStorageName) {
    Collection<ValidationResult> partialValidations = new HashSet<>();
    if (retentionRuleType == null) {
      partialValidations.add(
          ValidationResult.fromString(String.format(
              "type, one of [%s, %s],  must be provided.",
              RetentionRuleType.GLOBAL.toString(), RetentionRuleType.DATASET.toString())));
    } else {
      switch (retentionRuleType.toUpperCase()) {
        case ValidationConstants.GLOBAL_JSON_VALUE:
          break;
        case ValidationConstants.DATASET_JSON_VALUE:
          partialValidations.add(
              FieldValidations.validateFieldFollowsBucketNamingStructure(
                  "dataStorageName", dataStorageName));

          if (projectId == null) {
            partialValidations.add(
                ValidationResult.fromString("projectId must be provided if type is DATASET"));
          }
          break;
        default:
          partialValidations.add(ValidationResult.fromString(String.format(
              "type, one of [%s, %s],  must be provided.",
              RetentionRuleType.GLOBAL.toString(), RetentionRuleType.DATASET.toString())));
      }
    }
    return partialValidations;
  }

  private void validateUpdate(RetentionRuleUpdateRequest request) throws ValidationException {
    ValidationResult result = validateRetentionPeriod(request.getRetentionPeriod());

    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }

  private ValidationResult validateRetentionPeriod(Integer retentionPeriod) {
    Collection<String> messages = new HashSet<>();
    if (retentionPeriod == null) {
      messages.add("retentionPeriod must be provided");
    } else {
      if (retentionPeriod < 0) {
        messages.add("retentionPeriod must be at least 0");
      }
      if (retentionPeriod > ValidationConstants.RETENTION_MAX_VALUE) {
        messages.add(
            String.format(
                "retentionPeriod exceeds maximum value of %d",
                ValidationConstants.RETENTION_MAX_VALUE));
      }
    }
    return new ValidationResult(messages);
  }
}
