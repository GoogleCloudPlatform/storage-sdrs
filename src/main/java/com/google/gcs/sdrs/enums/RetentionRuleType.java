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

package com.google.gcs.sdrs.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * Supported types for Retention Rules
 *
 * <p>JsonProperty values indicate the supported JSON input string.
 *
 * <p>Enum string values indicate how this value is serialized to the database.
 */
public enum RetentionRuleType implements Serializable {
  @JsonProperty("GLOBAL")
  GLOBAL("global"),

  @JsonProperty("DATASET")
  DATASET("dataset");

  private final String value;

  RetentionRuleType(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return this.value;
  }
}
