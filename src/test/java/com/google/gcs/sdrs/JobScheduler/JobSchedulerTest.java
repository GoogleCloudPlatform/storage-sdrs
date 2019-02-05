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

package com.google.gcs.sdrs.JobScheduler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JobSchedulerTest {

  private JobScheduler instance;

  /**
   * Set up steps before each test
   */
  @Before
  public void setUp(){
    instance = JobScheduler.getInstance();
  }

  /**
   * Tear down steps after each test
   */
  @After
  public void tearDown(){
    instance.shutdownSchedulerNow();
  }

  /**
   * Test that a JobManager instance is created when a getJobManager is called without an existing instance
   */
  @Test
  public void getInstanceWhenInstanceDoesNotExist() {
    // Instance created in test setup
    assertNotNull(instance);
  }

  /**
   * Test that the same JobManager instance is returned when getJobManager is called more than once
   */
  @Test
  public void getInstanceWhenInstanceAlreadyExists() {
    assertNotNull(instance);
    JobScheduler secondInstance = JobScheduler.getInstance();
    assertEquals(instance, secondInstance);
  }
}
