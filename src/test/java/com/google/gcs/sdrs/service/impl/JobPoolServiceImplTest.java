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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gcs.sdrs.controller.pojo.PooledJobCreateRequest;
import com.google.gcs.sdrs.dao.PooledStsJobDao;
import com.google.gcs.sdrs.dao.impl.PooledStsJobDaoImpl;
import com.google.gcs.sdrs.service.JobPoolService;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.StsUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
@PrepareForTest({CredentialsUtil.class, StsUtil.class})
@PowerMockIgnore("javax.management.*")
public class JobPoolServiceImplTest {

  private JobPoolService jobPoolService; // Component under test.
  private PooledStsJobDao pooledStsJobDao; // Data tier to mock out.
  private CredentialsUtil mockCredentialsUtil;

  @Before
  public void setup() {
    mockCredentialsUtil = mock(CredentialsUtil.class);
    PowerMockito.mockStatic(CredentialsUtil.class);
    when(CredentialsUtil.getInstance()).thenReturn(mockCredentialsUtil);
    PowerMockito.mockStatic(StsUtil.class);
    when(StsUtil.createStsClient(any())).thenReturn(null);
    jobPoolService = createService();
  }

  private JobPoolService createService() {
    JobPoolServiceImpl jobPoolServiceImpl = JobPoolServiceImpl.getInstance();
    pooledStsJobDao = mock(PooledStsJobDaoImpl.class);
    jobPoolServiceImpl.setPooledStsJobDao(pooledStsJobDao);
    return jobPoolServiceImpl;
  }

  @Test
  public void testCreateJob() {
    PooledJobCreateRequest pooledJobCreateRequest = new PooledJobCreateRequest();
    pooledJobCreateRequest.setName("9890119");
    pooledJobCreateRequest.setProjectId("sdrs-project");
    pooledJobCreateRequest.setSchedule("12 PM");
    pooledJobCreateRequest.setSourceBucket("bucketX");
    pooledJobCreateRequest.setSourceProject("projectY");

    when(pooledStsJobDao.save(any())).thenReturn(989);

    Integer id = jobPoolService.createJob(pooledJobCreateRequest);
    assertEquals(new Integer(989), id);
  }
}
