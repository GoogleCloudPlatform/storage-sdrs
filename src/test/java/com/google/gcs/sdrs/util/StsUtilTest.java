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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.api.client.util.BackOff;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.storagetransfer.v1.model.Date;
import com.google.api.services.storagetransfer.v1.model.ObjectConditions;
import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class StsUtilTest {

  @Test
  public void intDaysToDurationStringTest() {
    int days = 1;
    // hours * minutes * seconds
    String duration = (days * 24 * 60 * 60) + "s";
    String result = StsUtil.convertRetentionInDaysToDuration(days);

    assertEquals(duration, result);
  }

  @Test
  public void createDateFromLocalDate() {
    LocalDate now = ZonedDateTime.now(Clock.systemUTC()).toLocalDate();
    Date convertedDate = StsUtil.convertToDate(now);

    assertEquals((int) convertedDate.getYear(), now.getYear());
    assertEquals((int) convertedDate.getMonth(), now.getMonthValue());
    assertEquals((int) convertedDate.getDay(), now.getDayOfMonth());
  }

  @Test
  public void createTimeOfDayFromLocalDate() {
    LocalTime now = ZonedDateTime.now(Clock.systemUTC()).toLocalTime();
    TimeOfDay timeOfDay = StsUtil.convertToTimeOfDay(now);

    assertEquals((int) timeOfDay.getHours(), now.getHour());
    assertEquals((int) timeOfDay.getMinutes(), now.getMinute());
    assertEquals((int) timeOfDay.getSeconds(), now.getSecond());
  }

  @Test
  public void buildScheduleRecurringTest() {
    ZonedDateTime startDateTime = ZonedDateTime.now(Clock.systemUTC());

    Schedule schedule = StsUtil.buildSchedule(startDateTime, false);
    Date startDate = StsUtil.convertToDate(startDateTime.toLocalDate().minusDays(1));

    assertEquals(startDate, schedule.getScheduleStartDate());
    assertNull(schedule.getScheduleEndDate());
  }

  @Test
  public void buildScheduleOneTimeTest() {
    ZonedDateTime startDateTime = ZonedDateTime.now(Clock.systemUTC());

    Schedule schedule = StsUtil.buildSchedule(startDateTime, true);
    Date startDate = StsUtil.convertToDate(startDateTime.toLocalDate().minusDays(1));

    assertEquals(startDate, schedule.getScheduleStartDate());
    assertEquals(startDate, schedule.getScheduleEndDate());
  }

  @Test
  public void buildObjectConditionsExcludeTest() {
    List<String> prefixes = new ArrayList<>();
    prefixes.add("/test/dataset");
    Integer retentionInDays = 1;
    String expectedDurationString = StsUtil.convertRetentionInDaysToDuration(retentionInDays);

    ObjectConditions conditions = StsUtil.buildObjectConditions(prefixes, true, retentionInDays);

    assertEquals(prefixes, conditions.getExcludePrefixes());
    assertNull(conditions.getIncludePrefixes());
    assertEquals(expectedDurationString, conditions.getMinTimeElapsedSinceLastModification());
  }

  @Test
  public void buildObjectConditionsIncludeTest() {
    List<String> prefixes = new ArrayList<>();
    prefixes.add("/test/dataset");
    Integer retentionInDays = 1;
    String expectedDurationString = StsUtil.convertRetentionInDaysToDuration(retentionInDays);

    ObjectConditions conditions = StsUtil.buildObjectConditions(prefixes, false, retentionInDays);

    assertEquals(prefixes, conditions.getIncludePrefixes());
    assertNull(conditions.getExcludePrefixes());
    assertEquals(expectedDurationString, conditions.getMinTimeElapsedSinceLastModification());
  }

  @Test
  public void buildObjectConditionsNullRetentionTest() {
    List<String> prefixes = new ArrayList<>();
    prefixes.add("/test/dataset");

    ObjectConditions conditions = StsUtil.buildObjectConditions(prefixes, false, null);

    assertNull(conditions.getMinTimeElapsedSinceLastModification());
  }

  @Test
  public void backoffTest() {
    try {
      // Test Google  ExponentialBackOff class. It's used to handle STS QPS limit (100 create per
      // 100s)
      // 100s is too long for unit test. Mock it using milliseconds.
      int mockStsQpsIntervalMillis = 100;
      double multiplier = 1.5;
      double randomizationFactor = 0.5;
      int initialIntervalMillis = (int) (mockStsQpsIntervalMillis / (1 - randomizationFactor));
      int maxIntervalMillis = initialIntervalMillis * 10;
      int maxElapsedTimeMillis = initialIntervalMillis * 15;

      System.out.println(
          String.format(
              "initialIntervalMillis=%d; maxIntervalMillis=%d; "
                  + "maxElapsedTimeMillis=%d; multipler=%.1f; factor=%.1f",
              initialIntervalMillis,
              maxIntervalMillis,
              maxElapsedTimeMillis,
              multiplier,
              randomizationFactor));

      ExponentialBackOff backoff =
          new ExponentialBackOff.Builder()
              .setInitialIntervalMillis(initialIntervalMillis)
              .setMaxElapsedTimeMillis(maxElapsedTimeMillis)
              .setMaxIntervalMillis(maxIntervalMillis)
              .setMultiplier(multiplier)
              .setRandomizationFactor(randomizationFactor)
              .build();

      long backOffMillis = backoff.nextBackOffMillis();
      long startTime = Instant.now().toEpochMilli();
      long beginTime = startTime;
      long firstRetryInterval = backOffMillis;
      System.out.println(String.format("Start time: %d", startTime));
      while (backOffMillis != BackOff.STOP) {
        System.out.println(String.format("Sleeping %dms... ", backOffMillis));
        Thread.sleep(backOffMillis);
        long retryTime = Instant.now().toEpochMilli();
        System.out.println(
            String.format(
                "Retrying time: %d;  Time elapsed: %dms", retryTime, retryTime - startTime));
        startTime = retryTime;
        backOffMillis = backoff.nextBackOffMillis();
      }
      long endTime = Instant.now().toEpochMilli();
      System.out.println(String.format("Total elapsed time: %dms", endTime - beginTime));
      assertTrue((endTime - beginTime) >= maxElapsedTimeMillis);
      assertTrue(firstRetryInterval >= mockStsQpsIntervalMillis);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
