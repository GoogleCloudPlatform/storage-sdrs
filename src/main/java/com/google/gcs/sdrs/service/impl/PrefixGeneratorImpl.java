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

package com.google.gcs.sdrs.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** A service to generate bucket name prefixes within a time interval. */
public class PrefixGeneratorImpl {

  /**
   * Generate a list of bucket name prefixes within a time interval.
   *
   * @param pattern indicating the base portion of the prefix.
   * @param upperBound indicating the time of the most recent prefix to generate (exclusive).
   * @param lowerBound indicating the time of the least recent prefix to generate. This value must
   *     be earlier than {@code upperBound}. There is no guarantee that files older than this value
   *     will not be deleted.
   * @return a {@link List} of {@link String}s of the form `pattern/period` for every time segment
   *     within the interval between upperBound and lowerBound.
   */
  public static List<String> generateTimePrefixes(
      String pattern, LocalDateTime upperBound, LocalDateTime lowerBound) {
    if (upperBound.isBefore(lowerBound)) {
      throw new IllegalArgumentException("upperBound occurs before lowerBound; try swapping them.");
    }
    List<String> result = new LinkedList<>();

    Map<ChronoUnit, DateTimeFormatter> formatters = new HashMap<>();
    formatters.put(ChronoUnit.YEARS, DateTimeFormatter.ofPattern("yyyy"));
    formatters.put(ChronoUnit.MONTHS, DateTimeFormatter.ofPattern("yyyy/MM"));
    formatters.put(ChronoUnit.DAYS, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    formatters.put(ChronoUnit.HOURS, DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));

    LocalDateTime currentTime = LocalDateTime.from(lowerBound);
    while (currentTime.isBefore(upperBound)) {
      ChronoUnit increment;
      if (!upperBound.isBefore(currentTime.plus(1, ChronoUnit.YEARS))) {
        increment = ChronoUnit.YEARS;
      } else if (!upperBound.isBefore(currentTime.plus(1, ChronoUnit.MONTHS))) {
        increment = ChronoUnit.MONTHS;
      } else if (!upperBound.isBefore(currentTime.plus(1, ChronoUnit.DAYS))) {
        increment = ChronoUnit.DAYS;
      } else {
        increment = ChronoUnit.HOURS;
      }
      result.add(String.format("%s/%s", pattern, formatters.get(increment).format(currentTime)));
      currentTime = currentTime.plus(1, increment);
    }

    return result;
  }
}
