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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.api.services.storagetransfer.v1.model.TransferSpec;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.dao.model.PooledStsJob;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.PrefixGeneratorUtility;
import com.google.gcs.sdrs.util.RetentionUtil;
import com.google.gcs.sdrs.util.StsUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.google.gcs.sdrs.util.StsUtil.SCHEDULE_TIME_DATE_TIME_FORMATTER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
  private TimeOfDay scheduleAt =
          new TimeOfDay().setHours(10).setMinutes(10).setSeconds(10).setNanos(10);
  private CredentialsUtil mockCredentialsUtil;

  @Before
  public void setup() {
    testRule = createRetentionRule(
            1, projectId, "test", "30:day", dataStorageName, 2);

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
  public void executeDatasetRuleSuccessSingleBucket() throws IOException {
    Collection<RetentionRule> datasetRules = new HashSet<>();
    datasetRules.add(testRule);

    TransferJob transferJob = createTransferJob(transferJobName, scheduleAt);
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
  public void executeDatasetRuleSuccessMultipleBuckets() throws IOException {
    Collection<RetentionRule> datasetRules = new HashSet<>();
    String datasetStorageName1 = "gs://testMultipleBucketDataset1/dataset";
    String bucketName1 = RetentionUtil.getBucketName(datasetStorageName1);
    String jobName1 = "executeDatasetRuleSuccessMultipleBuckets_JobName1";
    RetentionRule rule1 = createRetentionRule(
            3, projectId, "testMultipleBucketDataset1",
            "30:day", datasetStorageName1, 3 );
    datasetRules.add(rule1);
    TransferJob transferJob1 = createTransferJob(jobName1, scheduleAt);
    List<TransferJob> transferJobList1 = new ArrayList<>();
    transferJobList1.add(transferJob1);
    String datasetStorageName2 = "gs://testMultipleBucketDataset2/dataset";
    String jobName2 = "executeDatasetRuleSuccessMultipleBuckets_JobName2";
    String bucketName2 = RetentionUtil.getBucketName(datasetStorageName2);
    RetentionRule rule2 = createRetentionRule(
            3, projectId, "testMultipleBucketDataset2",
            "30:day", datasetStorageName2, 3 );
    TransferJob transferJob2 = createTransferJob(jobName2, scheduleAt);
    datasetRules.add(rule2);
    List<TransferJob> transferJobList2 = new ArrayList<>();
    transferJobList2.add(transferJob2);

    when(PrefixGeneratorUtility.generateTimePrefixes(
            any(), any(), (ZonedDateTime) notNull())).thenCallRealMethod();
    doReturn(transferJobList1).when(objectUnderTest).findPooledJobs(
            eq(projectId), eq(bucketName1), any(),
            eq(RetentionRuleType.DATASET), eq(1));
    doReturn(transferJobList2).when(objectUnderTest).findPooledJobs(
            eq(projectId), eq(bucketName2), any(),
            eq(RetentionRuleType.DATASET), eq(1));
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
            any(), any(), any(), any(), any());
    when(StsUtil.updateExistingJob(any(), any(), eq(transferJob1.getName()), any()))
            .thenReturn(transferJob1);
    when(StsUtil.updateExistingJob(any(), any(), eq(transferJob2.getName()), any()))
            .thenReturn(transferJob2);

    List<RetentionJob> datasetRuleJobs = objectUnderTest.executeDatasetRule(datasetRules, projectId);
    datasetRuleJobs.sort(Comparator.comparing(RetentionJob::getName));

    verify(objectUnderTest, times(1)).findPooledJobs(
            eq(projectId), eq(bucketName1), any(),
            eq(RetentionRuleType.DATASET), eq(1));
    verify(objectUnderTest, times(1)).findPooledJobs(
            eq(projectId), eq(bucketName2), any(),
            eq(RetentionRuleType.DATASET), eq(1));
    verify(objectUnderTest, times(1)).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName1), any(), any(), any());
    verify(objectUnderTest, times(1)).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName2), any(), any(), any());
    assertEquals(2, datasetRuleJobs.size());
    assertEquals(transferJob1.getName(), datasetRuleJobs.get(0).getName());
    assertEquals(transferJob2.getName(), datasetRuleJobs.get(1).getName());
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

    TransferJob transferJob = createTransferJob(transferJobName, scheduleAt);
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
  public void executeDatasetRuleInBucketWithLargeSetRules() throws IOException {
    List<RetentionRule> datasetRules = new ArrayList<>();
    int ruleCount = 1000;
    List<String> dataStorageNameList = new ArrayList<>(ruleCount);
    String baseDataStroageName = "gs://executeDatasetRuleInBucketMultipleRules/ds";
    String bucketName = RetentionUtil.getBucketName(baseDataStroageName);
    String baseDatasetName = "ds";
    for (int i = 0; i < ruleCount; ++i) {
      String curDataStorageName = baseDataStroageName + i;
      dataStorageNameList.add(curDataStorageName);
      String curDatasetName = baseDatasetName + i;
      datasetRules.add(
              createRetentionRule(
                      i + 10, projectId, curDatasetName, "30:day", curDataStorageName, 2));
    }
    ZonedDateTime zonedDateTime = ZonedDateTime.now(Clock.systemUTC());
    String bucketScheduleAtStr = zonedDateTime.format(
            DateTimeFormatter.ofPattern(SCHEDULE_TIME_DATE_TIME_FORMATTER));
    int perStsJobPrefixLimit = Integer.parseInt(
            SdrsApplication.getAppConfigProperty("sts.maxPrefixCount"));

    when(PrefixGeneratorUtility.generateTimePrefixes(
            any(), any(), (ZonedDateTime) notNull())).thenCallRealMethod();
    int expectJobCount = objectUnderTest.calcStsJobsNeeded(
            objectUnderTest.buildPrefixesForBucket(bucketName, datasetRules, zonedDateTime),
            perStsJobPrefixLimit);

    Map<String, TransferJob> transferJobMap = new HashMap();
    List<TransferJob> transferJobList = new ArrayList<>(expectJobCount);
    String baseTransferJobName = "executeDatasetRuleInBucketWithLargeSetRulesTransferJobName";
    for (int i = 0; i < expectJobCount; ++i) {
      String curTransferJobName = baseTransferJobName + i;
      TransferJob curJob = createTransferJob(curTransferJobName, scheduleAt);
      transferJobList.add(curJob);
      transferJobMap.put(curTransferJobName, curJob);
    }

    doReturn(transferJobList).when(objectUnderTest).findPooledJobs(
            eq(projectId), eq(bucketName), eq(bucketScheduleAtStr),
            eq(RetentionRuleType.DATASET), eq(expectJobCount));
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName), any(), any(), any());
    doCallRealMethod().when(objectUnderTest).processPrefixes(
            eq(projectId), eq(bucketName), any(), any(), eq(zonedDateTime), any(), anyBoolean());
    when(StsUtil.updateExistingJob(
            any(), (TransferJob) notNull(), startsWith(baseTransferJobName), any()))
            .thenAnswer(
                    invocation -> transferJobMap.get((String)invocation.getArguments()[2]));

    List<RetentionJob> datasetRuleJobs = objectUnderTest.executeDatasetRuleInBucket(
            projectId, bucketName, datasetRules, zonedDateTime);
    datasetRuleJobs.sort(Comparator.comparing(RetentionJob::getName));

    verify(objectUnderTest, times(1)).findPooledJobs(
            any(), any(),any(),any(), anyInt());
    verify(objectUnderTest, times(expectJobCount)).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName), any(), any(), any());
    verify(objectUnderTest, times(expectJobCount)).processPrefixes(
            eq(projectId), eq(bucketName), any(), any(), eq(zonedDateTime), any(), anyBoolean());
    assertEquals(ruleCount, datasetRuleJobs.size());
    for (RetentionJob retentionJob : datasetRuleJobs) {
      assertTrue(transferJobMap.containsKey(retentionJob.getName()));
    }
  }

  @Test
  public void processPrefixesWithPooledJob() throws IOException {
    ZonedDateTime zonedDateTime = ZonedDateTime.now(Clock.systemUTC());
    List<RetentionRule> datasetRules = new ArrayList<>();
    int ruleCount = 5;
    List<String> dataStorageNameList = new ArrayList<>(ruleCount);
    String baseDataStroageName = "gs://processPrefixesWithGoodPooledJob/ds";
    String bucketName = RetentionUtil.getBucketName(baseDataStroageName);
    String baseDatasetName = "ds";
    List<String> prefixesList = new ArrayList<>();
    for (int i = 0; i < ruleCount; ++i) {
      String curDataStorageName = baseDataStroageName + i;
      dataStorageNameList.add(curDataStorageName);
      String curDatasetName = baseDatasetName + i;
      RetentionRule retentionRule = createRetentionRule(
              i + 10, projectId, curDatasetName, "5:day", curDataStorageName, 2);
      datasetRules.add(retentionRule);
      prefixesList.add("gs://processPrefixesWithGoodPooledJob/ds" + i);
    }


    TransferJob pooledJob = createTransferJob(transferJobName, scheduleAt);
    when(StsUtil.updateExistingJob(any(), any(), eq(transferJobName), eq(projectId)))
            .thenAnswer(
                    invocation ->
                            createTransferJob((String) invocation.getArguments()[2], scheduleAt));
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName), eq(prefixesList), any(), any());

    TransferJob job = objectUnderTest.processPrefixes(
            projectId, bucketName, datasetRules, prefixesList,
            zonedDateTime, pooledJob, StsUtil.IS_STS_JOBPOOL_ONLY);


    verify(objectUnderTest, times(1)).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName), any(), any(), any());
    assertEquals(transferJobName, job.getName());
  }

  @Test
  public void processPrefixesWithNullPoolJob() throws IOException {
    ZonedDateTime zonedDateTime = ZonedDateTime.now(Clock.systemUTC());
    List<RetentionRule> datasetRules = new ArrayList<>();
    int ruleCount = 5;
    List<String> dataStorageNameList = new ArrayList<>(ruleCount);
    String baseDataStroageName = "gs://processPrefixesWithGoodPooledJob/ds";
    String bucketName = RetentionUtil.getBucketName(baseDataStroageName);
    String baseDatasetName = "ds";
    List<String> prefixesList = new ArrayList<>();
    for (int i = 0; i < ruleCount; ++i) {
      String curDataStorageName = baseDataStroageName + i;
      dataStorageNameList.add(curDataStorageName);
      String curDatasetName = baseDatasetName + i;
      RetentionRule retentionRule = createRetentionRule(
              i + 10, projectId, curDatasetName, "5:day", curDataStorageName, 2);
      datasetRules.add(retentionRule);
      prefixesList.add("gs://processPrefixesWithGoodPooledJob/ds" + i);
    }


    TransferJob transferJob = createTransferJob(transferJobName, scheduleAt);
    when(StsUtil.createStsJob(
            any(), eq(projectId), eq(bucketName), any(), eq(prefixesList), any(), any()))
            .thenReturn(transferJob);
    doNothing().when(objectUnderTest).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName), eq(prefixesList), any(), any());

    TransferJob job = objectUnderTest.processPrefixes(
            projectId, bucketName, datasetRules, prefixesList, zonedDateTime, null, false);

    verify(objectUnderTest, times(1)).sendInactiveDatasetNotification(
            eq(projectId), eq(bucketName), any(), any(), any());
    assertEquals(transferJob, job);
  }

  @Test
  public void chooseTransferJobsWithSingleJobAfterNow() {
    TimeOfDay curTime =
            new TimeOfDay().setHours(10).setMinutes(10).setSeconds(10).setNanos(10);
    TimeOfDay afterCurTime =
            new TimeOfDay().setHours(11).setMinutes(10).setSeconds(10).setNanos(10);
    TimeOfDay beforeCurTime =
            new TimeOfDay().setHours(9).setMinutes(10).setSeconds(10).setNanos(10);
    when(StsUtil.timeOfDayToString(any())).thenCallRealMethod();
    List<PooledStsJob> orderedJobList = new ArrayList<>();
    PooledStsJob beforeCurPooledStsJob = new PooledStsJob();
    beforeCurPooledStsJob.setSchedule(StsUtil.timeOfDayToString(beforeCurTime));
    orderedJobList.add(beforeCurPooledStsJob);
    PooledStsJob afterCurPooledStsJob = new PooledStsJob();
    afterCurPooledStsJob.setSchedule(StsUtil.timeOfDayToString(afterCurTime));
    orderedJobList.add(afterCurPooledStsJob);

    List<PooledStsJob> chosenJobs = objectUnderTest.chooseTransferJobs(
            orderedJobList, StsUtil.timeOfDayToString(curTime));
    assertEquals(1, chosenJobs.size());
    assertEquals(afterCurPooledStsJob, chosenJobs.get(0));
  }

  @Test
  public void chooseTransferJobsWithMultipleJobsAfterNow() {
    TimeOfDay curTime =
            new TimeOfDay().setHours(10).setMinutes(10).setSeconds(10).setNanos(10);
    TimeOfDay afterCurTime =
            new TimeOfDay().setHours(11).setMinutes(10).setSeconds(10).setNanos(10);
    TimeOfDay beforeCurTime =
            new TimeOfDay().setHours(9).setMinutes(10).setSeconds(10).setNanos(10);
    when(StsUtil.timeOfDayToString(any())).thenCallRealMethod();
    List<PooledStsJob> orderedJobList = new ArrayList<>();
    PooledStsJob beforeCurPooledStsJob = new PooledStsJob();
    beforeCurPooledStsJob.setSchedule(StsUtil.timeOfDayToString(beforeCurTime));
    orderedJobList.add(beforeCurPooledStsJob);
    PooledStsJob afterCurPooledStsJob1 = new PooledStsJob();
    afterCurPooledStsJob1.setSchedule(StsUtil.timeOfDayToString(afterCurTime));
    orderedJobList.add(afterCurPooledStsJob1);
    PooledStsJob afterCurPooledStsJob2 = new PooledStsJob();
    afterCurPooledStsJob2.setSchedule(StsUtil.timeOfDayToString(afterCurTime));
    orderedJobList.add(afterCurPooledStsJob2);

    List<PooledStsJob> chosenJobs = objectUnderTest.chooseTransferJobs(
            orderedJobList, StsUtil.timeOfDayToString(curTime));
    assertEquals(2, chosenJobs.size());
    assertEquals(afterCurPooledStsJob1, chosenJobs.get(0));
    assertEquals(afterCurPooledStsJob2, chosenJobs.get(1));
  }

  @Test
  public void chooseTransferJobsWithNoJobAfterNow() {
    TimeOfDay beforeCurTime1 =
            new TimeOfDay().setHours(9).setMinutes(10).setSeconds(10).setNanos(10);
    TimeOfDay beforeCurTime2 =
            new TimeOfDay().setHours(10).setMinutes(10).setSeconds(10).setNanos(10);
    TimeOfDay curTime =
            new TimeOfDay().setHours(11).setMinutes(10).setSeconds(10).setNanos(10);
    when(StsUtil.timeOfDayToString(any())).thenCallRealMethod();
    List<PooledStsJob> orderedJobList = new ArrayList<>();
    PooledStsJob beforeCurTimeJob1 = new PooledStsJob();
    beforeCurTimeJob1.setSchedule(StsUtil.timeOfDayToString(beforeCurTime1));
    orderedJobList.add(beforeCurTimeJob1);
    PooledStsJob beforeCurTimeJob2 = new PooledStsJob();
    beforeCurTimeJob2.setSchedule(StsUtil.timeOfDayToString(beforeCurTime2));
    orderedJobList.add(beforeCurTimeJob2);

    List<PooledStsJob> chosenJobs = objectUnderTest.chooseTransferJobs(
            orderedJobList, StsUtil.timeOfDayToString(curTime));
    assertEquals(1, chosenJobs.size());
    assertEquals(beforeCurTimeJob1, chosenJobs.get(0));
  }

  @Test
  public void chooseTransferJobsWithNullCandidate() {
    TimeOfDay curTime =
            new TimeOfDay().setHours(11).setMinutes(10).setSeconds(10).setNanos(10);
    when(StsUtil.timeOfDayToString(any())).thenCallRealMethod();
    List<PooledStsJob> orderedJobList = null;

    List<PooledStsJob> chosenJobs = objectUnderTest.chooseTransferJobs(
            orderedJobList, StsUtil.timeOfDayToString(curTime));
    assertEquals(0, chosenJobs.size());
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

  private TransferJob createTransferJob(String transferJobName, TimeOfDay scheduleAt) {
    TransferJob transferJob = new TransferJob();
    transferJob.setStatus(StsUtil.STS_ENABLED_STRING);
    TransferSpec transferSpec = new TransferSpec();
    transferJob.setName(transferJobName);
    transferJob.setTransferSpec(transferSpec);
    transferJob.setLastModificationTime(
        ZonedDateTime.now(Clock.systemUTC()).minusMinutes(10).toString());
    transferJob.setSchedule(
        new Schedule()
            .setStartTimeOfDay(scheduleAt));
    return transferJob;
  }

  private RetentionRule createRetentionRule(
          int id, String projectId, String datasetName,
          String retentionValue, String datasetStorageName, int version) {
    RetentionRule retentionRule = new RetentionRule();
    retentionRule.setId(id);
    retentionRule.setProjectId(projectId);
    retentionRule.setDatasetName(datasetName);
    retentionRule.setRetentionValue(retentionValue);
    retentionRule.setDataStorageName(datasetStorageName);
    retentionRule.setType(RetentionRuleType.DATASET);
    retentionRule.setVersion(version);
    return retentionRule;
  }
}
