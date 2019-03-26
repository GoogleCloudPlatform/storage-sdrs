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

package com.google.gcs.sdrs.service.worker;

import java.util.UUID;

import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.gcs.sdrs.service.worker.WorkerResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test class for WorkerResult
 */
public class WorkerResultTest {
  private WorkerResult objectUnderTest;

  /**
   * Set up steps before each test
   */
  @Before
  public void setUp() {
    objectUnderTest = new WorkerResult();
  }

  /**
   * Tear down steps after each test
   */
  @After
  public void tearDown() {
    objectUnderTest = null;
  }

  /**
   * Tests that the constructor sets the ID as a UUID
   */
  @Test
  public void workerResultConstructorTest() {
    try{
      UUID workerResultId = UUID.fromString(objectUnderTest.getId());
      assertNotNull(workerResultId);
    } catch (IllegalArgumentException ex) {
      fail("The generated WorkerResult ID is not a UUID.");
    }
  }

  /**
   * Tests that properties read back correctly after being set
   */
  @Test
  public void workerResultPropertiesTest() {
    WorkerResult.WorkerResultStatus testStatus = WorkerResult.WorkerResultStatus.RUNNING;
    String testType = "testType";
    DateTime testStartTime = DateTime.now().minusHours(1);
    DateTime testEndTime = DateTime.now();

    objectUnderTest.setStatus(testStatus);
    objectUnderTest.setType(testType);
    objectUnderTest.setStartTime(testStartTime);
    objectUnderTest.setEndTime(testEndTime);

    assertEquals(testStatus, objectUnderTest.getStatus());
    assertEquals(testType, objectUnderTest.getType());
    assertEquals(testStartTime, objectUnderTest.getStartTime());
    assertEquals(testEndTime, objectUnderTest.getEndTime());
  }

  /**
   * Tests the toString method to ensure null values display properly
   */
  @Test
  public void workerResultToStringTest() {
    assertNotNull(objectUnderTest.toString());
    assertTrue(objectUnderTest.toString().length() > 0);

    objectUnderTest.setType("testType");
    assertNotNull(objectUnderTest.toString());
    assertTrue(objectUnderTest.toString().length() > 0);

    objectUnderTest.setStatus(WorkerResult.WorkerResultStatus.RUNNING);
    assertNotNull(objectUnderTest.toString());
    assertTrue(objectUnderTest.toString().length() > 0);

    objectUnderTest.setStartTime(DateTime.now().minusHours(1));
    assertNotNull(objectUnderTest.toString());
    assertTrue(objectUnderTest.toString().length() > 0);

    objectUnderTest.setEndTime(DateTime.now());
    assertNotNull(objectUnderTest.toString());
    assertTrue(objectUnderTest.toString().length() > 0);
  }
}