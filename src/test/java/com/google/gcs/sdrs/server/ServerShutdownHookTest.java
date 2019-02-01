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

package com.google.gcs.sdrs.server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Test class for the ServerShutdownHook
 */
public class ServerShutdownHookTest {

  private HttpServer server;
  private ServerShutdownHook objectUnderTest;

  /**
   * Set up steps before each test
   */
  @Before
  public void setUp() throws Exception {
    // start the server
    server = new HttpServer();
    server.start();
    objectUnderTest = new ServerShutdownHook(server, 5, true);
  }

  /**
   * Tear down steps after each test
   */
  @After
  public void tearDown() {
    server.shutdown();
  }

  /**
   * Test that the shutdown hook does shutdown the server
   */
  @Test
  public void testServerShutdownHook() {
    objectUnderTest.run();
    assertFalse(server.isStarted());
  }
}
