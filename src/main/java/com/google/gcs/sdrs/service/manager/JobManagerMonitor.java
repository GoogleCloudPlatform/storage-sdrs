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

package com.google.gcs.sdrs.service.manager;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gcs.sdrs.service.worker.WorkerResult;

/**
 * A Runnable background monitoring thread for the results of workers managed by the JobManager.
 */
class JobManagerMonitor implements Runnable {

  private JobManager jobManager;
  private static final Logger logger = LoggerFactory.getLogger(JobManagerMonitor.class);

  /**
   * Constructor for the job manager monitor thread
   * @param jobManager the job manager instance that this thread will monitor
   */
  JobManagerMonitor(JobManager jobManager){
    this.jobManager = jobManager;
  }

  /**
   * Runs the monitoring thread.
   */
  public void run() {
    logger.info("Looking for worker results...");
    if (jobManager.activeWorkerCount.get() > 0) {
      try{
        getWorkerResults();
      } catch (InterruptedException ex){
        logger.error("Could not retrieve results. Job Manager interrupted:" + ex.getCause());
      }
    }
  }

  /**
   * Gets the results for any workers currently in flight.
   * @throws InterruptedException when the thread is interrupted or shutdown
   */
  void getWorkerResults() throws InterruptedException {
    logger.debug("Starting examining futures, there are " + jobManager.activeWorkerCount.get() + " workers in flight.");
    while(jobManager.activeWorkerCount.get() > 0) {
      // block until a callable completes
      Future<WorkerResult> callResult = jobManager.completionService.take();
      jobManager.activeWorkerCount.decrementAndGet();
      logger.debug("Active Workers after result poll: " + jobManager.activeWorkerCount.get());
      WorkerResult result;
      // get the underlying callable's result, if the Callable was able to create it
      try {
        result = callResult.get();
        if(result.getStatus().equals(WorkerResult.WorkerResultStatus.FAILED)){
          logger.error("Worker failed: ", result);
        } else {
          logger.info("Worker " + result.getStatus().name() + ": " + result.toString());
        }
      } catch (ExecutionException e) {
        logger.error("Error getting worker status: " + e.getCause());
      }
    }
  }
}
