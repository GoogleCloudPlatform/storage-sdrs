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

package com.google.gcs.sdrs.dao.converter;

import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import java.io.Serializable;

/**
 * Supported types for Data Storage
 *
 * <p>Enum string values indicate how it is serialized to the database.
 */
public enum DataStorageType implements Serializable {
  GOOGLE_CLOUD_STORAGE(DatabaseConstants.STORAGE_TYPE_GCS),
  BIG_QUERY(DatabaseConstants.STORAGE_TYPE_BIG_QUERY);

  private final String databaseValue;

  DataStorageType(final String databaseValue) {
    this.databaseValue = databaseValue;
  }

  public String toDatabaseRepresentation() {
    return databaseValue;
  }
}
