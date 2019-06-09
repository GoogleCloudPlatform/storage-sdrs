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

package com.google.gcs.sdrs.common;

import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import java.io.Serializable;

/**
 * Supported types for retention job statuses
 *
 * <p>Enum string values indicate how this is serialized to the database.
 */
public enum RetentionJobStatusType implements Serializable {
  SUCCESS(DatabaseConstants.STS_JOB_STATUS_SUCCESS),
  PENDING(DatabaseConstants.STS_JOB_STATUS_PENDING),
  ERROR(DatabaseConstants.STS_JOB_STATUS_ERROR);

  private final String databaseValue;

  RetentionJobStatusType(final String databaseValue) {
    this.databaseValue = databaseValue;
  }

  public String toDatabaseRepresentation() {
    return databaseValue;
  }
}
