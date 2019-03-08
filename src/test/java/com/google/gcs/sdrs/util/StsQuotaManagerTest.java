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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import org.junit.Ignore;
import org.junit.Test;

public class StsQuotaManagerTest {

  @Ignore
  @Test
  public void testQuotaManager() throws InterruptedException {
    for (int i = 0; i < 70; i++) {
      Runnable runnable = new MockRunnable("transferJob-" + i);
      Thread thread = new Thread(runnable);
      thread.start();
      Thread.sleep(50);
    }

    Thread.sleep(600000);
  }

  private class MockCreateStsJob implements Callable<TransferJob> {
    private String name;
    private CountDownLatch countDownLatch;

    public MockCreateStsJob(String name, CountDownLatch countDownLatch) {
      this.name = name;
      this.countDownLatch = countDownLatch;
    }

    @Override
    public TransferJob call() throws Exception {
      TransferJob transferJob = new TransferJob();
      transferJob.setName(name);
      return transferJob;
    }
  }

  private class MockRunnable implements Runnable {
    private String name;

    public MockRunnable(String name) {
      this.name = name;
    }

    @Override
    public void run() {
      CountDownLatch countDownLatch = new CountDownLatch(1);
      MockCreateStsJob mockCreateStsJob = new MockCreateStsJob(name, countDownLatch);
      String uuid = StsQuotaManager.getInstance().submitStsJob(mockCreateStsJob, countDownLatch);
      System.out.println(
          String.format("jobName=%s; uuid=%s; latch=%s", name, uuid, countDownLatch.toString()));

      try {

        countDownLatch.await();
        System.out.println(
            String.format("jobName=%s; uuid=%s; latch=%s", name, uuid, countDownLatch.toString()));
        TransferJob transferJob = StsQuotaManager.getInstance().getStsJobFuture(uuid).get();
        System.out.println(String.format("transfer job %s is scheduled", transferJob.getName()));
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }
    }
  }
}
