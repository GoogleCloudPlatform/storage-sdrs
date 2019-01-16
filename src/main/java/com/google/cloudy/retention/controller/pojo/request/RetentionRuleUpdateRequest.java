package com.google.cloudy.retention.controller.pojo.request;

import java.io.Serializable;

public class RetentionRuleUpdateRequest implements Serializable {
  private Integer retentionPeriod;

  public Integer getRetentionPeriod() {
    return retentionPeriod;
  }

  public void setRetentionPeriod(Integer retentionPeriod) {
    this.retentionPeriod = retentionPeriod;
  }
}
