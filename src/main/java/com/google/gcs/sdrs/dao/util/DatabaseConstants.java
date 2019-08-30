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

package com.google.gcs.sdrs.dao.util;

public class DatabaseConstants {
  public static final String STORAGE_TYPE_GCS = "gcs";
  public static final String STORAGE_TYPE_BIG_QUERY = "bq";

  public static final String POLICY_TYPE_GLOBAL = "global";
  public static final String POLICY_TYPE_DATASET = "dataset";
  public static final String POLICY_TYPE_USER = "user";
  public static final String POLICY_TYPE_DEFAULT = "default";
  public static final String STS_JOB_STATUS_SUCCESS = "success";
  public static final String STS_JOB_STATUS_PENDING = "pending";
  public static final String STS_JOB_STATUS_ERROR = "error";
  public static final String RETENTION_PERIOD_UNIT_DAY = "day";
  public static final String RETENTION_PERIOD_UNIT_MONTH = "month";
  public static final String RETENTION_PERIOD_UNIT_VERSION = "version";

  public static final String DMQUEUE_STATUS_READY = "ready";
  public static final String DMQUEUE_STATUS_PROCESSING =  "processing";
  public static final String DMQUEUE_STATUS_READY_RETRY = "ready_retry" ;
  public static final String DMQUEUE_STATUS_STS_EXECUTION = "sts_execution";
  public static final String DMQUEUE_STATUS_FAIL= "fail";


}
