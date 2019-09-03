package com.google.gcs.sdrs.dao.impl;

import java.util.GregorianCalendar;
import java.util.List;
import java.sql.Timestamp;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.model.LockEntry;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import static java.util.Calendar.MILLISECOND;

public class LockDaoImpl extends GenericDao<LockEntry, Integer> implements LockDao {

  public LockDaoImpl() {
    super(LockEntry.class);
  }

  public Session getLockSession(){

    return openSession();
  }

  /**
   * Method to get lock for Distributed Lock Table. this method will not lock any other table. String verification must be present.
   * It will return false if verification is null.
   * this method are not responsible for opening and closing the session passed as parameter. Make sure to handle opening and closing the
   * session by the cod that is calling this method
   * to open and close lock, identical verificationKey must match.
   *
   * @param verificationKey
   * @param lockSession         session object for locking Distributed Lock Table.
   * @return                    true when you successfully obtain the lock, false when fail.
   */

  public boolean obtainLock(String verificationKey ,Session lockSession){
    if(null == verificationKey  || "".equals(verificationKey)){
      return false;
    }
    //query if the table is empty or not, if it is empty then you insert new verification key
    //then set canIObtaion to true;
    try {

      lockSession.buildLockRequest(new LockOptions(LockMode.WRITE)).setTimeOut(Session.LockRequest.PESSIMISTIC_NO_WAIT);

      CriteriaBuilder builder = lockSession.getCriteriaBuilder();
      Transaction transaction = lockSession.beginTransaction();

      CriteriaQuery<LockEntry> query = builder.createQuery(LockEntry.class);

      Root<LockEntry> root = query.from(LockEntry.class);
      Query<LockEntry> queryResults = lockSession.createQuery(query);

      List<LockEntry> list = queryResults.getResultList();
      if(list.isEmpty()){
        // insert new LockEntry to the DistributedLockEntry
        insertNewLockKey(lockSession, verificationKey);

        return true;
      }else{
        // we assume that the distributedLock to be always size 0 and 1.
        // there is entry point, in table, however we are able to obtain the lock.
        // This means that the server crash, and mysql decide to expire the lock that was still hanging leaving the entry here.
        // This also means that the server is having an old data that need

          Timestamp creationTime = list.get(0).getLockCreationTime();
          GregorianCalendar cTime =  new GregorianCalendar();
          cTime.setTimeInMillis(creationTime.getTime());

          GregorianCalendar threadTime = new GregorianCalendar();
          threadTime.setTimeInMillis(threadTime.getTimeInMillis());

          int expiredInMilis = list.get(0).getDurationOfLockInSeconds() * 1000;

          cTime.add(MILLISECOND, expiredInMilis);

          // check if the thread time is after th expiration date then we are safely delete the old thread then
          if( threadTime.after(cTime)) {
            delete(list.get(0));
            // insert new entry on LockEntry
            insertNewLockKey(lockSession, verificationKey);
            return true;
          }else{
            return false;
          }

      }
    } catch(PessimisticLockException ple) {

      // failure to acquire lock means hibernate will throw exception
      return false;

    } catch(Exception e) {
      // any other exception
      e.printStackTrace();
      return false;
    }
  }

  /**
   * This method required to have the same session that is being used for locking the table. We do not want to create new session since creating
   * new session will not have the right of the lock thus not able to insert into the table.
   * @param lockSession
   */
  private void insertNewLockKey(Session lockSession, String verificationKey){
    LockEntry currentEntry = new LockEntry();
    currentEntry.setLockIdName(verificationKey);
    currentEntry.setDurationOfLockInSeconds(600);
    lockSession.save(currentEntry);
  }

  public boolean releaseLock(String verificationKey ,Session lockSession){
    if(null == verificationKey  || "".equals(verificationKey)){
      return false;
    }else{
        // call query make sure the verification key is the same as the one in db
        // if it is the same, then delete that entry.
        // then return true;

        // find out if the lockidverification match if it is match then we delete that entry. then close the session we are done our work
        // if somehow the program crash, it will release the session per JDBC and the lock table will be unlocked. Entries will be roll back.


        CriteriaBuilder builder = lockSession.getCriteriaBuilder();


        CriteriaQuery<LockEntry> query = builder.createQuery(LockEntry.class);
        Root<LockEntry> root = query.from(LockEntry.class);
        Predicate verKey = (builder.equal(root.get("LockIdVerificationToken"), verificationKey));
        query.select(root).where(verKey);
        Query<LockEntry> queryResults = lockSession.createQuery(query);

        List<LockEntry> list = queryResults.getResultList();

        if(list.size() >= 1 ) {
          // only successfull finding got deleted back.
          lockSession.delete(list);
          // do not commit transaction since we are not the one that issuing the session.
          // Let the creator of the session be the one that responsible
          // to close it. All we have to do here is to return the status of the release
          return true;
        }

        // do not commit transaction since we are not sure whether lockSession are valid one,
        // or we are not the one that issuing the session. let the creator of the session passed here
        // be the one that responsible for closing the session.
        // Just return false, since we got zero result from the tokenid verification.
        return false;
    }

  }

}
