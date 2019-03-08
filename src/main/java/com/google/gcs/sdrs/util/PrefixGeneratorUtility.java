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
import java.util.List;

/** A utility to generate bucket name prefixes within a time interval. */
public class PrefixGeneratorUtility {

  /**
   * Generate a list of bucket name prefixes within a time interval.
   *
   * @param pattern indicating the base portion of the prefix.
   * @param startTime indicating the time of the least recent prefix to generate. This value must be
   *     earlier than {@code endTime}. There is no guarantee that files older than this value will
   *     not be deleted.
   * @param endTime indicating the time of the most recent prefix to generate.
   * @return a {@link List} of {@link String}s of the form `pattern/period` for every time segment
   *     within the interval between endTime and startTime.
   */
  public static List<String> generateTimePrefixes(
      String pattern, ZonedDateTime startTime, ZonedDateTime endTime) {

    if (endTime.isBefore(startTime)) {
      throw new IllegalArgumentException("endTime occurs before startTime; try swapping them.");
    }

    endTime = endTime.truncatedTo(ChronoUnit.HOURS);

    List<String> result = new ArrayList<>();

    // The general approach here is to work backwards from the endTime to the start time at the
    // largest interval granularity possible. This means that first prefixes on the same day as the
    // endTime will be generated, then month, then year.
    ZonedDateTime currentTime = ZonedDateTime.from(endTime);
    // Generate all prefixes on the current day
    while (currentTime.getHour() > 0 && currentTime.isAfter(startTime)) {
      currentTime = currentTime.minus(1, ChronoUnit.HOURS);
      result.add(formatPrefix(currentTime, pattern, DateTimeFormatter.ofPattern("yyyy/MM/dd/HH")));
    }
    // Generate all prefixes in the current month
    while (currentTime.getDayOfMonth() > 1 && currentTime.isAfter(startTime)) {
      currentTime = currentTime.minus(1, ChronoUnit.DAYS);
      result.add(formatPrefix(currentTime, pattern, DateTimeFormatter.ofPattern("yyyy/MM/dd")));
    }
    // Generate all prefixes in the current year
    while (currentTime.getMonthValue() > 1 && currentTime.isAfter(startTime)) {
      currentTime = currentTime.minus(1, ChronoUnit.MONTHS);
      result.add(formatPrefix(currentTime, pattern, DateTimeFormatter.ofPattern("yyyy/MM")));
    }
    // From here on only year prefixes will be added. This might include values older than the
    // provided startTime
    while (currentTime.isAfter(startTime)) {
      currentTime = currentTime.minus(1, ChronoUnit.YEARS);
      result.add(formatPrefix(currentTime, pattern, DateTimeFormatter.ofPattern("yyyy")));
    }

    return result;
  }

  private static String formatPrefix(
      ZonedDateTime time, String pattern, DateTimeFormatter formatter) {

    String formattedPrefix = String.format("%s/%s",
        pattern, formatter.withZone(ZoneOffset.UTC).format(time));

    // prefixes cannot begin with a /. If the pattern is empty, remove the leading slash
    if (formattedPrefix.startsWith("/")) {
      formattedPrefix = formattedPrefix.replaceFirst("/", "");
    }

    return formattedPrefix;
  }
}
