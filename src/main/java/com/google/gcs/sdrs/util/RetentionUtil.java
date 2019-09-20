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

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class of helper methods for retention rules, job, and validations.
 *
 * <p>Many methods deal with dataStorageName. A dataStorageName is a fully qualified URL that
 * identifies a dataset in GCS. For example, gs://[your-bucket]/[your-dataset]. The dataset itself
 * could contain "/", which is used to represent a "directory" like hierarchy. Conceptually, in a
 * file system, bucket is a root directory and dataset defines sub-directories.
 *
 * <p>A
 */
public class RetentionUtil {
  public static final String DEFAULT_DM_REGEX_PATTERN = ".delete_this_folder";
  public static final String DM_REGEX_PATTERN =
      SdrsApplication.getAppConfigProperty(
          "scheduler.task.dmBatchProcessing.dmRegexPattern", DEFAULT_DM_REGEX_PATTERN);
  private static final Logger logger = LoggerFactory.getLogger(RetentionUtil.class);

  /**
   * Extracts the bucket name from the data storage name string
   *
   * @param dataStorageName the full data storage name path
   * @return the bucket name only
   */
  public static String getBucketName(String dataStorageName) {
    if (dataStorageName == null) {
      return "";
    }

    String bucketName = dataStorageName.replaceFirst(ValidationConstants.STORAGE_PREFIX, "");

    int separatorIndex = bucketName.indexOf(ValidationConstants.STORAGE_SEPARATOR);

    if (separatorIndex != -1) {
      bucketName = bucketName.substring(0, separatorIndex);
    }

    return bucketName;
  }

  /**
   * Extracts the bucket name from the data storage name string and appends the suffix
   *
   * @param dataStorageName the full data storage name path
   * @param suffix the string to append to the bucket name
   * @return the bucket name with the suffix appended
   */
  public static String getBucketName(String dataStorageName, String suffix) {
    return getBucketName(dataStorageName).concat(suffix);
  }

  /**
   * Extracts the dataset path from the data storage name string
   *
   * @param dataStorageName the full data storage name path
   * @return the dataset path without the root bucket
   */
  public static String getDatasetPath(String dataStorageName) {
    if (dataStorageName == null) {
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

  /**
   * Extracts the dataset path of a delete marker. A delete marker is a GCS object that triggers a
   * delete of a dataset containing the delete marker. A delete marker is defined by a
   * pre-configured regex pattern. The default pattern is .delete_this_folder.
   *
   * @param dmTarget A full GCS url to a delete marker object. i.e
   *     gs://[your-bucket]/[your-dataset]/.delete_this_folder
   * @return the dataset path without the root bucket and the delete marker.
   */
  public static String getDmDatasetPath(String dmTarget) {
    String datasetPath = getDatasetPath(dmTarget);

    if (datasetPath.lastIndexOf("/") > 0
        && datasetPath.lastIndexOf("/") < datasetPath.length() - 1) {
      datasetPath = datasetPath.substring(0, datasetPath.lastIndexOf("/"));
    } else {
      datasetPath = null;
    }

    return datasetPath;
  }

  /**
   * Check where or not a dataStroageName ends with a valid delete marker.
   *
   * @param dmTarget A full GCS url to a delete marker object.
   * @return
   */
  public static boolean isValidDeleteMarker(String dmTarget) {
    if (dmTarget == null) {
      return false;
    }

    if (dmTarget.lastIndexOf("/") < 0) {
      return false;
    } else {
      String deleteMarker = dmTarget.substring(dmTarget.lastIndexOf("/") + 1);
      return Pattern.matches(DM_REGEX_PATTERN, deleteMarker);
    }
  }
}
