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

package com.google.gcs.sdrs.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.gcs.sdrs.controller.pojo.PooledJobCreateRequest;
import com.google.gcs.sdrs.controller.pojo.PooledJobResponse;
import com.google.gcs.sdrs.dao.PooledStsJobDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.PooledStsJob;
import com.google.gcs.sdrs.service.JobPoolService;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.StsUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JobPoolServiceImpl implements JobPoolService {

  private static final Logger logger = LoggerFactory.getLogger(JobPoolServiceImpl.class);
  private static JobPoolServiceImpl instance;
  private static CredentialsUtil credentialsUtil = CredentialsUtil.getInstance();

  private Storagetransfer client;
  private GoogleCredential credentials;
  private PooledStsJobDao pooledStsJobDao = SingletonDao.getPooledStsJobDao();

  private JobPoolServiceImpl() throws IOException {
    credentials = credentialsUtil.getCredentials();
    client = StsUtil.createStsClient(credentials);
  }

  public static JobPoolServiceImpl getInstance() {
    if (instance == null) {
      synchronized (JobPoolServiceImpl.class) {
        if (instance == null) {
          try {
            instance = new JobPoolServiceImpl();
          } catch (Exception e) {
            logger.error(e.getMessage());
          }
        }
      }
    }
    return instance;
  }

  @Override
  public Integer createJob(PooledJobCreateRequest request) {
    PooledStsJob pooledStsJob = convertToEntity(request);
    pooledStsJob.setId(pooledStsJobDao.save(pooledStsJob));
    return pooledStsJob.getId();
  }

  @Override
  public Boolean createJobs(
      String sourceBucket,
      String sourceProject,
      Collection<PooledJobCreateRequest> pooledJobCreateRequests) {
    if (isValidCreatePoolRequest(sourceBucket, sourceProject, pooledJobCreateRequests)) {
      for (PooledJobCreateRequest pooledJobCreateRequest : pooledJobCreateRequests) {
        pooledStsJobDao.save(convertToEntity(pooledJobCreateRequest));
      }
      return true;
    } else {
      return false;
    }
  }

  /**
   * Method that verifies that for all of the incoming requests 1) the STS Job exists in the cloud
   * and 2) that the job is not already registered with SDRS.
   *
   * <p>If any of the incoming requests fails validation, then the entire batch/transaction of jobs
   * is rejected
   *
   * @param pooledJobCreateRequests
   * @return
   */
  protected boolean isValidCreatePoolRequest(
      String sourceBucket,
      String sourceProject,
      Collection<PooledJobCreateRequest> pooledJobCreateRequests) {
    if (doesJobPoolAlreadyExist(sourceBucket, sourceProject)) {
      return false;
    } else {
      for (PooledJobCreateRequest pooledJobCreateRequest : pooledJobCreateRequests) {
        if (!doesJobExist(pooledJobCreateRequest)
            || isJobAlreadyRegistered(pooledJobCreateRequest)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Convenience business logic method to check if a job pool already exists for a given source
   * bucket/project
   *
   * @return
   */
  protected boolean doesJobPoolAlreadyExist(String bucketName, String sourceProject) {
    Collection<PooledStsJob> pooledStsJob =
        pooledStsJobDao.getAllPooledStsJobsByBucketName(bucketName, sourceProject);
    if (pooledStsJob == null || pooledStsJob.isEmpty()) {
      return false;
    } else {
      return true;
    }
  }

  protected boolean doesJobExist(PooledJobCreateRequest pooledJobCreateRequest) {
    try {
      TransferJob transferJob =
          StsUtil.getExistingJob(
              client, pooledJobCreateRequest.getProjectId(), pooledJobCreateRequest.getName());
      if (transferJob == null || transferJob.isEmpty()) {
        return false;
      }
      return true;
    } catch (IOException e) {
      logger.error(
          String.format(
              "Unable to communicate with Google STS API for job %s in GCP project %s ",
              pooledJobCreateRequest.getName(), pooledJobCreateRequest.getProjectId()),
          e);
      return false;
    }
  }

  protected boolean isJobAlreadyRegistered(PooledJobCreateRequest pooledJobCreateRequest) {
    PooledStsJob pooledStsJob =
        pooledStsJobDao.findPooledStsJobByNameAndProject(
            pooledJobCreateRequest.getName(), pooledJobCreateRequest.getProjectId());
    if (pooledStsJob != null) {
      return true;
    }
    return false;
  }

  @Override
  public Collection<PooledJobResponse> getAllPooledStsJobsByBucketName(
      String sourceBucket, String sourceProject) {
    Collection<PooledStsJob> pooledStsJobs =
        pooledStsJobDao.getAllPooledStsJobsByBucketName(sourceBucket, sourceProject);
    Collection<PooledJobResponse> pooledJobResponses = new ArrayList<PooledJobResponse>();
    for (PooledStsJob pooledStsJob : pooledStsJobs) {
      pooledJobResponses.add(convertToPojo(pooledStsJob));
    }
    return pooledJobResponses;
  }

  @Override
  public Boolean deleteAllJobsByBucketName(String sourceBucket, String sourceProject) {
    return pooledStsJobDao.deleteAllJobsByBucketName(sourceBucket, sourceProject);
  }

  protected PooledStsJob convertToEntity(PooledJobCreateRequest request) {
    // TODO refactor: introduce bean converter
    PooledStsJob pooledStsJob = new PooledStsJob();
    pooledStsJob.setName(request.getName());
    pooledStsJob.setProjectId(request.getProjectId());
    pooledStsJob.setType(request.getType());
    pooledStsJob.setSchedule(request.getSchedule());
    pooledStsJob.setSourceBucket(request.getSourceBucket());
    pooledStsJob.setSourceProject(request.getSourceProject());
    pooledStsJob.setStatus(request.getStatus());
    pooledStsJob.setTargetBucket(request.getTargetBucket());
    return pooledStsJob;
  }

  /**
   * TODO refactor using bean utils
   *
   * @param pooledStsJob
   * @return
   */
  private PooledJobResponse convertToPojo(PooledStsJob pooledStsJob) {
    if (pooledStsJob == null) {
      return null;
    }
    PooledJobResponse pooledJobResponse = new PooledJobResponse();
    pooledJobResponse.setId(pooledStsJob.getId());
    pooledJobResponse.setName(pooledStsJob.getName());
    pooledJobResponse.setProjectId(pooledStsJob.getProjectId());
    pooledJobResponse.setSchedule(pooledStsJob.getSchedule());
    pooledJobResponse.setType(pooledStsJob.getType());
    pooledJobResponse.setSourceBucket(pooledStsJob.getSourceBucket());
    pooledJobResponse.setSourceProject(pooledStsJob.getSourceProject());
    pooledJobResponse.setTargetBucket(pooledStsJob.getTargetBucket());
    pooledJobResponse.setTargetProject(pooledStsJob.getTargetProject());
    pooledJobResponse.setCreatedAt(pooledStsJob.getCreatedAt());
    pooledJobResponse.setUpdatedAt(pooledStsJob.getUpdatedAt());
    pooledJobResponse.setStatus(pooledStsJob.getStatus());
    return pooledJobResponse;
  }

  public PooledStsJobDao getPooledStsJobDao() {
    return pooledStsJobDao;
  }

  public void setPooledStsJobDao(PooledStsJobDao stsJobPoolDao) {
    this.pooledStsJobDao = stsJobPoolDao;
  }
}
