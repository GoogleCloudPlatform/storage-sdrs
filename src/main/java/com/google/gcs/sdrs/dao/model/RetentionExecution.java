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
 *
 */

package com.google.gcs.sdrs.dao.model;

import com.google.gcs.sdrs.dao.converter.DataStorageTypeConverter;
import com.google.gcs.sdrs.enums.DataStorageType;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** Note - coding to JPA specification, not Hibernate specific annotations */
@Entity
@Table(name = "retention_execution")
public class RetentionExecution {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "executor_id")
  private String executorId;

  @Convert(converter = DataStorageTypeConverter.class)
  @Column(name = "data_storage_type")
  private DataStorageType dataStorageType;

  @Column(name = "retention_job_id")
  private Integer retentionJobId;

  @Column(name = "created_at")
  private Timestamp createdAt;

  public RetentionExecution() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getExecutorId() {
    return executorId;
  }

  public void setExecutorId(String executorId) {
    this.executorId = executorId;
  }

  public DataStorageType getDataStorageType() {
    return dataStorageType;
  }

  public void setDataStorageType(DataStorageType dataStorageType) {
    this.dataStorageType = dataStorageType;
  }

  public Integer getRetentionJobId() {
    return retentionJobId;
  }

  public void setRetentionJobId(Integer retentionJobId) {
    this.retentionJobId = retentionJobId;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }
}
