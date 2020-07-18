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

package com.google.gcs.sdrs.service.worker.rule.impl;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.api.services.storagetransfer.v1.model.TransferSpec;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.PrefixGeneratorUtility;
import com.google.gcs.sdrs.util.StsUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CredentialsUtil.class, StsUtil.class, PrefixGeneratorUtility.class})
@PowerMockIgnore("javax.management.*")
public class StsRuleExecutorTest {

  private StsRuleExecutor objectUnderTest;
  private RetentionRule testRule;
  private String dataStorageName = "gs://test/dataset";
  private String projectId = "sdrs-test";
  private String transferJobName = "testjob";
  private CredentialsUtil mockCredentialsUtil;

  @Before
  public void setup() {
    testRule = new RetentionRule();
    testRule.setId(1);
    testRule.setProjectId(projectId);
    testRule.setDatasetName("test");
    testRule.setRetentionValue("30:day");
    testRule.setDataStorageName(dataStorageName);
    testRule.setType(RetentionRuleType.DATASET);
    testRule.setVersion(2);

    mockCredentialsUtil = mock(CredentialsUtil.class);
    PowerMockito.mockStatic(CredentialsUtil.class);
    when(CredentialsUtil.getInstance()).thenReturn(mockCredentialsUtil);
    PowerMockito.mockStatic(StsUtil.class);
    when(StsUtil.createStsClient(any())).thenReturn(null);
    PowerMockito.mockStatic(PrefixGeneratorUtility.class);

    objectUnderTest = spy(StsRuleExecutor.getInstance());
  }

  @Test
  public void globalRuleExecutionWithDatasetType() {
    try {
      Collection<RetentionRule> bucketRules = new HashSet<>();
      Collection<RetentionRule> defaultRules = new HashSet<>();
      ZonedDateTime now = ZonedDateTime.now(Clock.systemUTC());
      objectUnderTest.executeDefaultRule(testRule, defaultRules, bucketRules, now, "project-id");
    } catch (IllegalArgumentException ex) {
      assertTrue(true);
    } catch (Exception ex) {
      Assert.fail();
    }
  }

  @Test
  public void executeDatasetRuleSuccess() throws IOException {
    Collection<RetentionRule> datasetRules = new HashSet<>();
    datasetRules.add(testRule);

    TransferJob transferJob = createBasicTransferJob();
    List<TransferJob> transferJobList = new ArrayList<>();
    transferJobList.add(transferJob);

    when(PrefixGeneratorUtility.generateTimePrefixes(
        any(), any(), (ZonedDateTime) notNull())).thenCallRealMethod();
    doReturn(transferJobList).when(objectUnderTest).findPooledJobs(
            any(), any(), any(), any(), anyInt());
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
        any(), any(), any(), any(), any());
    when(StsUtil.updateExistingJob(any(), any(), any(), any())).thenReturn(transferJob);

    List<RetentionJob> datasetRuleJobs = objectUnderTest.executeDatasetRule(datasetRules, projectId);

    verify(objectUnderTest, times(1)).findPooledJobs(
            any(), any(),any(),any(), anyInt());
    verify(objectUnderTest, times(1)).sendInactiveDatasetNotification(
        any(), any(), any(), any(), any());
    assertEquals(1, datasetRuleJobs.size());
    assertEquals(transferJobName, datasetRuleJobs.get(0).getName());
  }

  @Test
  public void executeDatasetRuleWithExceptionThrowByTimePrefixesGeneration() {
    Collection<RetentionRule> datasetRules = new HashSet<>();
    datasetRules.add(testRule);

    when(PrefixGeneratorUtility.generateTimePrefixes(
        any(), any(), (ZonedDateTime) notNull())).thenThrow(new IllegalArgumentException());
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
        any(), any(), any(), any(), any());

    List<RetentionJob> datasetRuleJobs = objectUnderTest.executeDatasetRule(datasetRules, projectId);

    verify(objectUnderTest, never()).sendInactiveDatasetNotification(
        any(), any(), any(), any(), any());
    assertEquals(1, datasetRuleJobs.size());
    //jobname is null will be used to mark error job
    assertEquals(null, datasetRuleJobs.get(0).getName());
    assertEquals(dataStorageName,
        datasetRuleJobs.get(0).getRetentionRuleDataStorageName());
  }

  @Test
  public void executeDatasetRuleWithEmptyPrefixesList() throws IOException {
    Collection<RetentionRule> datasetRules = new HashSet<>();
    datasetRules.add(testRule);

    TransferJob transferJob = createBasicTransferJob();
    List<TransferJob> transferJobList = new ArrayList<>();
    transferJobList.add(transferJob);

    when(PrefixGeneratorUtility.generateTimePrefixes(
        any(), any(), (ZonedDateTime) notNull())).thenReturn(new ArrayList<>());
    doReturn(transferJobList).when(objectUnderTest).findPooledJobs(
            any(), any(), any(), any(), anyInt());
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
        any(), any(), any(), any(), any());


    List<RetentionJob> datasetRuleJobs = objectUnderTest.executeDatasetRule(datasetRules, projectId);

    verify(objectUnderTest, never()).findPooledJobs(any(), any(),any(),any(), anyInt());
    verify(objectUnderTest, never()).sendInactiveDatasetNotification(
        any(), any(), any(), any(), any());
    assertEquals(1, datasetRuleJobs.size());
    //jobname is null will be used to mark error job
    assertEquals(null, datasetRuleJobs.get(0).getName());
    assertEquals(dataStorageName,
        datasetRuleJobs.get(0).getRetentionRuleDataStorageName());
  }

  @Test
  public void buildRetentionJobTest() {
    String jobName = "test";

    RetentionJob result = objectUnderTest.buildRetentionJobEntity(jobName, testRule, null, null);

    assertEquals(result.getName(), jobName);
    assertEquals((int) result.getRetentionRuleId(), (int) testRule.getId());
    assertEquals(result.getRetentionRuleDataStorageName(), testRule.getDataStorageName());
    assertEquals(result.getRetentionRuleType(), testRule.getType());
    assertEquals((int) result.getRetentionRuleVersion(), (int) testRule.getVersion());
  }

  private TransferJob createBasicTransferJob() {
    TransferJob transferJob = new TransferJob();
    transferJob.setStatus(StsUtil.STS_ENABLED_STRING);
    TransferSpec transferSpec = new TransferSpec();
    transferJob.setName(transferJobName);
    transferJob.setTransferSpec(transferSpec);
    transferJob.setLastModificationTime(
        ZonedDateTime.now(Clock.systemUTC()).minusMinutes(10).toString());
    transferJob.setSchedule(
        new Schedule()
            .setStartTimeOfDay(new TimeOfDay().setHours(10).setMinutes(10).setSeconds(10).setNanos(10)));
    return transferJob;
  }
}
