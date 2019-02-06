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

package com.google.gcs.sdrs.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;

/**
 * Supported type for events sent to the execution endpoint
 *
 * <p>JsonProperty values indicate the supported JSON input string.
 */
public enum ExecutionEventType {
  @JsonProperty(ValidationConstants.POLICY_JSON_VALUE)
  POLICY(ValidationConstants.POLICY_JSON_VALUE),

  @JsonProperty(ValidationConstants.USER_JSON_VALUE)
  USER_COMMANDED(ValidationConstants.USER_JSON_VALUE);

  private final String jsonValue;

  ExecutionEventType(String jsonValue) {
    this.jsonValue = jsonValue;
  }

  /** This will return the JSON representation */
  @Override
  public String toString() {
    return jsonValue;
  }
}
