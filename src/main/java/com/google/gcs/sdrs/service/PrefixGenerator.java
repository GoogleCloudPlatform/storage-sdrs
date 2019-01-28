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

package com.google.gcs.sdrs.service;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;
import java.util.List;

/** A service to generate bucket name prefixes within a time interval. */
public class PrefixGenerator {

  /**
   * Generate a list of bucket name prefixes with hourly suffix
   *
   * @param pattern indicating the base portion of the prefix.
   * @param rangeStart indicating the time of the most recent prefix to generate (inclusive).
   * @param rangeEnd indicating the time of the least recent prefix to generate (inclusive). This
   *     value must be earlier than {@code rangeStart}.
   * @return a {@link List} of {@link String}s of the form `pattern/yyyy/mm/dd/hh` for every hour
   *     within the interval between rangeStart and rangeEnd.
   */
  public List<String> generateHourlyPrefixes(String pattern, Instant rangeStart, Instant rangeEnd) {
    List<String> result = new LinkedList<>();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/mm/dd/hh");


    Instant nextPrefixTime = Instant.from(rangeStart);
    while (nextPrefixTime.isBefore(rangeEnd)) {
      result.add(String.format("%s/%s", pattern, formatter.format(nextPrefixTime)));

      nextPrefixTime.minus(1, ChronoUnit.HOURS);
    }

    return result;
  }
}
