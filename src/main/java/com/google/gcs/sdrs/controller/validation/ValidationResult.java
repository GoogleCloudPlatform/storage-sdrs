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
import java.util.Collections;
import java.util.LinkedList;

/** Exposes a list of validation messages */
public class ValidationResult {

  /**
   * An immutable collection of user actionable changes required to pass a validation check.
   */
  public final Collection<String> validationMessages;

  /** Flag indicating that validation checks were passed */
  public final boolean isValid;

  /** Creates a ValidationResult */
  public ValidationResult(Collection<String> validationMessages) {
    Collection<String> collection = new LinkedList<>(validationMessages);
    this.validationMessages = Collections.unmodifiableCollection(collection);
    this.isValid = collection.size() == 0;
  }

  /** Create a ValidationResult from a composite of partial results. */
  public static ValidationResult compose(Collection<ValidationResult> validationResults) {
    Collection<String> partialMessages = new LinkedList<>();
    for (ValidationResult validationResult : validationResults) {
      partialMessages.addAll(validationResult.validationMessages);
    }
    return new ValidationResult(partialMessages);
  }

  /** Convenience method for creating a ValidationResult with a single validation message */
  public static ValidationResult fromString(String validationMessage) {
    Collection<String> messages = new LinkedList<>();
    messages.add(validationMessage);
    return new ValidationResult(messages);
  }
}
