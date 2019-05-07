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
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Callable BaseWorker class */
public abstract class BaseWorker implements Worker {

  public WorkerResult workerResult;
  private static final Logger logger = LoggerFactory.getLogger(BaseWorker.class);

  /** BaseWorker constructor that instantiates the internal WorkerResult object */
  protected BaseWorker(String uuid) {
    workerResult = new WorkerResult();
    workerResult.setType(this.getClass().getName());
    if (uuid == null) {
      workerResult.setId(UUID.randomUUID().toString());
    } else {
      workerResult.setId(uuid);
    }

    logger.debug("Worker created: " + this.workerResult.toString());
  }

  /**
   * The call method required to make this class Callable
   *
   * @return A basic populated WorkerResult object
   */
  @Override
  public WorkerResult call() {
    workerResult.setStartTime(DateTime.now(DateTimeZone.UTC));
    String currentName = Thread.currentThread().getName();
    Thread.currentThread().setName(currentName + ";" + workerResult.getId());
    logger.info("Worker processing begins: " + this.workerResult.toString());

    doWork();

    workerResult.setEndTime(DateTime.now(DateTimeZone.UTC));
    logger.info("Worker processing ends: " + this.workerResult.toString());

    Thread.currentThread().setName(currentName);
    return workerResult;
  }

  public abstract void doWork();

  public WorkerResult getWorkerResult() {
    return workerResult;
  }

  public String getUuid() {
    return workerResult.getId();
  }

  public static String getCorrelationId() {
    String threadName = Thread.currentThread().getName();
    if (threadName != null && threadName.lastIndexOf(";") > 0) {
      return threadName.substring(threadName.lastIndexOf(";"), threadName.length());
    }
    return null;
  }
}
