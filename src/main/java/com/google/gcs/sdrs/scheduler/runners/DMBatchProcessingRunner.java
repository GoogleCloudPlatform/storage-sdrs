package com.google.gcs.sdrs.scheduler.runners;

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
  private UUID token;
  private Session currentLockSession =  lockDao.getLockSession();

  @Override
  public void run() {
    logger.info("Making request to execution service endpoint.");
    try {
      token = generateType5UUID("testing", "sdrsRunner");

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

        //lockDao.releaseLock(token.toString(), currentLockSession); no need here, we have finally

      }

    }catch(UnsupportedEncodingException ue){
      // put error on logstack
    }catch(Exception e){
      // put error on logstack
    }
    finally{
      // guarante execution
      lockDao.releaseLock(token.toString(), currentLockSession);
      currentLockSession.close();
    }

    //else do nothing since we dont obtain the lock (it means some other system is running the DMBatch Processing Runner)




  }

  private void changeStatusToSTSExecuting(List<DMQueueTableEntry> dMQueueThatIsInSTSProcessing){
    for (DMQueueTableEntry dmqueue : dMQueueThatIsInSTSProcessing){
      dmqueue.setStatus(DMQUEUE_STATUS_STS_EXECUTION);
      queueDao.save(dmqueue);
    }

  }
  /**
   * Type 5 UUID Generation
   *
   * @throws UnsupportedEncodingException
   */
  private static UUID generateType5UUID(String namespace, String name) throws UnsupportedEncodingException {
    String source = namespace + name;
    byte[] bytes = source.getBytes("UTF-8");
    UUID uuid = type5UUIDFromBytes(bytes);
    return uuid;
  }

  private static UUID type5UUIDFromBytes(byte[] name) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException nsae) {
      throw new InternalError("MD5 not supported", nsae);
    }
    byte[] bytes = md.digest(name);
    bytes[6] &= 0x0f; /* clear version        */
    bytes[6] |= 0x50; /* set to version 5     */
    bytes[8] &= 0x3f; /* clear variant        */
    bytes[8] |= 0x80; /* set to IETF variant  */
    return constructType5UUID(bytes);
  }

  private static UUID constructType5UUID(byte[] data) {
    long msb = 0;
    long lsb = 0;
    assert data.length == 16 : "data must be 16 bytes in length";

    for (int i = 0; i < 8; i++)
      msb = (msb << 8) | (data[i] & 0xff);

    for (int i = 8; i < 16; i++)
      lsb = (lsb << 8) | (data[i] & 0xff);
    return new UUID(msb, lsb);
  }
}



