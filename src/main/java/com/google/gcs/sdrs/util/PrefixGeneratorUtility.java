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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A utility to generate bucket name prefixes within a time interval. */
public class PrefixGeneratorUtility {

  /**
   * Generate a list of bucket name prefixes within a time interval.
   *
   * @param pattern indicating the base portion of the prefix.
   * @param startTime indicating the time of the least recent prefix to generate. This value must
   *     be earlier than {@code endTime}. There is no guarantee that files older than this value
   *     will not be deleted.
   * @param endTime indicating the time of the most recent prefix to generate.
   * @return a {@link List} of {@link String}s of the form `pattern/period` for every time
   *     segment within the interval between endTime and startTime.
   */
  public static List<String> generateTimePrefixes(
      String pattern, ZonedDateTime startTime, ZonedDateTime endTime) {

    if (endTime.isBefore(startTime)) {
      throw new IllegalArgumentException(
          "endTime occurs before startTime; try swapping them.");
    }

    endTime = endTime.truncatedTo(ChronoUnit.HOURS);

    List<String> result = new ArrayList<>();

    Map<ChronoUnit, DateTimeFormatter> formatters = new HashMap<>();
    formatters.put(ChronoUnit.YEARS, DateTimeFormatter.ofPattern("yyyy"));
    formatters.put(ChronoUnit.MONTHS, DateTimeFormatter.ofPattern("yyyy/MM"));
    formatters.put(ChronoUnit.DAYS, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
    formatters.put(ChronoUnit.HOURS, DateTimeFormatter.ofPattern("yyyy/MM/dd/HH"));

    ZonedDateTime currentTime = ZonedDateTime.from(startTime);
    while (currentTime.isBefore(endTime)) {
      ChronoUnit increment;
      if (!endTime.isBefore(currentTime.plus(1, ChronoUnit.YEARS))) {
        increment = ChronoUnit.YEARS;
      } else if (!endTime.isBefore(currentTime.plus(1, ChronoUnit.MONTHS))) {
        increment = ChronoUnit.MONTHS;
      } else if (!endTime.isBefore(currentTime.plus(1, ChronoUnit.DAYS))) {
        increment = ChronoUnit.DAYS;
      } else {
        increment = ChronoUnit.HOURS;
      }

      DateTimeFormatter formatter = formatters.get(increment).withZone(ZoneOffset.UTC);
      result.add(String.format("%s/%s", pattern, formatter.format(currentTime)));
      currentTime = currentTime.plus(1, increment);
    }

    return result;
  }
}
