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

import com.google.gcs.sdrs.dao.DmQueueDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Remove successfully processed requests from the queue. */
public class DmQueueCleanupRunner implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(DmQueueCleanupRunner.class);

  @Override
  public void run() {
    DmQueueDao dmQueueDao = SingletonDao.getDmQueueDao();
    int result = dmQueueDao.deleteSuccessfulDmRequests();
    logger.info(String.format("Removed %d successfully processed DM requests.", result));
  }
}
