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
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.HierarchicalConfiguration;
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

  private static JobManager instance;
  private static JobManagerMonitor monitor;
  private static Thread monitorThread;
  private static Configurations configs = new Configurations();
  private static int THREAD_POOL_SIZE;
  private static int SLEEP_MINUTES;
  private static int MONITOR_THREAD_SLEEP_IN_SECONDS;
  private static final String MONITOR_THREAD_NAME = "monitoringThread";
  private static final Logger logger = LoggerFactory.getLogger(JobManager.class);

  /**
   * Gets the current JobManager instance and creates one if it doesn't exist.
   * @return The JobManager instance.
   */
  public static synchronized JobManager getInstance() {
    if (instance == null) {
      try {
        logger.info("JobManager not created. Creating...");

        instance = new JobManager();

        monitor = new JobManagerMonitor(instance, MONITOR_THREAD_SLEEP_IN_SECONDS);
        monitorThread = new Thread(monitor, MONITOR_THREAD_NAME);
        monitorThread.start();
      } catch (ConfigurationException configEx) {
        logger.error("Configurations couldn't be loaded from the file.", configEx.getCause());
      } catch (Exception e) {
        logger.error("An error occurred while retrieving the JobManager instance: ", e.getCause());
      }
    }

    return instance;
  }

  /**
   * Gracefully shuts down the jobManager and associated threads
   */
  public void shutDownJobManager(){
    shutDownJobManager(false);
  }

  /**
   * Gracefully shuts down the jobManager and associated threads
   * @param shutdownNow Optionally allows the service to immediately shutdown rather than waiting for thread completion
   */
  public void shutDownJobManager(boolean shutdownNow) {
    logger.info("Shutting down JobManager.");
    if(shutdownNow){
      logger.info("Forcing shutdown now...");
      executorService.shutdownNow();
    } else {
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
    }
    monitorThread.interrupt();

    // Ensure the job manager instance is destroyed
    instance = null;

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

  private JobManager () throws ConfigurationException{
    HierarchicalConfiguration config = configs.xml("default-applicationConfig.xml");
    THREAD_POOL_SIZE = config.getInt("jobManager.threadPoolSize");
    SLEEP_MINUTES = config.getInt("jobManager.shutdownSleepMinutes");
    MONITOR_THREAD_SLEEP_IN_SECONDS = config.getInt("jobManager.monitorThreadSleepInSeconds");

    executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    completionService = new ExecutorCompletionService<>(executorService);
    logger.info("JobManager instance created.");
  }
}