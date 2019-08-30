package com.google.gcs.sdrs.dao.impl;

import java.io.Serializable;
import java.util.Calendar;
import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.model.LockEntry;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import static com.google.gcs.sdrs.dao.util.DatabaseConstants.DMQUEUE_STATUS_READY_RETRY;

import static org.hibernate.LockOptions.NO_WAIT;

public class LockDaoImpl extends GenericDao<LockEntry, Integer> implements LockDao {

  public LockDaoImpl() {
    super(LockEntry.class);
  }

  public Session getLockSession(){
    return openSession();
  }

  public boolean obtainLock(String verificationKey ,Session session){
    if(null == verificationKey  || "".equals(verificationKey)){
      return false;
    }
    boolean canIObtain = false;
    //Session session = openSession();

    //query if the table is empty or not, if it is empty then you insert new verification key
    //then set canIObtaion to true;
    session.buildLockRequest();

    CriteriaBuilder builder = session.getCriteriaBuilder();
    Transaction transaction = session.beginTransaction();

    CriteriaQuery<LockEntry> query = builder.createQuery(LockEntry.class);

    Root<LockEntry> root = query.from(LockEntry.class);
    Query<LockEntry> queryResults = session.createQuery(query);

    List<LockEntry> list = queryResults.getResultList();
    session.close();

    if(list.isEmpty()){
      return true;
    }else{
      if(list.size() >= 1){
        //Calendar creationTime = list.get(0).getLockCreationTime();
        list.get(0).getDurationOfLockInSeconds();
        //if(LocalDateTime.now())
        // check if the current time >= to time creation + MaxLengthTime,
        //    delete current one
        //    return true;
        // else
        //    return false;
      }else{
        // we do need to loop thru, // more like
        // if c
      }
    }

    return false;
  }

  public boolean releaseLock(String verificationKey ,Session session){
    if(null == verificationKey  || "".equals(verificationKey)){
      return false;
    }else{
      // call query make sure the verification key is the same as the one in db
      // if it is the same, then delete that entry.
      // then return true;
      try{

        CriteriaBuilder builder = session.getCriteriaBuilder();
        Transaction transaction = session.beginTransaction();

        CriteriaQuery<LockEntry> query = builder.createQuery(LockEntry.class);
        Root<LockEntry> root = query.from(LockEntry.class);

        Predicate verKey = (builder.equal(root.get("LockIdVerificationToken"), verificationKey));
        query.select(root).where(verKey);

         session.createQuery(query);


        //getSingleRecordWithCriteriaQuery(query, session);


      }catch(Exception e){

      }
      return true;

    }
    //return false;
  }

}
