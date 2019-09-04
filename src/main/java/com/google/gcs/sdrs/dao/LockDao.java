package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.LockEntry;

import org.hibernate.Session;

public interface LockDao  extends Dao<LockEntry, Integer>{

  public Session getLockSession();

  public boolean obtainLock(String verificationKey, Session session);

  public boolean releaseLock(String verificationKey, Session session);

}
