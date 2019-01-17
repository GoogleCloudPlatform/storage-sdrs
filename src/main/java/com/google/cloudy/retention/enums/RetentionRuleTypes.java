package com.google.cloudy.retention.enums;

import java.io.Serializable;

/**
 * Supported types for Retention Rules
 */
public enum RetentionRuleTypes implements Serializable {
  GLOBAL("GLOBAL"),
  DATASET("DATASET");

  private final String value;

  RetentionRuleTypes(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
