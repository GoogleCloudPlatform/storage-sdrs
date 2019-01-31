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

package com.google.gcs.sdrs.dao.converter;

import com.google.gcs.sdrs.enums.RetentionRuleType;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RetentionRuleTypeConverterTest {

  private RetentionRuleTypeConverter converter;

  @Before
  public void setup() {
    converter = new RetentionRuleTypeConverter();
  }

  @Test
  public void convertsDatasetToString() {
    String result = converter.convertToDatabaseColumn(RetentionRuleType.DATASET);
    assertEquals(result, RetentionRuleType.DATASET.toString());
  }

  @Test
  public void convertsGlobalToString() {
    String result = converter.convertToDatabaseColumn(RetentionRuleType.GLOBAL);
    assertEquals(result, RetentionRuleType.GLOBAL.toString());
  }

  @Test
  public void convertsDatasetToRetentionRuleType() {
    RetentionRuleType result = converter.convertToEntityAttribute("dataset");
    assertEquals(result, RetentionRuleType.DATASET);
  }

  @Test
  public void convertsGlobalToRetentionRuleType() {
    RetentionRuleType result = converter.convertToEntityAttribute("global");
    assertEquals(result, RetentionRuleType.GLOBAL);
  }

  @Test(expected = IllegalArgumentException.class)
  public void convertsBadValueToError() {
    converter.convertToEntityAttribute("bad");
  }
}
