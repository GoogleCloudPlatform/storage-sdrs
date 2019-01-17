/*
 * Copyright 2018 Google LLC. All rights reserved.
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

package com.google.cloudy.retention.scheduler;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.cloudy.retention.pojo.RetentionJobLog;
import com.google.cloudy.retention.scheduler.command.Command;
import com.google.cloudy.retention.service.dataaccess.SqlDbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public class JobScheduler {

  private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);

  //TODO Consider use of scheduler like quartz to run the jobs that aren't invoked immediately
  private static JobScheduler instance;
  private static AtomicInteger workerCounter = new AtomicInteger(0);
  public static final int	THREAD_SLEEP_SECS=30;
  private static int SLEEP_MINS;
  private String projectID;
  private GoogleCredential googleCredential;
  private ExecutorService executorService;
  private CompletionService<RetentionJobLog> threadCompletionService;
  private SqlDbManager sqlDbManager; // reference only

  public Map<String,Command> commandMap = new HashMap<String,Command>();

  //TODO  wire up Spring dependency injection before this goes to prod
  public static JobScheduler getInstance(int threadPoolSize, int sleepMins, SqlDbManager sqlDbManager)  {
    if (instance == null) {
      synchronized (JobScheduler.class) {
        if(instance == null){
          try {
            instance = new JobScheduler(threadPoolSize, sleepMins, sqlDbManager);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(2);
          }
        }
      }
    }
    return instance;
  }

  private JobScheduler (int threadPoolSize, int sleepMins, SqlDbManager sqlDbManager) {
    //poor man's Singleton
    SLEEP_MINS = sleepMins;
    this.sqlDbManager = sqlDbManager;
    executorService = Executors.newFixedThreadPool(threadPoolSize);
    threadCompletionService = new ExecutorCompletionService<RetentionJobLog>(executorService);
    init();
  }

  private void init(){
    new Thread(new BackgroundMonitoringThread()).start();
    logger.info("Job Scheduler open for business ...");
  }

  /**
   * Open for business (listening for incoming requests)
   *
   * @param o
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  public void handleRequest(String command) throws ClassNotFoundException, SQLException {
    if(command!=null) {
      executeCommandPattern(command);
    }
  }

  public void executeCommandPattern(String command) throws ClassNotFoundException, SQLException {
    logger.debug("command coming in " + command);
    for(String key: commandMap.keySet()) {
      if(command.contains(key)) {
        commandMap.get(key).execute(command);
      }
    }
  }

  public void submitJob(Callable<RetentionJobLog> job) {
    threadCompletionService.submit(job);
    workerCounter.incrementAndGet();
  }


  public void submitTaskForExecution(Callable callable, Serializable serializable) throws ClassNotFoundException, SQLException {
    threadCompletionService.submit(callable);
    workerCounter.incrementAndGet();
    sqlDbManager.saveOrUpdate(serializable);
  }


  private void shutDownExecutorService() {

    executorService.shutdown();// waits nicely for executing tasks to finish, and won't spawn new ones
    try {
      if (!executorService.awaitTermination(SLEEP_MINS, TimeUnit.MINUTES)) {//timeout after 1 hour max
        executorService.shutdownNow();
      }
    } catch (InterruptedException e) {
      executorService.shutdownNow();
    }
  }


  /**
   *
   * listening request/monitors the
   *
   * @param threadCompletionService
   * @throws InterruptedException
   * @throws ClassNotFoundException
   * @throws SQLException
   */
  private void examineFutureResults() throws InterruptedException, ClassNotFoundException, SQLException {
    logger.debug(String.format("Starting examining futures, there are X workers in flight %d",
            workerCounter.get()));

    while(workerCounter.get()> 0) {
      // block until a callable completes
      Future<RetentionJobLog> callResult = threadCompletionService.take();
      workerCounter.decrementAndGet();
      RetentionJobLog result;
      // get the underlying callable's result, if the Callable was able to create it
      try {
        result = callResult.get();
        logger.debug(
            String.format("Callable with retention process id %s returned", result.getId()));

        sqlDbManager.saveOrUpdate(result);
      } catch (ExecutionException e) {
        //Throwable cause = e.getCause();
        logger.error(String.format("Error getting future result %s", e.getCause()));
      }
    }
    logger.debug(String.format("Finishing examining futures, there are X workers in flight %d",
        workerCounter.get()));
  }

  private class BackgroundMonitoringThread implements Runnable  {

    public void run () {
      while (true) {
        if(workerCounter.get()>0) {
          try {
            examineFutureResults();
          } catch (ClassNotFoundException | InterruptedException | SQLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        try {
          // polls every X seconds to see if there are workThreads waiting to complete
          TimeUnit.SECONDS.sleep(THREAD_SLEEP_SECS);
        } catch (InterruptedException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  public String getProjectID() {
    return projectID;
  }

  public void setProjectID(String projectID) {
    this.projectID = projectID;
  }

  public GoogleCredential getGoogleCredential() {
    return googleCredential;
  }

  public void setGoogleCredential(GoogleCredential googleCredential) {
    this.googleCredential = googleCredential;
  }
}
