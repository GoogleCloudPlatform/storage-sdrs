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

import java.util.LinkedList;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ValidationResultTest {

  @Test
  public void composeIncludesAllMemberMessagesInResult() {
    List<String> messages1 = new LinkedList<>();
    messages1.add("1");
    messages1.add("2");
    List<String> messages2 = new LinkedList<>();
    messages2.add("3");
    ValidationResult result1 = new ValidationResult(messages1);
    ValidationResult result2 = new ValidationResult(messages2);
    List<ValidationResult> results = new LinkedList<>();
    results.add(result1);
    results.add(result2);

    ValidationResult composed = ValidationResult.compose(results);

    assertTrue(composed.validationMessages.contains("1"));
    assertTrue(composed.validationMessages.contains("2"));
    assertTrue(composed.validationMessages.contains("3"));
    assertEquals(composed.validationMessages.size(), 3);
  }

  @Test
  public void fromString() {
    ValidationResult result = ValidationResult.fromString("test");

    assertTrue(result.validationMessages.contains("test"));
    assertEquals(result.validationMessages.size(), 1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void modifyingContentsThrowsError() {
    ValidationResult result = ValidationResult.fromString("test");
    result.validationMessages.add("test2");
  }
}
