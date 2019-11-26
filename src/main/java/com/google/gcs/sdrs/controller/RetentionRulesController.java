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

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.common.RetentionUnitType;
import com.google.gcs.sdrs.common.RetentionValue;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleDeleteResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.controller.validation.FieldValidations;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.controller.validation.ValidationResult;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.RetentionRulesService;
import com.google.gcs.sdrs.service.impl.RetentionRulesServiceImpl;
import com.google.gcs.sdrs.util.RetentionUtil;
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
      preProcessCreateRequest(request);
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
          service.getRetentionRuleByBusinessKey(
              projectId,
              dataStorageName,
              RetentionRuleType.valueOf(retentionRuleType.toUpperCase()));
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
      RetentionRule rule = service.getRetentionRuleByRuleId(ruleId);
      RetentionValue retentionValue = RetentionValue.parse(rule.getRetentionValue());
      validateUpdate(request, retentionValue);
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

      Integer ruleId =
          service.deleteRetentionRuleByBusinessKey(
              projectId,
              dataStorageName,
              RetentionRuleType.valueOf(retentionRuleType.toUpperCase()));

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
    partialValidations.add(validateRetentionPeriod(request.getRetentionPeriodUnit(),
        request.getRetentionPeriod()));
    partialValidations.add(validateRetentionUnit(request.getRetentionPeriodUnit()));

    ValidationResult result = ValidationResult.compose(partialValidations);

    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }

  private void preProcessCreateRequest(RetentionRuleCreateRequest request) {
    if (request.getRetentionRuleType() == RetentionRuleType.GLOBAL) {
      request.setProjectId(SdrsApplication.getAppConfigProperty("sts.defaultProjectId"));
      request.setDataStorageName(SdrsApplication.getAppConfigProperty("sts.defaultStorageName"));
    } else if (request.getRetentionRuleType() == RetentionRuleType.DEFAULT) {
      request.setDataStorageName(
          ValidationConstants.STORAGE_PREFIX
              + RetentionUtil.getBucketName(request.getDataStorageName()));
    }
    if (request.getRetentionPeriodUnit() == null) {
      request.setRetentionPeriodUnit(RetentionUnitType.DAY.toString());
    }
  }

  private Collection<ValidationResult> validateCompositeKey(
      String retentionRuleType, String projectId, String dataStorageName) {
    Collection<ValidationResult> partialValidations = new HashSet<>();
    boolean validateDataStorageName = false;
    if (retentionRuleType == null) {
      partialValidations.add(
          ValidationResult.fromString(
              String.format(
                  "type, one of [%s, %s, %s],  must be provided.",
                  RetentionRuleType.GLOBAL.toString(),
                  RetentionRuleType.DATASET.toString(),
                  RetentionRuleType.DEFAULT.toString())));
    } else {
      switch (retentionRuleType.toUpperCase()) {
        case ValidationConstants.GLOBAL_JSON_VALUE:
          break;
        case ValidationConstants.DATASET_JSON_VALUE:
          validateDataStorageName = true;
          break;
        case ValidationConstants.DEFAULT_JSON_VALUE:
          validateDataStorageName = true;
          break;
        default:
          partialValidations.add(
              ValidationResult.fromString(
                  String.format(
                      "type, one of [%s, %s, %s],  must be provided.",
                      RetentionRuleType.GLOBAL.toString(),
                      RetentionRuleType.DATASET.toString(),
                      RetentionRuleType.DEFAULT.toString())));
      }

      if (validateDataStorageName) {
        partialValidations.add(
            FieldValidations.validateFieldFollowsBucketNamingStructure(
                "dataStorageName", dataStorageName));

        if (projectId == null) {
          partialValidations.add(
              ValidationResult.fromString(
                  String.format(
                      "projectId must be provided if type is %s or %s",
                      RetentionRuleType.DATASET.toString(), RetentionRuleType.DEFAULT.toString())));
        }
      }
    }
    return partialValidations;
  }

  private void validateUpdate(RetentionRuleUpdateRequest request, RetentionValue retentionValue)
      throws ValidationException {
    ValidationResult result = validateRetentionPeriod(retentionValue.getUnitTypeString(),
        request.getRetentionPeriod());

    if (!result.isValid) {
      throw new ValidationException(result);
    }
  }

  private ValidationResult validateRetentionPeriod(String retentionUnit, Integer retentionPeriod) {
    Collection<String> messages = new HashSet<>();
    if (retentionPeriod == null) {
      messages.add("retentionPeriod must be provided");
    } else {
      if (retentionPeriod < 0) {
        messages.add("retentionPeriod must be at least 0");
      }
      RetentionUnitType type = RetentionUnitType.getType(retentionUnit);
      switch (type) {
        case DAY:
          if (retentionPeriod > ValidationConstants.RETENTION_MAX_VALUE_DAY) {
            messages.add(
                String.format(
                    "retentionPeriod exceeds maximum value of retentionPeriodUnit %s of %d",
                    RetentionUnitType.DAY.toString(), ValidationConstants.RETENTION_MAX_VALUE_DAY));
          }
          break;
        case MONTH:
          if (retentionPeriod > ValidationConstants.RETENTION_MAX_VALUE_MONTH) {
            messages.add(
                String.format(
                    "retentionPeriod exceeds maximum value of retentionPeriodUnit %s of %d",
                    RetentionUnitType.MONTH.toString(), ValidationConstants.RETENTION_MAX_VALUE_MONTH));
          }
          break;
        case VERSION:
          if (retentionPeriod > ValidationConstants.RETENTION_MAX_VALUE_VERSION) {
            messages.add(
                String.format(
                    "retentionPeriod exceeds maximum value of retentionPeriodUnit %s of %d",
                    RetentionUnitType.VERSION.toString(), ValidationConstants.RETENTION_MAX_VALUE_VERSION));
          }
          break;
        default:
          messages.add("retentionPeriodUnit must be provided");
          break;
      }
    }
    return new ValidationResult(messages);
  }

  private ValidationResult validateRetentionUnit(String retentionUnit) {
    Collection<String> messages = new HashSet<>();
    if (retentionUnit != null) {
      RetentionUnitType type = RetentionUnitType.getType(retentionUnit);
      if (type == null) {
        messages.add(
            String.format(
                "retentionPeriodUnit has to be one of [%s, %s, %s]",
                RetentionUnitType.DAY.toString(), RetentionUnitType.MONTH.toString(),
                RetentionUnitType.VERSION.toString()));
      }
    }

    return new ValidationResult(messages);
  }
}
