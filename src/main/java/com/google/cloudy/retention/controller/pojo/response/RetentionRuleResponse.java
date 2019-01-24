package com.google.cloudy.retention.controller.pojo.request;

import com.google.cloudy.retention.controller.pojo.response.BaseHttpResponse;
import com.google.cloudy.retention.enums.RetentionRuleTypes;

public class RetentionRuleUpdateResponse extends BaseHttpResponse {
  private Integer ruleId;
  private String datasetName;
  private Integer retentionPeriod;
  private String dataStorageName;
  private String projectId;
  private RetentionRuleTypes type;

  public Integer getRuleId() {
    return ruleId;
  }

  public void setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
  }

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

  public RetentionRuleTypes getType() {
    return type;
  }

  public void setType(RetentionRuleTypes type) {
    this.type = type;
  }
}
