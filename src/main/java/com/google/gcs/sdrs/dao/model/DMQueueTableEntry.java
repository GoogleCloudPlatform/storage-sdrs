package com.google.gcs.sdrs.dao.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


@Entity
@Table(name = "DMQueueTable")
public class DMQueueTableEntry {


  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "created_at",nullable = false)
  @CreationTimestamp
  private Timestamp createdAt;

  @Column(name = "updated_at",nullable = false)
  @UpdateTimestamp
  private Timestamp updatedAt;

  @Column(name = "data_storage_name",nullable = false)
  private String dataStorageName;

  @Column(name = "status")
  private String status;

  @Column(name = "priority" ,nullable = false)
  private int priority;

  @Column(name = "data_storage_root",nullable = false)
  private String dataStorageRoot;

  @Column(name = "retention_job_id")
  private int retentionJobId;

  @Column(name = "number_of_retry", nullable =false)
  private int numberOfRetry;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getDataStorageName() {
    return dataStorageName;
  }

  public void setDataStorageName(String dataStorageName) {
    this.dataStorageName = dataStorageName;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getPriority() {
    return priority;
  }

  public void setPriority(int priority) {
    this.priority = priority;
  }

  public String getDataStorageRoot() {
    return dataStorageRoot;
  }

  public void setDataStorageRoot(String dataStorageRoot) {
    this.dataStorageRoot = dataStorageRoot;
  }

  public int getRetentionJobId() {
    return retentionJobId;
  }

  public void setRetentionJobId(int retentionJobId) {
    this.retentionJobId = retentionJobId;
  }

  public int getNumberOfRetry() {
    return numberOfRetry;
  }

  public void setNumberOfRetry(int numberOfRetry) {
    this.numberOfRetry = numberOfRetry;
  }
}
