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

package com.google.gcs.sdrs.controller.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gcs.sdrs.common.RetentionRuleType;
import java.io.Serializable;

/** POJO Tracking JSON input fields/types for creating a retention rule */
public class RetentionRuleCreateRequest implements Serializable {

  private static final long serialVersionUID = -6338592900101329098L;
  private String datasetName;
  private Integer retentionPeriod;
  private String retentionPeriodUnit;
  private String dataStorageName;
  private String projectId;

  @JsonProperty("type")
  private RetentionRuleType retentionRuleType;

  public String getDatasetName() {
    return datasetName;
  }

  public void setDatasetName(String datasetName) {
    this.datasetName = datasetName;
  }

  public Integer getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Integer retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
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

  public RetentionRuleType getRetentionRuleType() {
    return retentionRuleType;
  }

  public void setRetentionRuleType(RetentionRuleType retentionRuleType) {
    this.retentionRuleType = retentionRuleType;
  }

  public String getRetentionPeriodUnit() {
    return retentionPeriodUnit;
  }

  public void setRetentionPeriodUnit(String retentionPeriodUnit) {
    this.retentionPeriodUnit = retentionPeriodUnit;
  }
}
