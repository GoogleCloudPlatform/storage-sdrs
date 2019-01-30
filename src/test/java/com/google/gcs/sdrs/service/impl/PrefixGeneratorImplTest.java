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

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PrefixGeneratorImplTest {

  @Before
  public void setup() {}

  @Test
  public void patternPrecedesPrefix() {
    String pattern = "pattern/example";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertTrue(result.get(0).startsWith("pattern/example/"));
  }

  @Test
  public void hourlyGeneratorWithEmptyRangeIs1Prefix() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(1, result.size());
  }

  @Test
  public void hourlyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01/00", result.get(0));
  }

  @Test
  public void dailyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01", result.get(0));
  }

  @Test
  public void monthlyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 2, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(1, result.size());
    assertEquals("test/2019/01", result.get(0));
  }

  @Test
  public void yearlyGeneratorPrefixFormatMatchesExpectedOutput() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(1, result.size());
    assertEquals("test/2019", result.get(0));
  }

  @Test
  public void prefixGeneratorFluctuatesInterval() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 2, 2, 1, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(4, result.size());
    assertEquals("test/2019", result.get(0));
    assertEquals("test/2020/01", result.get(1));
    assertEquals("test/2020/02/01", result.get(2));
    assertEquals("test/2020/02/02/00", result.get(3));
  }

  @Test
  public void generatesMultiplePrefixesOfVariousIntervals() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 3, 3, 2, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(8, result.size());
    assertEquals("test/2018", result.get(0));
    assertEquals("test/2019", result.get(1));
    assertEquals("test/2020/01", result.get(2));
    assertEquals("test/2020/02", result.get(3));
    assertEquals("test/2020/03/01", result.get(4));
    assertEquals("test/2020/03/02", result.get(5));
    assertEquals("test/2020/03/03/00", result.get(6));
    assertEquals("test/2020/03/03/01", result.get(7));
  }

  @Test
  public void tailsAreNotPreserved() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2018, 3, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2020, 3, 3, 2, 0, 0, 0, ZoneOffset.UTC);

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    // Not all of 2018 is between the two dates, but all of 2018 is to be deleted because there is
    // no requirement to preserve old values.
    assertEquals("test/2018", result.get(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void respectsTimeZones() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.ofHours(2));

    PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);
  }

  @Test()
  public void outputsInUtc() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.ofHours(-12));
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 1, 0, 0, 0, ZoneOffset.ofHours(-12));

    List<String> result = PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);

    assertEquals(1, result.size());
    assertEquals("test/2019/01/01/12", result.get(0));
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsWhenTimesAreOutOfOrder() {
    String pattern = "test";
    ZonedDateTime time1 = ZonedDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    ZonedDateTime time2 = ZonedDateTime.of(2019, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    PrefixGeneratorImpl.generateTimePrefixes(pattern, time2, time1);
  }
}
