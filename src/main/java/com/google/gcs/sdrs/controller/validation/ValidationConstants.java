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

package com.google.gcs.sdrs.controller.validation;

public class ValidationConstants {
  // 3 Years
  public static final Integer RETENTION_MAX_VALUE = 1095;
  public static final String STORAGE_PREFIX = "gs://";
  public static final String STORAGE_SEPARATOR = "/";
  public static final String GLOBAL_JSON_VALUE = "GLOBAL";
  public static final String DATASET_JSON_VALUE = "DATASET";
  public static final String POLICY_JSON_VALUE = "POLICY";
  public static final String USER_JSON_VALUE = "USER";
  public static final String DEFAULT_JSON_VALUE = "DEFAULT";
}
