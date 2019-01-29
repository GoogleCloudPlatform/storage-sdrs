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

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages scheduled jobs within SDRS
 */
public class JobScheduler {

  private ScheduledExecutorService scheduledExecutor;
  private static JobScheduler instance;
  private static int THREAD_POOL_SIZE = 5;
  private static int SHUTDOWN_WAIT = 1;
  private static TimeUnit SHUTDOWN_TIME_UNIT = TimeUnit.MINUTES;
  private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

  /**
   * Gets the instance of the Job Scheduler. If none exists, one is created.
   * @return The active Job Scheduler instance
   */
  public static synchronized JobScheduler getInstance() {
    if (instance == null) {
      try {
        logger.info("JobScheduler not created. Creating...");

        instance = new JobScheduler();

      } catch (ConfigurationException configEx) {
        logger.error("Configurations couldn't be loaded from the file. Using default values..."
            , configEx.getCause());
      }
    }

    return instance;
  }

  /**
   * Gracefully shuts down the Job Scheduler
   */
  public void shutdownScheduler() {
    shutdownScheduler(false);
  }

  /**
   * Shuts down the job scheduler
   * @param isImmediateShutdown Tells the scheduler to shut down immediately, if needed
   */
  public void shutdownScheduler(boolean isImmediateShutdown) {
    logger.info("Shutting down...");
    if(isImmediateShutdown){
      logger.info("Forcing JobScheduler shutdown now...");
      scheduledExecutor.shutdownNow();
    } else {
      // waits nicely for executing tasks to finish, and won't spawn new ones
      logger.info("Attempting JobScheduler graceful shutdown...");
      scheduledExecutor.shutdown();
      try {
        if (!scheduledExecutor.awaitTermination(SHUTDOWN_WAIT, SHUTDOWN_TIME_UNIT)) {
          scheduledExecutor.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduledExecutor.shutdownNow();
      }
    }

    // Ensure the job scheduler instance is destroyed
    instance = null;

    logger.info("JobScheduler shut down.");
  }

  /**
   * Submits a job to be run on a scheduled, recurring basis
   * @param job The runnable job to be executed
   * @param initialDelay How long to wait until the first execution
   * @param frequency How long to wait between job executions
   * @param timeUnit The timeunit that determines the time value of initialDelay and frequency
   */
  public void submitScheduledJob(Runnable job, int initialDelay, int frequency, TimeUnit timeUnit){
    scheduledExecutor.scheduleAtFixedRate(job,
        initialDelay, frequency, timeUnit);
  }

  private JobScheduler() throws ConfigurationException {
    Configuration config = new Configurations().xml("applicationConfig.xml");
    THREAD_POOL_SIZE = config.getInt("scheduler.threadPoolSize");
    SHUTDOWN_WAIT = config.getInt("scheduler.shutdownWait");
    SHUTDOWN_TIME_UNIT = TimeUnit.valueOf(config.getString("scheduler.shutdownTimeUnit"));

    scheduledExecutor = Executors.newScheduledThreadPool(THREAD_POOL_SIZE);
    logger.info("JobScheduler instance created.");
  }
}
