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

import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.impl.ExecuteRetentionWorker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test class for JobManager
 */
public class JobManagerTest {
  private JobManager instance;

  /**
   * Set up steps before each test
   */
  @Before
  public void setUp(){
    instance = JobManager.getInstance();
  }

  /**
   * Tear down steps after each test
   */
  @After
  public void tearDown(){
    instance.shutDownJobManagerNow();
  }

  /**
   * Test that a JobManager instance is created when a getInstance is called without an existing instance
   */
  @Test
  public void getInstanceWhenInstanceDoesNotExist() {
    // Instance created in test setup
    assertNotNull(instance);
    assertNotNull(instance.completionService);
    assertEquals(instance.activeWorkerCount.get(), 0);
  }

//  /**
//   * Test that the same JobManager instance is returned when getInstance is called more than once
//   */
//  @Test
//  public void getInstanceWhenInstanceAlreadyExists() {
//    assertNotNull(instance);
//    JobManager secondInstance = JobManager.getInstance();
//    assertEquals(instance, secondInstance);
//  }

  /**
   * Test that the worker count is incremented when a job is submitted
   */
  @Test
  public void testSubmitJob() {
    assertNotNull(instance);
    BaseWorker worker = new ExecuteRetentionWorker(null);
    int currentActiveWorkers = instance.activeWorkerCount.get();
    instance.submitJob(worker);
    assertEquals(currentActiveWorkers + 1, instance.activeWorkerCount.get());
  }
}
