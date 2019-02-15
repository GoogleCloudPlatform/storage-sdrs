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
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrefixGeneratorUtilityTest {

  @Before
  public void setup() {}

  @Test
  public void patternPrecedesPrefix() {
    String pattern = "pattern/example";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.get(0).startsWith("pattern/example/"));
  }

  @Test
  public void hourlyGeneratorWithEmptyRangeIs1Prefix() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
  }

  @Test
  public void hourlyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01/00", result.get(0));
  }

  @Test
  public void dailyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01", result.get(0));
  }

  @Test
  public void monthlyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
    assertEquals("test/2019/01", result.get(0));
  }

  @Test
  public void yearlyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
    assertEquals("test/2019", result.get(0));
  }

  @Test
  public void startMonthValueGreaterThanEndMonthWorks() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 9, 19, 12, 50, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 3, 11, 10, 30, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.contains("test/2019/02"));
  }

  @Test
  public void noExcessiveMonthPrefixesAreGenerated() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 4, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 6, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.contains("test/2019/05"));
    assertTrue(result.contains("test/2019/04"));
    assertEquals(2, result.size());
  }

  @Test
  public void startDayValueGreaterThanEndDayWorks() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 9, 19, 12, 50, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 3, 11, 10, 30, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.contains("test/2019/03/10"));
  }

  @Test
  public void noExcessiveDayPrefixesAreGenerated() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 3, 4, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 3, 6, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.contains("test/2019/03/05"));
    assertTrue(result.contains("test/2019/03/04"));
    assertEquals(2, result.size());
  }

  @Test
  public void startHourValueGreaterThanEndHourWorks() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 9, 19, 12, 50, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 3, 11, 10, 30, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.contains("test/2019/03/11/00"));
  }

  @Test
  public void noExcessiveHourPrefixesAreGenerated() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 3, 3, 4, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 3, 3, 6, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertTrue(result.contains("test/2019/03/03/04"));
    assertTrue(result.contains("test/2019/03/03/05"));
    assertEquals(2, result.size());
  }

  @Test
  public void prefixGeneratorFluctuatesInterval() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 2, 2, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(4, result.size());
    assertTrue(result.contains("test/2019"));
    assertTrue(result.contains("test/2020/01"));
    assertTrue(result.contains("test/2020/02/01"));
    assertTrue(result.contains("test/2020/02/02/00"));
  }

  @Test
  public void generatesMultiplePrefixesOfVariousIntervals() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 3, 3, 2, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(8, result.size());
    assertTrue(result.contains("test/2018"));
    assertTrue(result.contains("test/2019"));
    assertTrue(result.contains("test/2020/01"));
    assertTrue(result.contains("test/2020/02"));
    assertTrue(result.contains("test/2020/03/01"));
    assertTrue(result.contains("test/2020/03/02"));
    assertTrue(result.contains("test/2020/03/03/00"));
    assertTrue(result.contains("test/2020/03/03/01"));
  }

  @Test
  public void tailsAreNotPreserved() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 3, 3, 2, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    // Not all of 2018 is between the two dates, but all of 2018 is to be deleted because there is
    // no requirement to preserve old values.
    assertTrue(result.contains("test/2018"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void respectsTimeZones() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.ofHours(2));

    PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);
  }

  @Test
  public void outputsInUtc() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-12));
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.ofHours(-12));

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01/12", result.get(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsWhenTimesAreOutOfOrder() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);
  }

  @Test
  public void mostRecentMinutesAreTruncated() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 30, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorUtility.generateTimePrefixes(pattern, time1, time2);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01/00", result.get(0));
  }
}
