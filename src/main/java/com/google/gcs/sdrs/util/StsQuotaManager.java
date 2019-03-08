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

package com.google.gcs.sdrs.util;

import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.gcs.sdrs.SdrsApplication;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages STS quota limits. */
public class StsQuotaManager {

  public static final int DEFAULT_STS_THROTTLE_LIMIT = 10;
  public static final int DEFAULT_STST_THROTTLE_INTERVAL = 120000;
  private static final int THREAD_POOL_SIZE = 20;
  private static final int SLEEP_MINUTES = 5;
  private static StsQuotaManager instance;
  private ConcurrentLinkedQueue<TransferJobCallableWrapper> stsJobList;
  private ScheduledExecutorService scheduledExecutor;
  private ExecutorService executorService;
  private ConcurrentHashMap<String, Future<TransferJob>> stsJobFeatureMap;

  private ConcurrentHashMap<String, CountDownLatch> countDownLatchMap;

  private static final Logger logger = LoggerFactory.getLogger(StsQuotaManager.class);

  private StsQuotaManager() {
    stsJobList = new ConcurrentLinkedQueue<>();
    scheduledExecutor = Executors.newScheduledThreadPool(1);
    executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    int throttleInterval =
        Integer.valueOf(
            SdrsApplication.getAppConfigProperty(
                "sts.throttleInterval", String.valueOf(DEFAULT_STST_THROTTLE_INTERVAL)));

    scheduledExecutor.scheduleAtFixedRate(
        new StsJobScheduler(), 0, throttleInterval, TimeUnit.MILLISECONDS);
    stsJobFeatureMap = new ConcurrentHashMap<>();
    countDownLatchMap = new ConcurrentHashMap<>();
  }

  public static synchronized StsQuotaManager getInstance() {
    if (instance == null) {
      logger.info("StsQuotaManager not created. Creating...");
      instance = new StsQuotaManager();
    }

    return instance;
  }

  public String submitStsJob(Callable<TransferJob> jobCallable, CountDownLatch countDownLatch) {
    String uuid = String.valueOf(Instant.now().toEpochMilli()) + ";" + UUID.randomUUID().toString();
    stsJobList.add(new TransferJobCallableWrapper(uuid, jobCallable));
    countDownLatchMap.put(uuid, countDownLatch);
    return uuid;
  }

  public Future<TransferJob> getStsJobFuture(String uuid) {
    return stsJobFeatureMap.get(uuid);
  }

  public void removeStsJobFuture(String uuid) {
    stsJobFeatureMap.remove(uuid);
    countDownLatchMap.remove(uuid);
  }

  public void shutdown() {
    logger.info("Shutting down StsQuotaManager.");
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
    scheduledExecutor.shutdownNow();
    try {
      if (!scheduledExecutor.awaitTermination(SLEEP_MINUTES, TimeUnit.MINUTES)) {
        scheduledExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduledExecutor.shutdownNow();
    }

    // Ensure the job manager instance is destroyed
    instance = null;

    logger.info("StsQuotaManager shut down.");
  }

  private class StsJobScheduler implements Runnable {

    @Override
    public void run() {

      int throttleLimit =
          Integer.valueOf(
              SdrsApplication.getAppConfigProperty(
                  "sts.throttleLimit", String.valueOf(DEFAULT_STS_THROTTLE_LIMIT)));

      List<TransferJobCallableWrapper> wrappers = new ArrayList<>();

      for (int i = 0; i < throttleLimit; i++) {
        TransferJobCallableWrapper wrapper = stsJobList.poll();
        if (wrapper == null) {
          break;
        } else {
          wrappers.add(wrapper);
        }
      }

      logger.debug(
          String.format(
              "StsJobScheduler running %s. %d out of %d being processed. throttleLimit=%d",
              Instant.now().toString(), wrappers.size(), stsJobList.size(), throttleLimit));

      for (TransferJobCallableWrapper wrapper : wrappers) {
        if (wrapper.callable != null) {
          Future<TransferJob> future = executorService.submit(wrapper.callable);
          stsJobFeatureMap.put(wrapper.uuid, future);
          new Thread(
                  () -> {
                    String uuid = wrapper.uuid;
                    CountDownLatch countDownLatch = countDownLatchMap.get(uuid);
                    if (countDownLatch != null) {
                      countDownLatch.countDown();
                      logger.debug(
                          String.format("countDown=%s; uuid=%s", countDownLatch.toString(), uuid));
                    } else {
                      logger.warn(String.format("countDown for %s countDown is null", uuid));
                    }
                  })
              .start();
        } else {
          logger.warn(String.format("callable for %s is null", wrapper.uuid));
        }
      }
    }
  }

  private class TransferJobCallableWrapper {
    private String uuid;
    private Callable<TransferJob> callable;

    public TransferJobCallableWrapper(String uuid, Callable<TransferJob> callable) {
      this.uuid = uuid;
      this.callable = callable;
    }
  }
}
