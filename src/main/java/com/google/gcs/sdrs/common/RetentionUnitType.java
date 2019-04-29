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

package com.google.gcs.sdrs.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import java.io.Serializable;

/**
 * Supported types for Retention Unit
 *
 * <p>JsonProperty values indicate the supported JSON input string.
 *
 * <p>Enum string values indicate how this value is serialized to the database.
 */
public enum RetentionUnitType implements Serializable {
  @JsonProperty(ValidationConstants.RETENTION_PEROID_UNIT_DAY_JSON)
  DAY(
      ValidationConstants.RETENTION_PEROID_UNIT_DAY_JSON,
      DatabaseConstants.RETENTION_PEROID_UNIT_DAY),

  @JsonProperty(ValidationConstants.RETENTION_PEROID_UNIT_MONTH_JSON)
  MONTH(ValidationConstants.RETENTION_PEROID_UNIT_MONTH_JSON, DatabaseConstants.RETENTION_PEROID_UNIT_MONTH),

  @JsonProperty(ValidationConstants.RETENTION_PEROID_UNIT_VERSION_JSON)
  VERSION(ValidationConstants.RETENTION_PEROID_UNIT_VERSION_JSON, DatabaseConstants.RETENTION_PEROID_UNIT_VERSION);

  private final String jsonValue;
  private final String databaseValue;

  RetentionUnitType(final String jsonValue, final String databaseValue) {
    this.jsonValue = jsonValue;
    this.databaseValue = databaseValue;
  }

  /** This will return the JSON representation */
  @Override
  public String toString() {
    return this.jsonValue;
  }

  /** This will return the database representation */
  public String toDatabaseRepresentation() {
    return this.databaseValue;
  }

  public static RetentionUnitType getType(String type) {
    try {
      return RetentionUnitType.valueOf(type.toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      return null;
    }
  }
}
