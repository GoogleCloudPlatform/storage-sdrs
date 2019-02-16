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

package com.google.gcs.sdrs.util;

import com.google.gcs.sdrs.controller.validation.ValidationConstants;

/**
 * A class of helper methods for retention rules, job, and validations.
 */
public class RetentionUtil {
  /**
   * Extracts the bucket name from the data storage name string
   * @param dataStorageName the full data storage name path
   * @return the bucket name only
   */
  public static String getBucketName(String dataStorageName) {
    if (dataStorageName == null){
      return "";
    }

    String bucketName = dataStorageName.replaceFirst(ValidationConstants.STORAGE_PREFIX,"");

    int separatorIndex = bucketName.indexOf(ValidationConstants.STORAGE_SEPARATOR);

    if (separatorIndex != -1) {
      bucketName = bucketName.substring(0, separatorIndex);
    }

    return bucketName;
  }

  /**
   * Extracts the bucket name from the data storage name string and appends the suffix
   * @param dataStorageName the full data storage name path
   * @param suffix the string to append to the bucket name
   * @return the bucket name with the suffix appended
   */
  public static String getBucketName(String dataStorageName, String suffix) {
    return getBucketName(dataStorageName).concat(suffix);
  }

  /**
   * Extracts the dataset path from the data storage name string
   * @param dataStorageName the full data storage name path
   * @return the dataset path without the root bucket
   */
  public static String getDatasetPath(String dataStorageName) {
    if (dataStorageName == null){
      return "";
    }

    String bucketName = getBucketName(dataStorageName);
    String datasetPath = dataStorageName.replaceFirst(ValidationConstants.STORAGE_PREFIX, "");
    datasetPath = datasetPath.replaceFirst(bucketName, "");

    if (datasetPath.indexOf(ValidationConstants.STORAGE_SEPARATOR) == 0) {
      datasetPath = datasetPath.replaceFirst(ValidationConstants.STORAGE_SEPARATOR, "");
    }

    return datasetPath;
  }
}
