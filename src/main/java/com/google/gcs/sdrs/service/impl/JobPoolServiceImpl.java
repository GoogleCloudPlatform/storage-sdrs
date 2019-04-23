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

import java.util.Collection;

import com.google.gcs.sdrs.controller.pojo.PooledJobCreateRequest;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.PooledStsJobDao;
import com.google.gcs.sdrs.dao.model.PooledStsJob;
import com.google.gcs.sdrs.service.JobPoolService;

public class JobPoolServiceImpl implements JobPoolService {

  private PooledStsJobDao pooledStsJobDao = SingletonDao.getPooledStsJobDao();

  @Override
  public Integer createJob(PooledJobCreateRequest request) {
    PooledStsJob pooledStsJob = convertToEntity(request);
    pooledStsJob.setId(pooledStsJobDao.save(pooledStsJob)); 
    return pooledStsJob.getId();
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

    return pooledStsJob;
  }

  public PooledStsJobDao getPooledStsJobDao() {
    return pooledStsJobDao;
  }

  public void setPooledStsJobDao(PooledStsJobDao stsJobPoolDao) {
    this.pooledStsJobDao = stsJobPoolDao;
  }

  @Override
  public Collection<PooledStsJob> getAllPooledStsJobsByBucketName(
      String sourceBucket, String sourceProject) { 
	 return pooledStsJobDao.getAllPooledStsJobsByBucketName(sourceBucket, sourceProject);
  }
}
