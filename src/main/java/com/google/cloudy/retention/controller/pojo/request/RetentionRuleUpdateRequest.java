package com.google.cloudy.retention.controller.pojo.request;

import java.io.Serializable;

public class RetentionRuleUpdateRequest implements Serializable {
  private Integer retentionPeriod;
  private Integer ruleId;

  public Integer getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Integer retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public void setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
  }
}
