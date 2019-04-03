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

package com.google.gcs.sdrs;

import com.google.gcs.sdrs.service.manager.JobManager;
import com.google.gcs.sdrs.service.mq.PubSubMessageQueueManagerImpl;
import com.google.gcs.sdrs.service.scheduler.JobScheduler;
import java.util.concurrent.TimeUnit;
import org.glassfish.grizzly.http.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper class around a Runnable thread designed to gracefully shutdown the Job Manager. */
public class ServerShutdownHook implements Runnable {

  private static HttpServer server;
  private static JobManager jobManager;
  private static JobScheduler jobScheduler;
  private static long GRACE_PERIOD_IN_SECONDS;
  private boolean isImmediateShutdown;
  private static final Logger logger = LoggerFactory.getLogger(ServerShutdownHook.class);

  /**
   * @param httpServer The server to shutdown
   * @param gracePeriodInSeconds How long to wait until forcing the http server to shutdown
   * @param isImmediateShutdown Immediately shuts down all threads if true
   */
  public ServerShutdownHook(
      HttpServer httpServer, long gracePeriodInSeconds, boolean isImmediateShutdown) {
    server = httpServer;
    GRACE_PERIOD_IN_SECONDS = gracePeriodInSeconds;
    this.isImmediateShutdown = isImmediateShutdown;
  }

  @Override
  public void run() {
    logger.info("Running shutdown hook...");

    logger.info("Shutting down Job Manager...");
    jobManager = JobManager.getInstance();
    if (isImmediateShutdown) {
      jobManager.shutDownJobManagerNow();
    } else {
      jobManager.shutDownJobManager();
    }
    logger.info("Job Manager shutdown complete.");
    if (Boolean.valueOf(SdrsApplication.getAppConfigProperty("scheduler.enabled", "false"))) {
      logger.info("Shutting down Job Scheduler...");
      jobScheduler = JobScheduler.getInstance();
      if (isImmediateShutdown) {
        jobScheduler.shutdownSchedulerNow();
      } else {
        jobScheduler.shutdownScheduler();
      }
      logger.info("Job Scheduler shutdown complete.");
    }

    PubSubMessageQueueManagerImpl.getInstance().shutdown();

    logger.info("Shutting down web server...");
    server.shutdown(GRACE_PERIOD_IN_SECONDS, TimeUnit.SECONDS);
    logger.info("Server shutdown complete.");
  }
}
