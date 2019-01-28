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

import java.util.concurrent.TimeUnit;

import com.google.gcs.sdrs.JobScheduler.JobScheduler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gcs.sdrs.JobManager.JobManager;

/**
 * Wrapper class around a Runnable thread designed to gracefully shutdown the Job Manager.
 */
public class ServerShutdownHook implements Runnable {

  private static HttpServer server;
  private static JobManager jobManager;
  private static JobScheduler jobScheduler;
  private static long GRACE_PERIOD_IN_SECONDS;
  private static final Logger logger = LoggerFactory.getLogger(ServerShutdownHook.class);

  public ServerShutdownHook(HttpServer httpServer, long gracePeriodInSeconds) {
    server = httpServer;
    GRACE_PERIOD_IN_SECONDS = gracePeriodInSeconds;
  }

  @Override
  public void run() {
    logger.info("Running shutdown hook...");

    logger.info("Shutting down Job Manager...");
    jobManager = JobManager.getInstance();
    jobManager.shutDownJobManager();
    logger.info("Job Manager shutdown complete.");

    logger.info("Shutting down Job Scheduler...");
    jobScheduler = JobScheduler.getInstance();
    jobScheduler.shutdownScheduler();
    logger.info("Job Scheduler shutdown complete.");

    logger.info("Shutting down web server...");
    server.shutdown(GRACE_PERIOD_IN_SECONDS, TimeUnit.SECONDS);
    logger.info("Server shutdown complete.");
  }
}
