package com.google.cloudy.retention.filter;

/**
 * Enumerated custom properties included as part of the container context
 */
public enum ContainerContextProperties {
  CORRELATION_UUID("correlationUuid");

  private final String value;

  ContainerContextProperties(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
