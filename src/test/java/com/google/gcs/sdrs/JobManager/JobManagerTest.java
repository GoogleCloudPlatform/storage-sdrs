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

package com.google.gcs.sdrs.JobManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.DemoWorker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for JobManager
 */
public class JobManagerTest {
  private JobManager jobManager;

  /**
   * Set up steps before each test
   */
  @Before
  public void setUp(){
    jobManager = JobManager.getJobManager();
  }

  /**
   * Tear down steps after each test
   */
  @After
  public void tearDown(){
    jobManager.shutDownJobManagerNow();
  }

  /**
   * Test that a JobManager singleton is created when a getJobManager is called without an existing jobManager
   */
  @Test
  public void getInstanceWhenInstanceDoesNotExist() {
    // Instance created in test setup
    assertNotNull(jobManager);
    assertNotNull(jobManager.completionService);
    assertEquals(jobManager.activeWorkerCount.get(), 0);
  }

  /**
   * Test that the same JobManager singleton is returned when getJobManager is called more than once
   */
  @Test
  public void getInstanceWhenInstanceAlreadyExists() {
    assertNotNull(jobManager);
    JobManager secondInstance = JobManager.getJobManager();
    assertEquals(jobManager, secondInstance);
  }

  /**
   * Test that the worker count is incremented when a job is submitted
   */
  @Test
  public void testSubmitJob() {
    assertNotNull(jobManager);
    BaseWorker worker = new DemoWorker();
    int currentActiveWorkers = jobManager.activeWorkerCount.get();
    jobManager.submitJob(worker);
    assertEquals(currentActiveWorkers + 1, jobManager.activeWorkerCount.get());
  }
}