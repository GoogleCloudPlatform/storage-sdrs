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

import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.controller.validation.FieldValidations;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.controller.validation.ValidationResult;
import com.google.gcs.sdrs.service.RetentionRulesService;
import com.google.gcs.sdrs.service.impl.RetentionRulesServiceImpl;

import java.sql.SQLException;
import java.util.Collection;
import java.util.HashSet;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Controller for handling /retentionrules endpoints to manage retention rules. */
@Path("/retentionrules")
public class RetentionRulesController extends BaseController {

  private static final Logger logger = LoggerFactory.getLogger(RetentionRulesController.class);

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
      return Response.status(HttpStatus.OK_200).entity(response).build();
    } catch (HttpException exception) {
      return generateExceptionResponse(exception);
    } catch (Exception exception) {
      logger.error(exception.getMessage());
      return generateExceptionResponse(new InternalServerException(exception));
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
      return Response.status(HttpStatus.OK_200).entity(response).build();
    } catch (HttpException exception) {
      return generateExceptionResponse(exception);
    } catch (Exception exception) {
      logger.error(exception.getMessage());
      return generateExceptionResponse(new InternalServerException(exception));
    }
  }

  /** CRUD delete endpoint */
  @DELETE
  @Produces(MediaType.APPLICATION_JSON)
  public Response deleteByBusinessKey(
      @QueryParam("project") String project,
      @QueryParam("bucket") String bucket,
      @QueryParam("dataset") String dataset) {

    String requestUuid = generateRequestUuid();

    RetentionRuleResponse response = new RetentionRuleResponse();
    response.setRequestUuid(requestUuid);

    try {
      service.deleteRetentionRuleByBusinessKey(project, bucket, dataset);
    } catch (Exception e) {
      return Response.status(400).entity(null).build();
    }
    return Response.status(200).entity(response).build();
  }

  /**
   * Validates the object for creating a retention rule
   *
   * @param request the create request object
   * @throws ValidationException when the request is invalid
   */
  private void validateCreate(RetentionRuleCreateRequest request) throws ValidationException {
    Collection<ValidationResult> partialValidations = new HashSet<>();

    partialValidations.add(validateRetentionPeriod(request.getRetentionPeriod()));

    if (request.getRetentionRuleType() == null) {
      partialValidations.add(ValidationResult.fromString("type must be provided"));
    } else {
      switch (request.getRetentionRuleType()) {
        case GLOBAL:
          break;
        case DATASET:
          partialValidations.add(
              FieldValidations.validateFieldFollowsBucketNamingStructure(
                  "dataStorageName", request.getDataStorageName()));

          if (request.getProjectId() == null) {
            partialValidations.add(
                ValidationResult.fromString("projectId must be provided if type is DATASET"));
          }
          break;
        default:
          break;
      }
    }

    ValidationResult result = ValidationResult.compose(partialValidations);

    if (!result.isValid) {
      throw new ValidationException(result);
    }
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
