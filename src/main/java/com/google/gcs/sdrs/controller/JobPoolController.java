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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.gcs.sdrs.controller.pojo.PooledJobCreateRequest;
import com.google.gcs.sdrs.controller.pojo.PooledJobCreateResponse;
import com.google.gcs.sdrs.service.JobPoolService;
import com.google.gcs.sdrs.service.impl.JobPoolServiceImpl;

/** Controller for handling /jobpool endpoints to manage STS job pooling. */
@Path("/jobpool")
public class JobPoolController extends BaseController {

  JobPoolService service = new JobPoolServiceImpl(); // TODO need to wire up the instantiation via a factory

  /** CRUD create endpoint */
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(PooledJobCreateRequest request) {
    try {
      validate(request);

      Integer id = service.createJob(request);
      PooledJobCreateResponse response = new PooledJobCreateResponse();
      return successResponse(response);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /**
   * @param request
   * @throws ValidationException
   */
  private void validate(PooledJobCreateRequest request) throws ValidationException {
    // TODO
  }
}
