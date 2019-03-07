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

import com.google.api.services.storagetransfer.v1.model.Date;
import com.google.api.services.storagetransfer.v1.model.ObjectConditions;
import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StsUtilTest {

  @Before
  public void setup() {
    StsUtil.quotaManager = mock(StsQuotaManager.class);
    when(StsUtil.quotaManager.submitStsJob(any(), any())).thenReturn("uuid");
  }

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
}
