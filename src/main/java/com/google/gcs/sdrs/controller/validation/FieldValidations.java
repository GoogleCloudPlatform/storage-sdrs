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

import java.util.Collection;
import java.util.HashSet;

/** Exposes static methods for validation shared between controllers. */
public class FieldValidations {
  public static final String STORAGE_PREFIX = "gs://";
  public static final String STORAGE_SEPARATOR = "/";

  private FieldValidations() {}

  /**
   * Runs a validation check on an arbitrary field that conforms to bucket naming conventions
   *
   * @param fieldName the JSON field name
   * @param fieldValue the input value for the field being validated
   * @return A validation result containing a list of validation error messages
   */
  public static ValidationResult validateFieldFollowsBucketNamingStructure(
      String fieldName, String fieldValue) {

    Collection<String> validationMessages = new HashSet<>();
    if (fieldValue == null) {
      validationMessages.add(String.format("%s must be provided", fieldName));
    } else {
      // Field value should match gs://<bucket_name>/<dataset_name>
      if (!fieldValue.startsWith(STORAGE_PREFIX)) {
        validationMessages.add(String.format("%s must start with '%s'", fieldName, STORAGE_PREFIX));
      } else {
        String bucketAndDataset = fieldValue.substring(STORAGE_PREFIX.length());
        String[] pathSegments = bucketAndDataset.split(STORAGE_SEPARATOR);

        if (pathSegments[0].length() == 0) {
          validationMessages.add(String.format("%s must include a bucket name", fieldName));
        }
      }
    }
    return new ValidationResult(validationMessages);
  }
}
