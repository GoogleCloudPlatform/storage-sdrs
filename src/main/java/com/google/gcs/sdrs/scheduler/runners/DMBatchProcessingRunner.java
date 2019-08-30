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

//TODO // this run the actual processing of DM   (2)
public class DMBatchProcessingRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(DMBatchProcessingRunner.class);
  private DMQueueTableDao queueDao = SingletonDao.getDMQueueDao();
  private LockDao lockDao = SingletonDao.getLockDao();
  private UUID token;
  private Session currentSession  =  lockDao.getLockSession();

  @Override
  public void run() {
    logger.info("Making request to execution service endpoint.");
    try {
      token = generateType5UUID("testing", "sdrsRunner");

      if(lockDao.obtainLock(token.toString(), currentSession)){
        List<DMQueueTableEntry> allAvailableQueueForProcessingSTSJobs = queueDao.getAllAvailableQueueForProcessingSTSJobs();

        Map<String, List<DMQueueTableEntry>> groupedRawResult = allAvailableQueueForProcessingSTSJobs.stream().collect(Collectors.groupingBy(DMQueueTableEntry::getDataStorageRoot));

        //Map<String, List<DMQueueTableEntry>> groupedOneThousandLessResult = ;

        Iterator it = groupedRawResult.entrySet().iterator();
        while (it.hasNext()) {
          Map.Entry pair = (Map.Entry)it.next();
          String currentBucket = pair.getKey().toString();
          List<DMQueueTableEntry> entriesPerBucket = (List<DMQueueTableEntry>)pair.getValue();
          if(entriesPerBucket.size() <=1000){


            //called sts job
            //change status of this list to STSExecuting
          }else{
            // bucket size is > 1000

          }

        }



        //update list<DMQueueTableEntry> STATUS to PROCESSING


        lockDao.releaseLock(token.toString(), currentSession);

      }

    }catch(UnsupportedEncodingException ue){
      //throw error
    }

    //else do nothing since we dont obtain the lock (it means some other system is running the DMBatch Processing Runner)




  }
     //this class is a scheduller, i can wrap the logic in sperate class but do that after I finish and verify things.
     //KISS PRINCIPLE!!!
     //we can wrap this logic into its own class...
     //Perfrom Lock ( call Lock so that STSAPI call only can be executed one at a time

        // get candidates from the queue
        // filtered = filter results for every bucket keep maximum 1000
        //
        // for every bucket filtered, call for execute STS Api wrapper
        // if successful change those 1000 prefix in the bucket to have,
        //           processing sts status pending run do it one at a time
        //
        //




    // Unlock


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



