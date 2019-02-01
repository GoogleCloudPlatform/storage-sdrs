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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FieldValidationsTest {

  @Test
  public void createDatasetRuleMissingDataStorageFails() {
    ValidationResult result =
        FieldValidations.validateFieldFollowsBucketNamingStructure("dataStorageName", null);
    assertTrue(result.validationMessages.contains("dataStorageName must be provided"));
    assertEquals(1, result.validationMessages.size());
  }

  @Test
  public void createDatasetRuleMissingDataStoragePrefixFails() {
    ValidationResult result =
        FieldValidations.validateFieldFollowsBucketNamingStructure(
            "dataStorageName", "bucket/dataset");

    assertTrue(result.validationMessages.contains("dataStorageName must start with 'gs://'"));
  }

  @Test
  public void createDatasetRuleMissingDataStorageBucketFails() {
    ValidationResult result =
        FieldValidations.validateFieldFollowsBucketNamingStructure(
            "dataStorageName", "gs:///dataset");

    assertTrue(result.validationMessages.contains("dataStorageName must include a bucket name"));
  }

  @Test
  public void createDatasetRuleMissingDataStorageDatasetFails() {
    ValidationResult result =
        FieldValidations.validateFieldFollowsBucketNamingStructure(
            "dataStorageName", "gs://bucket");

    assertTrue(result.validationMessages.contains("dataStorageName must include a dataset name"));
  }
}
