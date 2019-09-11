/*
 * Copyright 2019 Google LLC. All rights reserved.
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
 */
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
@Table(name = "dm_queue")
public class DmRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "created_at", updatable = false)
  @CreationTimestamp
  private Timestamp createdAt;

  @Column(name = "updated_at", updatable = false)
  @UpdateTimestamp
  private Timestamp updatedAt;

  @Column(name = "data_storage_name")
  private String dataStorageName;

  @Column(name = "status")
  private String status;

  @Column(name = "priority")
  private int priority;

  @Column(name = "data_storage_root")
  private String dataStorageRoot;

  @Column(name = "retention_job_id")
  private int retentionJobId;

  @Column(name = "number_of_retry")
  private int numberOfRetry;

  @Column(name = "project_id")
  private String projectId;

  public Integer getId() {
    return id;
  }


  public Timestamp getCreatedAt() {
    return createdAt;
  }


  public Timestamp getUpdatedAt() {
    return updatedAt;
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

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }
}
