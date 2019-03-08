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

import com.google.gcs.sdrs.dao.converter.RetentionRuleTypeConverter;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import java.sql.Timestamp;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/** Note - coding to JPA specification, not Hibernate specific annotations */
@Entity
@Table(name = "retention_job")
public class RetentionJob {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "name")
  private String name;

  @Column(name = "retention_rule_id")
  private Integer retentionRuleId;

  @Column(name = "retention_rule_version")
  private Integer retentionRuleVersion;

  @Convert(converter = RetentionRuleTypeConverter.class)
  @Column(name = "retention_rule_type")
  private RetentionRuleType retentionRuleType;

  @Column(name = "retention_rule_data_storage_name")
  private String retentionRuleDataStorageName;

  @Column(name = "retention_rule_project_id")
  private String retentionRuleProjectId;

  @Column(name = "created_at", updatable = false)
  private Timestamp createdAt;

  @Column(name = "updated_at", updatable = false)
  private Timestamp updatedAt;

  @OneToMany(mappedBy = "retentionJobId")
  private List<RetentionJobValidation> jobValidations;

  public RetentionJob() {}

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Integer getRetentionRuleId() {
    return retentionRuleId;
  }

  public void setRetentionRuleId(Integer retentionRuleId) {
    this.retentionRuleId = retentionRuleId;
  }

  public RetentionRuleType getRetentionRuleType() {
    return retentionRuleType;
  }

  public void setRetentionRuleType(RetentionRuleType retentionRuleType) {
    this.retentionRuleType = retentionRuleType;
  }

  public String getRetentionRuleDataStorageName() {
    return retentionRuleDataStorageName;
  }

  public void setRetentionRuleDataStorageName(String retentionRuleDataStorageName) {
    this.retentionRuleDataStorageName = retentionRuleDataStorageName;
  }

  public String getRetentionRuleProjectId() {
    return retentionRuleProjectId;
  }

  public void setRetentionRuleProjectId(String retentionRuleProjectId) {
    this.retentionRuleProjectId = retentionRuleProjectId;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Integer getRetentionRuleVersion() {
    return retentionRuleVersion;
  }

  public void setRetentionRuleVersion(Integer retentionRuleVersion) {
    this.retentionRuleVersion = retentionRuleVersion;
  }
}
