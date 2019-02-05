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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gcs.sdrs.JobScheduler.JobScheduler;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gcs.sdrs.worker.BaseWorker;
import com.google.gcs.sdrs.worker.WorkerResult;

/**
 * JobManager for creating and managing worker threads.
 */
public class JobManager {
  CompletionService<WorkerResult> completionService;
  AtomicInteger activeWorkerCount = new AtomicInteger(0);

  private ExecutorService executorService;

  private static JobManager jobManager;
  private static JobScheduler scheduler;
  private static JobManagerMonitor monitor;
  private static int DEFAULT_THREAD_POOL_SIZE = 10;
  private static int DEFAULT_SLEEP_MINUTES = 5;
  private static int DEFAULT_MONITOR_INITIAL_DELAY = 0;
  private static int DEFAULT_MONITOR_FREQUENCY = 30;
  private static TimeUnit DEFAULT_MONITOR_TIME_UNIT = TimeUnit.MINUTES;
  private static int THREAD_POOL_SIZE;
  private static int SLEEP_MINUTES;
  private static int MONITOR_INITIAL_DELAY;
  private static int MONITOR_FREQUENCY;
  private static TimeUnit MONITOR_TIME_UNIT = TimeUnit.SECONDS;
  private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

  /**
   * Gets the current JobManager singleton and creates one if it doesn't exist.
   * @return The JobManager singleton.
   */
  public static synchronized JobManager getJobManager() {
    if (jobManager == null) {
      logger.info("JobManager not created. Creating...");
      jobManager = new JobManager();

      monitor = new JobManagerMonitor(jobManager);
      scheduler = JobScheduler.getInstance();
      scheduler.submitScheduledJob(monitor,
          MONITOR_INITIAL_DELAY, MONITOR_FREQUENCY, MONITOR_TIME_UNIT);
    }

    return jobManager;
  }

  /**
   * Immediately shuts down the job manager and doesn't wait for threads to resolve
   */
  public void shutDownJobManagerNow(){
    logger.info("Forcing shutdown now...");
    executorService.shutdownNow();
    scheduler.shutdownSchedulerNow();

    // Ensure the job manager singleton is destroyed
    jobManager = null;

    logger.info("JobManager shut down.");
  }

  /**
   * Gracefully shuts down the Job Manager and associated threads
   */
  public void shutDownJobManager() {
    logger.info("Shutting down JobManager.");
    // waits nicely for executing tasks to finish, and won't spawn new ones
    logger.info("Attempting graceful shutdown...");
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(SLEEP_MINUTES, TimeUnit.MINUTES)) {
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
    scheduler.shutdownScheduler();

    // Ensure the job manager singleton is destroyed
    jobManager = null;

    logger.info("JobManager shut down.");
  }

  /**
   * Submits a callable worker for execution
   * @param job A callable that returns a WorkerResult record.
   */
  public void submitJob(BaseWorker job) {
    completionService.submit(job);
    activeWorkerCount.incrementAndGet();
    logger.debug("Active Workers after submission: " + activeWorkerCount.get());
    logger.info("Job submitted: " + job.getWorkerResult().toString());
  }

  private JobManager () {
    try{
      Configuration config = new Configurations().xml("applicationConfig.xml");
      THREAD_POOL_SIZE = config.getInt("jobManager.threadPoolSize");
      SLEEP_MINUTES = config.getInt("jobManager.shutdownSleepMinutes");
      MONITOR_INITIAL_DELAY = config.getInt("jobManager.monitor.initialDelay");
      MONITOR_FREQUENCY = config.getInt("jobManager.monitor.frequency");
      MONITOR_TIME_UNIT = TimeUnit.valueOf(config.getString("jobManager.monitor.timeUnit"));
    } catch (ConfigurationException ex) {
      logger.error("Configuration file could not be read. Using defaults: " + ex.getMessage());
      THREAD_POOL_SIZE = DEFAULT_THREAD_POOL_SIZE;
      SLEEP_MINUTES = DEFAULT_SLEEP_MINUTES;
      MONITOR_INITIAL_DELAY = DEFAULT_MONITOR_INITIAL_DELAY;
      MONITOR_FREQUENCY = DEFAULT_MONITOR_FREQUENCY;
      MONITOR_TIME_UNIT = DEFAULT_MONITOR_TIME_UNIT;
    }

    executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    completionService = new ExecutorCompletionService<>(executorService);
    logger.info("JobManager created.");
  }
}