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

package com.google.gcs.sdrs.controller.pojo;

import java.io.Serializable;

public class PooledJobCreateRequest implements Serializable {

  private static final long serialVersionUID = 4511882014702712891L;

  private String name;
  private String projectId;
  private String schedule;
  private String type;
  private String sourceBucket;
  private String sourceProject;
  private String targetBucket; // Optional
  private String targetProject; // Optional

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getSchedule() {
    return schedule;
  }

  public void setSchedule(String schedule) {
    this.schedule = schedule;
  }

  public String getSourceBucket() {
    return sourceBucket;
  }

  public void setSourceBucket(String sourceBucket) {
    this.sourceBucket = sourceBucket;
  }

  public String getSourceProject() {
    return sourceProject;
  }

  public void setSourceProject(String sourceProject) {
    this.sourceProject = sourceProject;
  }

  public String getTargetBucket() {
    return targetBucket;
  }

  public void setTargetBucket(String targetBucket) {
    this.targetBucket = targetBucket;
  }

  public String getTargetProject() {
    return targetProject;
  }

  public void setTargetProject(String targetProject) {
    this.targetProject = targetProject;
  }

public String getType() {
return type;}

public void setType(String type) {
this.type = type;}
}
