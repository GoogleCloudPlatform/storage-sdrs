package com.google.gcs.sdrs.dao.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "DistributedLockEntry")
public class LockEntry {
  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "lockIdVerificationToken", updatable = false, nullable = false)
  private String LockIdVerificationToken;

  @Column(name = "lockDuration",nullable = false)
  private int durationOfLockInSeconds;

  @Column(name = "created_at",nullable = false)
  private Timestamp lockCreationTime;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getLockIdVerification() {
    return LockIdVerificationToken;
  }

  public void setLockIdName(String LockIdVerificationToken) {
    this.LockIdVerificationToken = LockIdVerificationToken;
  }

  public int getDurationOfLockInSeconds() {
    return durationOfLockInSeconds;
  }

  public void setDurationOfLockInSeconds(int durationOfLockInSeconds) {
    this.durationOfLockInSeconds = durationOfLockInSeconds;
  }

  public Timestamp getLockCreationTime() {
    return lockCreationTime;
  }

}
