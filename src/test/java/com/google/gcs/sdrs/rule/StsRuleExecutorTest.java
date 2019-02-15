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

package com.google.gcs.sdrs.rule;

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

  StsRuleExecutor objectUnderTest;
  String dataStorageName = "gs://test";
  String expectedBucketName = "test";
  String suffix = "shadow";
  RetentionRule testRule;

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
        rule.setDataStorageName(dataStorageName);
        bucketRules.add(rule);
      }
      objectUnderTest.executeDefaultRule(testRule, bucketRules, now);
      Assert.fail();
    } catch (IOException ex) {
      Assert.fail();
    }
  }

  @Test(expected = IllegalArgumentException.class)
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
  public void getBucketNameRootTest(){
    String result = objectUnderTest.getBucketName(dataStorageName);
    assertEquals(expectedBucketName, result);
  }

  @Test
  public void getBucketNameTrailingSlashTest(){
    dataStorageName = dataStorageName.concat("/");
    String result = objectUnderTest.getBucketName(dataStorageName);

    assertEquals(expectedBucketName, result);
  }

  @Test
  public void getBucketNameWithPathTest(){
    dataStorageName = dataStorageName.concat("/test/myLog");
    String result = objectUnderTest.getBucketName(dataStorageName);

    assertEquals(expectedBucketName, result);
  }

  @Test
  public void getBucketNameNullTest(){
    String result = objectUnderTest.getBucketName(null);

    assertEquals("", result);
  }

  @Test
  public void getBucketNameWithSuffix(){
    String result = objectUnderTest.getBucketName(dataStorageName, suffix);;

    assertEquals(expectedBucketName.concat(suffix), result);
  }

  @Test
  public void getBucketNameWithSuffixAndTrailingSlash(){
    dataStorageName = dataStorageName.concat("/");
    String result = objectUnderTest.getBucketName(dataStorageName, suffix);

    assertEquals(expectedBucketName.concat(suffix), result);
  }

  @Test
  public void getBucketNameWithSuffixAndPathTest(){
    dataStorageName = dataStorageName.concat("/dataset/myLog");
    String result = objectUnderTest.getBucketName(dataStorageName, suffix);

    assertEquals(expectedBucketName.concat(suffix), result);
  }

  @Test
  public void getDatasetPathTest(){
    String path = "/dataset/myLog";
    dataStorageName = dataStorageName.concat(path);

    String result = objectUnderTest.getDatasetPath(dataStorageName);
    String expected = path.replaceFirst("/", "");

    assertEquals(expected, result);
  }

  @Test
  public void getDatasetPathBucketOnlyTest(){
    String result = objectUnderTest.getDatasetPath(dataStorageName);

    assertEquals("", result);
  }

  @Test
  public void getDatasetPathTrailingSlash(){
    String result = objectUnderTest.getDatasetPath(dataStorageName.concat("/"));

    assertEquals("", result);
  }

  @Test
  public void getDatasetPathNull(){
    String result = objectUnderTest.getDatasetPath(null);

    assertEquals("", result);
  }

  @Test
  public void getDatasetPathSameName(){
    String expected = "test";
    String fullPath = dataStorageName + "/" + expected;
    String result = objectUnderTest.getDatasetPath(fullPath);

    assertEquals(expected, result);
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
