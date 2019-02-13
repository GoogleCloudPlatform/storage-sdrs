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

import com.google.gcs.sdrs.dao.converter.RetentionRuleTypeConverter;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** Note - coding to JPA specification, not Hibernate specific annotations */
@Entity
@Table(name = "retention_rule")
public class RetentionRule {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "dataset_name")
  private String datasetName;

  @Column(name = "retention_period_in_days")
  private Integer retentionPeriodInDays;

  @Column(name = "data_storage_name")
  private String dataStorageName;

  @Column(name = "project_id")
  private String projectId;

  @Convert(converter = RetentionRuleTypeConverter.class)
  @Column(name = "type")
  private RetentionRuleType type;

  @Column(name = "version")
  private Integer version;

  @Column(name = "is_active")
  private Boolean isActive;

  @Column(name = "created_at")
  private Timestamp createdAt;

  @Column(name = "updated_at")
  private Timestamp updatedAt;

  @Column(name = "user")
  private String user;

  public RetentionRule() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public Integer getRetentionPeriodInDays() {
    return retentionPeriodInDays;
  }

  public void setRetentionPeriodInDays(Integer retentionPeriodInDays) {
    this.retentionPeriodInDays = retentionPeriodInDays;
  }

  public String getDataStorageName() {
    return dataStorageName;
  }

  public void setDataStorageName(String dataStorageName) {
    this.dataStorageName = dataStorageName;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public RetentionRuleType getType() {
    return type;
  }

  public void setType(RetentionRuleType type) {
    this.type = type;
  }

  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  public Boolean getIsActive() {
    return isActive;
  }

  public void setIsActive(Boolean isActive) {
    this.isActive = isActive;
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

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }
}
