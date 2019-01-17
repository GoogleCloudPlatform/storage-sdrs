package com.google.cloudy.retention.controller.pojo.response;

public class RetentionRuleCreateResponse extends BaseHttpResponse {

  private int ruleId;

  public int getRuleId() {
    return ruleId;
  }

  public void setRuleId(int ruleId) {
    this.ruleId = ruleId;
  }
}
