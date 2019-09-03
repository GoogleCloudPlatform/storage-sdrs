package com.google.gcs.sdrs.scheduler.runners;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.google.gcs.sdrs.dao.DMQueueTableDao;
import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.DMQueueTableEntry;

import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_STS_EXECUTION;

//TODO // this run the actual processing of DM   (2)
public class DMBatchProcessingRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DMBatchProcessingRunner.class);
  private DMQueueTableDao queueDao = SingletonDao.getDMQueueDao();
  private LockDao lockDao = SingletonDao.getLockDao();
  private String token;
  private Session currentLockSession =  lockDao.getLockSession();

  public DMBatchProcessingRunner(){
    super();
  }
  @Override
  public void run() {
    logger.info("Making request to execution service endpoint.");
    try {
      token = getUniqueToken("sdrsRunner");

      if(lockDao.obtainLock(token.toString(), currentLockSession)){
        //getting sorted list of all available queue for processing
        List<DMQueueTableEntry> allAvailableQueueForProcessingSTSJobs = queueDao.getAllAvailableQueueForProcessingSTSJobs();
        //stream maintain order processing, so we still have order based on the sorted list above.
        Map<String, List<DMQueueTableEntry>> groupedRawResult = allAvailableQueueForProcessingSTSJobs.stream().collect(Collectors.groupingBy(DMQueueTableEntry::getDataStorageRoot));

        //iterate thru every bucket
        Iterator it = groupedRawResult.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          String currentBucket = pair.getKey().toString();
          List<DMQueueTableEntry> entriesPerBucket = (List<DMQueueTableEntry>)pair.getValue();
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

            List<DMQueueTableEntry> first1k = entriesPerBucket.subList(0,1000);
            //call sts job
            changeStatusToSTSExecuting(first1k);

          }

        }
        // we need to release lock by executing releaseDock inside lockDao, since they need to perform
        // clean up to lock table entry. if acquired lock fail, then this code will not be executed.
        // hence no
        lockDao.releaseLock(token.toString(), currentLockSession);

      }

    }catch(Exception e){
      // put error on logstack
      //else do nothing since we dont obtain the lock (it means some other system is running the DMBatch Processing Runner) lockDao.releaseLock(token.toString(), currentLockSession);


    }
    finally{
      // guarante execution even when the lock are not obtain, running the close session is necessary even when exception occured
      // since there are no achive-able goal of ke
      currentLockSession.close();
    }




  }

  private void changeStatusToSTSExecuting(List<DMQueueTableEntry> dMQueueThatIsInSTSProcessing){
    for (DMQueueTableEntry dmqueue : dMQueueThatIsInSTSProcessing){
      dmqueue.setStatus(DMQUEUE_STATUS_STS_EXECUTION);
      queueDao.save(dmqueue);
    }

  }

  private String getUniqueToken(String seed){
    String uniqueToken = seed + "-";
    try {
      InetAddress inetAddress = InetAddress.getLocalHost();               // can throw UnknownHostEception
      uniqueToken = uniqueToken + inetAddress.getAddress().toString()+"-";
      uniqueToken = uniqueToken + inetAddress.getHostName().toString()+"-";
      uniqueToken = uniqueToken + GregorianCalendar.getInstance().getTimeInMillis();
    } catch (UnknownHostException e) {
      UUID uuid = UUID.randomUUID();
      uniqueToken = uniqueToken + uuid.toString()+ "-";
      uniqueToken = uniqueToken + GregorianCalendar.getInstance().getTimeInMillis();
    }
    return uniqueToken;
  }

}



