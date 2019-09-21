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
package com.google.gcs.sdrs.scheduler.runners;

import com.google.gcs.sdrs.service.manager.JobManager;
import com.google.gcs.sdrs.service.worker.Worker;
import com.google.gcs.sdrs.service.worker.impl.DmBatchProcessingWorker;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Submits the job to the task queue managed by JobManager */
public class DmBatchProcessingRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DmBatchProcessingRunner.class);

  public DmBatchProcessingRunner() {
    super();
  }

  @Override
  public void run() {
    String uuid = UUID.randomUUID().toString();
    logger.info(String.format("Submitting DM batch processing job %s", uuid));
    Worker dmBatchProcessingWorker = new DmBatchProcessingWorker(uuid);
    JobManager.getInstance().submitJob(dmBatchProcessingWorker);
  }
}
