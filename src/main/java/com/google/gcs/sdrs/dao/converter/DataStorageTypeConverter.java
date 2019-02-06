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

import com.google.gcs.sdrs.constants.DatabaseConstants;
import com.google.gcs.sdrs.enums.DataStorageType;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/** Supports converting a DataStorageType between the database and application representations */
@Converter
public class DataStorageTypeConverter implements AttributeConverter<DataStorageType, String> {

  /** Convert DataStorageType to a String */
  @Override
  public String convertToDatabaseColumn(DataStorageType type) {
    return type.toDatabaseRepresentation();
  }

  /** Convert a database string representation to a DataStorageType */
  @Override
  public DataStorageType convertToEntityAttribute(String databaseRepresentation) {
    switch (databaseRepresentation) {
      case DatabaseConstants.GOOGLE_CLOUD_STORAGE_VALUE:
        return DataStorageType.GOOGLE_CLOUD_STORAGE;
      case DatabaseConstants.BIG_QUERY_VALUE:
        return DataStorageType.BIG_QUERY;
      default:
        throw new IllegalArgumentException(
            String.format(
                "%s is not representable as a %s", databaseRepresentation, DataStorageType.class));
    }
  }
}
