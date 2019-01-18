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

package com.google.gcs.sdrs.worker;

import java.util.concurrent.Callable;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Callable BaseWorker class
 */
public abstract class BaseWorker implements Callable<WorkerLog> {

  public WorkerLog workerLog;
  private static final Logger logger = LoggerFactory.getLogger(BaseWorker.class);

  /**
   * BaseWorker constructor that instantiates the internal WorkerLog object
   */
  public BaseWorker(){
    workerLog = new WorkerLog();
    workerLog.setType(this.getClass().getName());
    logger.debug("Worker created: " + this.workerLog.toString());
  }

  /**
   * The call method required to make this class Callable
   * @return A basic populated WorkerLog object
   */
  @Override
  public WorkerLog call() {
    workerLog.setStartTime(DateTime.now(DateTimeZone.UTC));
    logger.info("Worker processing begins: " + this.workerLog.toString());
    workerLog.setStatus(WorkerLog.WorkerLogStatus.SUCCESS);

    workerLog.setEndTime(DateTime.now(DateTimeZone.UTC));
    logger.info("Worker processing ends: " + this.workerLog.toString());

    return workerLog;
  }

  public WorkerLog getWorkerLog() {
    return workerLog;
  }
}
