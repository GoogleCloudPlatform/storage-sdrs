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

package com.google.gcs.sdrs.rule.impl;

import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StsRuleExecutorTest {

  private StsRuleExecutor objectUnderTest;
  private String dataStorageName = "gs://test";
  private RetentionRule testRule;

  @Before
  public void initialize(){
    testRule = new RetentionRule();
    testRule.setId(1);
    testRule.setProjectId("sdrs-test");
    testRule.setDatasetName("test");
    testRule.setRetentionPeriodInDays(30);
    testRule.setDataStorageName(dataStorageName);
    testRule.setType(RetentionRuleType.DATASET);
    testRule.setVersion(2);

    objectUnderTest = StsRuleExecutor.getInstance();
  }

  @Test
  public void datasetRuleExecutionWithGlobalType(){
    try {
      testRule.setType(RetentionRuleType.GLOBAL);
      objectUnderTest.executeDatasetRule(testRule);
    } catch (IllegalArgumentException ex) {
      assertTrue(true);
    } catch (IOException ex) {
      Assert.fail();
    }
  }

  @Test
  public void globalRuleExecutionWithDatasetType(){
    try {
      Collection<RetentionRule> bucketRules = new HashSet<>();
      ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
      objectUnderTest.executeDefaultRule(testRule, bucketRules, now);
    } catch (IllegalArgumentException ex) {
      assertTrue(true);
    } catch (IOException ex) {
      Assert.fail();
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void globalRuleExecutionWithOver1000DatasetRules(){
    try {
      testRule.setType(RetentionRuleType.GLOBAL);
      ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

      Collection<RetentionRule> bucketRules = new HashSet<>();
      for(int i = 0; i < 1002; i++) {
        RetentionRule rule = new RetentionRule();
        rule.setDataStorageName(dataStorageName + "/myPath" + String.valueOf(i));
        rule.setProjectId("test-project-id");
        bucketRules.add(rule);
      }
      objectUnderTest.executeDefaultRule(testRule, bucketRules, now);
      Assert.fail();
    } catch (IOException ex) {
      Assert.fail();
    }
  }

  @Test(expected = IndexOutOfBoundsException.class)
  public void globalRuleExecutionNoProjectId(){
    try{
      testRule.setType(RetentionRuleType.GLOBAL);
      testRule.setProjectId("");
      ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());

      Collection<RetentionRule> bucketRules = new HashSet<>();
      RetentionRule bucketRule = new RetentionRule();
      bucketRule.setProjectId("");
      bucketRule.setDataStorageName("");
      bucketRules.add(bucketRule);
      objectUnderTest.executeDefaultRule(testRule, bucketRules, now);
      Assert.fail();
    } catch (IOException ex){
      Assert.fail();
    }
  }

  @Test
  public void buildRetentionJobTest(){
    String jobName = "test";

    RetentionJob result = objectUnderTest.buildRetentionJobEntity(jobName, testRule);

    assertEquals(result.getName(), jobName);
    assertEquals((int)result.getRetentionRuleId(), (int)testRule.getId());
    assertEquals(result.getRetentionRuleDataStorageName(), testRule.getDataStorageName());
    assertEquals(result.getRetentionRuleType(), testRule.getType());
    assertEquals((int)result.getRetentionRuleVersion(), (int)testRule.getVersion());
  }
}
