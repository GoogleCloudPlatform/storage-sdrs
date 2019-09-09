package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.dao.DMQueueDao;
import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.DMQueue;
import com.google.gcs.sdrs.dao.model.DistributedLock;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmBatchProcessingWorker extends BaseWorker {
  private static final Logger logger = LoggerFactory.getLogger(DmBatchProcessingWorker.class);
  private DMQueueDao queueDao = SingletonDao.getDMQueueDao();
  private LockDao lockDao = SingletonDao.getLockDao();

  public static final int DEFAULT_DM_LOCK_TIMEOUT = 60000; // one minute by default
  public static final int DM_LOCK_TIMEOUT =
      Integer.valueOf(
          SdrsApplication.getAppConfigProperty(
              "lock.dm.timeout", String.valueOf(DEFAULT_DM_LOCK_TIMEOUT)));
  public static final String DEFAULT_DM_LOCK_ID = "dm-batch";
  public static final String DM_LOCK_ID =
      SdrsApplication.getAppConfigProperty("lock.dm.id", DEFAULT_DM_LOCK_ID);

  public DmBatchProcessingWorker(String correlationId) {
    super(correlationId);
  }

  @Override
  public void doWork() {
    Session currentLockSession = lockDao.getLockSession();
    try {

      logger.info(String.format("acquiring lock at %s", Instant.now(Clock.systemUTC()).toString()));
      DistributedLock distributedLock =
          lockDao.obtainLock(currentLockSession, DM_LOCK_TIMEOUT, DM_LOCK_ID);
      if (distributedLock != null) {
        logger.info(
            String.format(
                "acquired lock %s at %s",
                distributedLock.getLockToken(), Instant.now(Clock.systemUTC()).toString()));
        // getting sorted list of all available queue for processing
        List<DMQueue> allAvailableQueueForProcessingSTSJobs =
            queueDao.getAllAvailableQueueForProcessingSTSJobs();
        // stream maintain order processing, so we still have order based on the sorted list above.
        Map<String, List<DMQueue>> groupedRawResult =
            allAvailableQueueForProcessingSTSJobs.stream()
                .collect(Collectors.groupingBy(DMQueue::getDataStorageRoot));

        Thread.sleep(3000);
        // iterate thru every bucket
        /*        Iterator it = groupedRawResult.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          String currentBucket = pair.getKey().toString();
          List<DMQueue> entriesPerBucket = (List<DMQueue>)pair.getValue();
          if(entriesPerBucket.size() <=1000){


            //called sts job
            //change status of this list to STSExecuting
            changeStatusToSTSExecuting(entriesPerBucket);

          }else{
            // bucket size is > 1000
            // change status of this listing, if we are doing nothing for greater than 1000 (to be executed next time this thread active)
            // then we dont update the status of this listing

            //called sts job for the first 1000
            //change status of the first 100 to STSExecuting.

            List<DMQueue> first1k = entriesPerBucket.subList(0,1000);
            //call sts job
            changeStatusToSTSExecuting(first1k);

            //put the rest logic here!

          }

        }*/
        // we need to release lock by executing releaseDock inside lockDao, since they need to
        // perform
        // clean up to lock table entry. if acquired lock fail, then this code will not be executed.
        // we also need to commit the transaction after releasing the lock.

        lockDao.releaseLock(currentLockSession, distributedLock);
        logger.info(
            String.format(
                "released lock %s at %s",
                distributedLock.getLockToken(), Instant.now(Clock.systemUTC()).toString()));
      } else {
        logger.info("Can not acquire lock.");
      }
      workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
    } catch (Exception e) {
      logger.error("Unknown error. ", e);
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    } finally {
      // clean up resource
      lockDao.closeLockSession(currentLockSession);
    }
  }
}
