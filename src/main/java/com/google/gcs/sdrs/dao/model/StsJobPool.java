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

/** Note - coding to JPA specification, not Hibernate specific annotations */
@Entity
@Table(name = "sts_job_pool")
public class StsJobPool {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(name = "id", updatable = false, nullable = false)
  private Integer id;

  @Column(name = "name")
  private int name;

  @Column(name = "project")
  private String project;

  @Column(name = "source_bucket")
  private String sourceBucket;

  @Column(name = "source_project")
  private String sourceProject;

  @Column(name = "schedule")
  private String schedule;

  @Column(name = "updated_at")
  private Timestamp updatedAt;

  @Column(name = "status")
  private String status;

  public StsJobPool() {}

  public Integer getId() {
    return this.id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getName() {
    return this.name;
  }

  public void setName(int name) {
    this.name = name;
  }

  public String getProject() {
    return this.project;
  }

  public void setProject(String project) {
    this.project = project;
  }

  public String getSourceBucket() {
    return this.sourceBucket;
  }

  public void setSourceBucket(String sourceBucket) {
    this.sourceBucket = sourceBucket;
  }

  public String getSourceProject() {
    return this.sourceProject;
  }

  public void setSourceProject(String sourceProject) {
    this.sourceProject = sourceProject;
  }

  public String getSchedule() {
    return this.schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  public Timestamp getUpdatedAt() {
    return this.updatedAt;
  }

  public void setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getStatus() {
    return this.status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
