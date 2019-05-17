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

import com.google.gcs.sdrs.controller.pojo.PooledJobCreateRequest;
import com.google.gcs.sdrs.controller.pojo.PooledJobCreateResponse;
import com.google.gcs.sdrs.controller.pojo.PooledJobDeleteResponse;
import com.google.gcs.sdrs.controller.pojo.PooledJobResponse;
import com.google.gcs.sdrs.service.JobPoolService;
import com.google.gcs.sdrs.service.impl.JobPoolServiceImpl;
import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


/** Controller for handling /jobpool endpoints to manage STS job pooling. */
@Path("/stsjobpool")
public class JobPoolController extends BaseController {

  private JobPoolService jobPoolService = JobPoolServiceImpl.getInstance(); // TODO need to wire up the instantiation via a factory

 /* *//** CRUD create endpoint *//*
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(PooledJobCreateRequest request) {
    try {
      validateCreate(request);
      Integer id = jobPoolService.createJob(request);
      PooledJobCreateResponse pooledJobCreateResponse = new PooledJobCreateResponse();
      pooledJobCreateResponse.setId(id);
      pooledJobCreateResponse.setSuccess(true);
      return successResponse(pooledJobCreateResponse);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }*/

  /** CRUD create batch endpoint */
  @POST
  //@Path("/batch")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createByBatch(List<PooledJobCreateRequest> requests) {
    try {
      Boolean success = jobPoolService.createJobs(requests);
      PooledJobCreateResponse response = new PooledJobCreateResponse();
      response.setSuccess(success);
      return successResponse(response);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }

  /** CRUD read endpoint */
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public Collection<PooledJobResponse> getPooledJobs(
      @QueryParam("sourceBucket") String sourceBucket,
      @QueryParam("sourceProject") String sourceProject) {
    return jobPoolService.getAllPooledStsJobsByBucketName(sourceBucket, sourceProject);
  }

  /** CRUD delete endpoint */
  @DELETE
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(
      @QueryParam("sourceBucket") String sourceBucket,
      @QueryParam("sourceProject") String sourceProject) {
    try {

      Boolean success = jobPoolService.deleteAllJobsByBucketName(sourceBucket, sourceProject);
      PooledJobDeleteResponse response = new PooledJobDeleteResponse();
      response.setSuccess(success);
      return successResponse(response);
    } catch (Exception exception) {
      return errorResponse(exception);
    }
  }
}
