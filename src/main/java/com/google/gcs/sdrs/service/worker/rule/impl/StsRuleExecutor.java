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
 *
 */

package com.google.gcs.sdrs.service.worker.rule.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.model.GcsData;
import com.google.api.services.storagetransfer.v1.model.ObjectConditions;
import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.api.services.storagetransfer.v1.model.TransferOptions;
import com.google.api.services.storagetransfer.v1.model.TransferSpec;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.common.RetentionValue;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.dao.DmQueueDao;
import com.google.gcs.sdrs.dao.PooledStsJobDao;
import com.google.gcs.sdrs.dao.RetentionJobDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.PooledStsJob;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import com.google.gcs.sdrs.service.mq.PubSubMessageQueueManagerImpl;
import com.google.gcs.sdrs.service.mq.pojo.InactiveDatasetMessage;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.rule.RuleExecutor;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.PrefixGeneratorUtility;
import com.google.gcs.sdrs.util.RetentionUtil;
import com.google.gcs.sdrs.util.StsUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An implementation of the Rule Executor interface that uses STS */
public class StsRuleExecutor implements RuleExecutor {

  private static StsRuleExecutor instance;
  private Storagetransfer client;
  private RetentionJobDao retentionJobDao;
  private PooledStsJobDao stsJobDao;
  private DmQueueDao dmQueueDao;

  private static final Logger logger = LoggerFactory.getLogger(StsRuleExecutor.class);

  public static StsRuleExecutor getInstance() {
    if (instance == null) {
      try {
        instance = new StsRuleExecutor();
      } catch (IOException ex) {
        logger.error("Could not establish connection with STS: ", ex);
      }
    }

    return instance;
  }

  /**
   * STS Rule Executor constructor that reads the bucket suffix from the configuration file
   *
   * @throws IOException when the STS Client cannot be instantiated
   */
  private StsRuleExecutor() throws IOException {
    GoogleCredential credentials = CredentialsUtil.getInstance().getCredentials();
    client = StsUtil.createStsClient(credentials);
    retentionJobDao = SingletonDao.getRetentionJobDao();
    stsJobDao = SingletonDao.getPooledStsJobDao();
    dmQueueDao = SingletonDao.getDmQueueDao();
  }

  /**
   * Apply user commanded on-demand retention.
   *
   * @param userCommandedRules A list of user commanded retention rules.
   * @param projectId GCP project ID
   * @throws IOException when STS api call fails or other errors
   */
  @Override
  public List<DmRequest> executeUserCommandedRule(
      Collection<RetentionRule> userCommandedRules, String projectId) {
    List<DmRequest> dmRequests = new ArrayList<>();
    // get all rules for a bucket
    Map<String, List<RetentionRule>> bucketRuleMap = buildBucketRuleMap(userCommandedRules);
    String correlationId = getCorrelationId();
    ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(Clock.systemUTC());

    for (String bucketName : bucketRuleMap.keySet()) {
      List<String> prefixes = new ArrayList<>();

      // create prefixes from all user commanded rules for a bucket
      for (RetentionRule userCommandedRule : bucketRuleMap.get(bucketName)) {

        if (userCommandedRule.getType() != RetentionRuleType.USER) {
          logger.warn("Rule type is not user commanded retention.");
          continue;
        }
        String prefix = RetentionUtil.getDatasetPath(userCommandedRule.getDataStorageName());
        if (prefix.isEmpty()) {
          String message =
              String.format(
                  "The target %s is the root of a bucket. Can not delete a bucket",
                  userCommandedRule.getDataStorageName());
          logger.error(message);
        } else {
          // create dm request
          DmRequest dmRequest = new DmRequest();
          dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_PENDING);
          dmRequest.setDataStorageName(userCommandedRule.getDataStorageName());
          String dataStroageRoot = userCommandedRule.getDataStorageRoot();
          if (dataStroageRoot == null) {
            dataStroageRoot = RetentionUtil.getBucketName(userCommandedRule.getDataStorageName());
          }
          dmRequest.setDataStorageRoot(dataStroageRoot);
          dmRequest.setProjectId(userCommandedRule.getProjectId());
          dmRequests.add(dmRequest);
          prefixes.add(prefix);
        }
      }

      if (prefixes.isEmpty()) {
        continue;
      }

      sendInactiveDatasetNotification(
          projectId, bucketName, prefixes, zonedDateTimeNow.toInstant(), correlationId);
    }

    dmQueueDao.saveOrUpdateBatch(dmRequests);
    return dmRequests;
  }

  /**
   * Executes dataset retention rules
   *
   * @param datasetRules a list of dataset retention rules
   * @param projectId the project that the datasets belong to
   * @throws IOException when STS api call fails or other errors
   */
  @Override
  public List<RetentionJob> executeDatasetRule(
      Collection<RetentionRule> datasetRules, String projectId) {
    List<RetentionJob> datasetRuleJobs = new ArrayList<>();
    // get all dataset rules for a bucket
    Map<String, List<RetentionRule>> bucketDatasetMap = buildBucketRuleMap(datasetRules);
    String correlationId = getCorrelationId();
    ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(Clock.systemUTC());
    String scheduleTimeOfDay = zonedDateTimeNow.format(DateTimeFormatter.ofPattern("HH:mm:ss"));

    for (String bucketName : bucketDatasetMap.keySet()) {

      List<String> prefixes = new ArrayList<>();
      Map<String, List<String>> prefixesPerDatasetMap = new HashMap<>();

      // create prefixes from all dataset rules for a bucket
      for (RetentionRule datasetRule : bucketDatasetMap.get(bucketName)) {
        if (datasetRule.getType() != RetentionRuleType.DATASET) {
          logger.warn("Rule type is not dataset.");
          continue;
        }

        List<String> tmpPrefixes =
            PrefixGeneratorUtility.generateTimePrefixes(
                RetentionUtil.getDatasetPath(datasetRule.getDataStorageName()),
                zonedDateTimeNow.minusDays(StsUtil.STS_LOOKBACK_DAYS),
                zonedDateTimeNow.minusDays(
                    RetentionValue.convertValue(
                        RetentionValue.parse(datasetRule.getRetentionValue()))));
        prefixesPerDatasetMap.put(datasetRule.getDataStorageName(), tmpPrefixes);
        prefixes.addAll(tmpPrefixes);
      }

      sendInactiveDatasetNotification(
          projectId, bucketName, prefixes, zonedDateTimeNow.toInstant(), correlationId);

      String sourceBucket = bucketName;
      String destinationBucket = StsUtil.buildDestinationBucketName(bucketName);
      String description =
          buildDescription(
              RetentionRuleType.DATASET.toString(),
              bucketDatasetMap.get(bucketName),
              scheduleTimeOfDay);

      logger.info(
          String.format(
              "Scheduling dataset STS job with projectId: %s, "
                  + "description: %s, source: %s, destination: %s",
              projectId, description, sourceBucket, destinationBucket));

      TransferJob job = null;
      try {
        TransferJob stsPooledJob =
            findPooledJob(projectId, bucketName, scheduleTimeOfDay, RetentionRuleType.DATASET);
        if (stsPooledJob == null) {
          if (!StsUtil.IS_STS_JOBPOOL_ONLY) {
            job =
                StsUtil.createStsJob(
                    client,
                    projectId,
                    sourceBucket,
                    destinationBucket,
                    prefixes,
                    description,
                    zonedDateTimeNow);
          }
        } else {
          TransferJob jobToUpdate = new TransferJob();
          jobToUpdate
              .setDescription(description)
              .setTransferSpec(
                  StsUtil.buildTransferSpec(sourceBucket, destinationBucket, prefixes, false, null))
              .setStatus(StsUtil.STS_ENABLED_STRING);
          job = StsUtil.updateExistingJob(client, jobToUpdate, stsPooledJob.getName(), projectId);
        }
      } catch (IOException e) {
        logger.error(
            String.format(
                "Failed to schedule dataset STS job for %s/%s. %s",
                projectId, sourceBucket, e.getMessage()),
            e);
      }

      String jobName = null;
      Timestamp createdAt = null;
      if (job != null) {
        jobName = job.getName();
        createdAt = new Timestamp(Instant.parse(job.getLastModificationTime()).toEpochMilli());
      }

      for (RetentionRule datasetRule : bucketDatasetMap.get(bucketName)) {
        datasetRuleJobs.add(
            buildRetentionJobEntity(
                jobName,
                datasetRule,
                StsUtil.convertPrefixToString(
                    prefixesPerDatasetMap.get(datasetRule.getDataStorageName())),
                createdAt));
      }
    }

    return datasetRuleJobs;
  }

  /**
   * Apply default retention. Default rule is set
   *
   * @param globalDefaultRule Global default rule
   * @param defaultRules A list of default rules
   * @param datasetRules A list of dataset rules
   * @param scheduledTime The time at which the STS job for default rule is schedule to run daily
   * @param projectId GCP project ID
   * @throws IOException when STS api call fails or other errors
   */
  @Override
  public List<RetentionJob> executeDefaultRule(
      RetentionRule globalDefaultRule,
      Collection<RetentionRule> defaultRules,
      Collection<RetentionRule> datasetRules,
      ZonedDateTime scheduledTime,
      String projectId) {
    List<RetentionJob> defaultRuleJobs = new ArrayList<>();
    Map<String, Set<String>> prefixesToExcludeMap = buildDefaultStsJobPrefixMap(datasetRules);
    Map<String, RetentionRule> defaultRuleMap = buildDefaultRuleMap(defaultRules);

    Set<String> bucketsToProcess = new HashSet<>();
    bucketsToProcess.addAll(prefixesToExcludeMap.keySet());
    bucketsToProcess.addAll(defaultRuleMap.keySet());

    for (String bucketName : bucketsToProcess) {
      String fullSourceBucket = ValidationConstants.STORAGE_PREFIX + bucketName;
      String destinationBucket = StsUtil.buildDestinationBucketName(bucketName);
      List<String> prefixesToExclude = buildPrefixesToExclude(prefixesToExcludeMap, bucketName);

      RetentionRule defaultRule = defaultRuleMap.get(bucketName);

      if (defaultRule == null) {
        // use global default rule if a default rule is not set on a bucket.
        defaultRule = globalDefaultRule;
      }

      if (defaultRule != null) {
        TransferJob transferJob =
            applyDefaultRulePerBucket(
                defaultRule,
                scheduledTime,
                projectId,
                bucketName,
                destinationBucket,
                prefixesToExclude);

        String jobName = null;
        if (transferJob != null) {
          jobName = transferJob.getName();
        }

        if (defaultRule.getType() == RetentionRuleType.GLOBAL) {
          // Save the job with the actual projectId it is being created for, not the fake global
          // projectId that is set on the global default rule. Same for data storage.
          defaultRule.setProjectId(projectId);
          defaultRule.setDataStorageName(fullSourceBucket);
        }
        RetentionJob defaultRetentionJob =
            buildRetentionJobEntity(
                jobName, defaultRule, StsUtil.convertPrefixToString(prefixesToExclude), null);
        defaultRuleJobs.add(defaultRetentionJob);
      }
    }

    return defaultRuleJobs;
  }

  private List<String> buildPrefixesToExclude(
      final Map<String, Set<String>> prefixesToExcludeMap, String bucketName) {
    List<String> prefixesToExclude = new ArrayList<>();
    String predDefinedList =
        SdrsApplication.getAppConfigProperty("sts.defaultRuleExlcudePrefixList");
    if (predDefinedList != null && !predDefinedList.isEmpty()) {
      prefixesToExclude.addAll(Arrays.asList(predDefinedList.split(";")));
    }
    if (prefixesToExcludeMap.containsKey(bucketName)) {
      prefixesToExclude.addAll(prefixesToExcludeMap.get(bucketName));
      if (prefixesToExclude.isEmpty()) {
        // we have bucket that has one implied dataset. i.e 2018/, 2019, 2020/ are at the root of
        // the bucket
        prefixesToExclude.addAll(Arrays.asList(StsUtil.DEFAULT_LOG_CAT_BUCKET_PREFIX));
      }
    } else if (predDefinedList == null || predDefinedList.isEmpty()) {
      // we have to add a "fake" no-op exclude prefix. otherwise STS throws an error.
      prefixesToExclude.add(UUID.randomUUID().toString() + "_NOOP_EXCLUDE_PREFIX/");
    }

    return prefixesToExclude;
  }

  private TransferJob updateDefaultJobIfNeeded(
      TransferJob existingTransferJob,
      RetentionRule defaultRule,
      List<String> prefixesToExclude,
      String description)
      throws IOException {

    if (existingTransferJob == null) {
      return null;
    }
    TransferSpec transferSpec = existingTransferJob.getTransferSpec();
    ObjectConditions objectConditions = transferSpec.getObjectConditions();
    if (objectConditions == null) {
      objectConditions = new ObjectConditions();
      transferSpec.setObjectConditions(objectConditions);
    }

    // make sure transfer options are set properly
    transferSpec.setTransferOptions(
        new TransferOptions()
            .setDeleteObjectsFromSourceAfterTransfer(true)
            .setOverwriteObjectsAlreadyExistingInSink(true));

    boolean retentionPeriodChanged = false;
    boolean prefixesToExcludeChanged = false;

    TransferJob updatedJob = existingTransferJob;

    // Check if retention period changed
    String existingRetention = objectConditions.getMinTimeElapsedSinceLastModification();
    String updatedRetention =
        StsUtil.convertRetentionInDaysToDuration(
            RetentionValue.convertValue(RetentionValue.parse(defaultRule.getRetentionValue())));

    if (existingRetention == null || !existingRetention.equals(updatedRetention)) {
      objectConditions.setMinTimeElapsedSinceLastModification(updatedRetention);
      retentionPeriodChanged = true;
    }

    // check if prefixes to exclude changed
    List<String> existingExcludePrefixList = objectConditions.getExcludePrefixes();
    List<String> updatedPrefixesToExclude = prefixesToExclude;

    if (!isSamePrefixList(existingExcludePrefixList, updatedPrefixesToExclude)) {
      objectConditions.setExcludePrefixes(updatedPrefixesToExclude);
      prefixesToExcludeChanged = true;
    }

    // only update if the retention period or prefix list has changed
    if (retentionPeriodChanged || prefixesToExcludeChanged) {
      // Build transfer job object
      updatedJob = new TransferJob();
      updatedJob.setName(existingTransferJob.getName());
      updatedJob.setDescription(description);
      updatedJob.setTransferSpec(transferSpec);
      updatedJob.setStatus("ENABLED");
      StsUtil.updateExistingJob(
          client, updatedJob, existingTransferJob.getName(), existingTransferJob.getProjectId());
    }
    return updatedJob;
  }

  private TransferJob applyDefaultRulePerBucket(
      RetentionRule defaultRule,
      ZonedDateTime scheduledTime,
      String projectId,
      String sourceBucket,
      String destinationBucket,
      List<String> prefixesToExclude) {
    String description =
        buildDescription(
            defaultRule.getType().toString(),
            Arrays.asList(new RetentionRule[] {defaultRule}),
            null);
    TransferJob transferJob = null;

    TransferJob stsPooledJob = null;

    logger.info(
        String.format(
            "Scheduling default STS job for rule %s, projectId: %s, "
                + "description: %s, source: %s, destination: %s",
            defaultRule.getId(), projectId, description, sourceBucket, destinationBucket));

    try {
      stsPooledJob = findPooledJob(projectId, sourceBucket, null, RetentionRuleType.DEFAULT);
      RetentionJob existingDefaultRetentionJob =
          retentionJobDao.findLatestDefaultJob(ValidationConstants.STORAGE_PREFIX + sourceBucket);

      if (stsPooledJob == null && existingDefaultRetentionJob == null) {
        if (!StsUtil.IS_STS_JOBPOOL_ONLY) {
          transferJob =
              StsUtil.createDefaultStsJob(
                  client,
                  projectId,
                  sourceBucket,
                  destinationBucket,
                  prefixesToExclude,
                  description,
                  scheduledTime,
                  RetentionValue.convertValue(
                      RetentionValue.parse(defaultRule.getRetentionValue())));
        }
      } else {
        if (stsPooledJob == null && existingDefaultRetentionJob != null) {
          stsPooledJob =
              StsUtil.getExistingJob(
                  client,
                  existingDefaultRetentionJob.getRetentionRuleProjectId(),
                  existingDefaultRetentionJob.getName());
          if (stsPooledJob == null
              || !stsPooledJob.getStatus().equals(StsUtil.STS_ENABLED_STRING)) {
            return null;
          }
        }
        transferJob =
            updateDefaultJobIfNeeded(stsPooledJob, defaultRule, prefixesToExclude, description);
      }
    } catch (IOException e) {
      logger.error(
          String.format(
              "Failed to schedule default STS job for %s/%s. %s",
              projectId, sourceBucket, e.getMessage()),
          e);
      return null;
    }

    return transferJob;
  }

  public static String buildDescription(
      String type, List<RetentionRule> rules, @Nullable String details) {
    StringBuilder sb = new StringBuilder();
    if (rules != null) {
      for (RetentionRule rule : rules) {
        sb.append(rule.getId() + ":" + rule.getVersion() + "|");
      }
      sb.deleteCharAt(sb.length() - 1);
    }

    String moreDetails = details == null ? "" : details;
    return String.format("%s %s %s", type, sb.toString(), moreDetails);
  }

  private boolean isSamePrefixList(List<String> oldList, List<String> newList) {
    if (oldList == null && newList == null) {
      return true;
    }

    if (oldList == null || newList == null) {
      return false;
    }

    if (oldList.size() != newList.size()) {
      return false;
    }

    Collections.sort(oldList);
    Collections.sort(newList);

    if (oldList.equals(newList)) {
      return true;
    }

    return false;
  }

  private Map<String, Set<String>> buildDefaultStsJobPrefixMap(
      final Collection<RetentionRule> datasetRules) {
    Map<String, Set<String>> prefixMap = new HashMap<>();
    for (RetentionRule datasetRule : datasetRules) {
      String bucketName = RetentionUtil.getBucketName(datasetRule.getDataStorageName());

      String datasetPath = RetentionUtil.getDatasetPath(datasetRule.getDataStorageName());
      if (prefixMap.containsKey(bucketName)) {
        if (datasetPath != null && !datasetPath.isEmpty()) {
          prefixMap.get(bucketName).add(datasetPath + "/");
        }
      } else {
        Set<String> s = new HashSet<>();
        if (datasetPath != null && !datasetPath.isEmpty()) {
          s.add(datasetPath + "/");
        }
        prefixMap.put(bucketName, s);
      }
    }
    return prefixMap;
  }

  private Map<String, RetentionRule> buildDefaultRuleMap(
      final Collection<RetentionRule> defaultRules) {
    Map<String, RetentionRule> defaultRuleMap = new HashMap<>();
    for (RetentionRule defaultRule : defaultRules) {
      if (defaultRule.getType() == RetentionRuleType.DEFAULT) {
        defaultRuleMap.put(
            RetentionUtil.getBucketName(defaultRule.getDataStorageName()), defaultRule);
      }
    }
    return defaultRuleMap;
  }

  private Map<String, List<RetentionRule>> buildBucketRuleMap(
      Collection<RetentionRule> datasetRules) {
    Map<String, List<RetentionRule>> bucketDatasetMap = new HashMap<>();
    for (RetentionRule rule : datasetRules) {
      String key = RetentionUtil.getBucketName(rule.getDataStorageName());

      if (bucketDatasetMap.containsKey(key)) {
        bucketDatasetMap.get(key).add(rule);
      } else {
        List<RetentionRule> retentionRules = new ArrayList<>();
        retentionRules.add(rule);
        bucketDatasetMap.put(key, retentionRules);
      }
    }

    return bucketDatasetMap;
  }

  public static RetentionJob buildRetentionJobEntity(
      String jobName,
      RetentionRule rule,
      @Nullable String metadata,
      @Nullable Timestamp createdAt) {
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName(jobName);
    retentionJob.setRetentionRuleId(rule.getId());
    retentionJob.setRetentionRuleProjectId(rule.getProjectId());
    retentionJob.setRetentionRuleDataStorageName(rule.getDataStorageName());
    retentionJob.setRetentionRuleType(rule.getType());
    retentionJob.setRetentionRuleVersion(rule.getVersion());
    retentionJob.setType(StsUtil.JOB_TYPE_STS);
    retentionJob.setDataStorageRoot(RetentionUtil.getBucketName(rule.getDataStorageName()));
    retentionJob.setMetadata(metadata);
    retentionJob.setCreatedAt(createdAt);

    return retentionJob;
  }

  public static List<TimeOfDay> generateScheduleTimeOfDay(RetentionRuleType retentionRuleType) {
    int numberOfJobs = 0;
    List<TimeOfDay> result = new ArrayList<>();
    LocalTime now = LocalTime.now();
    LocalTime timeOfDay = LocalTime.of(0, now.getMinute(), now.getSecond());

    if (retentionRuleType == null) {
      return result;
    }

    switch (retentionRuleType) {
      case USER:
        numberOfJobs =
            Integer.parseInt(
                SdrsApplication.getAppConfigProperty(
                    "sts.jobPoolOnDemand." + RetentionRuleType.USER.toString().toLowerCase(),
                    StsUtil.DEFAULT_STS_JOB_POOL_NUMBER));
        numberOfJobs = Math.min(numberOfJobs, StsUtil.MAX_USER_STS_JOB_POOL_NUMBER);
        break;
      case DATASET:
        numberOfJobs =
            Integer.parseInt(
                SdrsApplication.getAppConfigProperty(
                    "sts.jobPoolOnDemand." + RetentionRuleType.DATASET.toString().toLowerCase(),
                    StsUtil.DEFAULT_STS_JOB_POOL_NUMBER));
        numberOfJobs = Math.min(numberOfJobs, StsUtil.MAX_DATASET_STS_JOB_POOL_NUMBER);
        break;
      case DEFAULT:
        numberOfJobs = 1;
        timeOfDay = LocalTime.of(23, 59, 59);
        break;
      default:
        // shouldn't happen
        return result;
    }

    result.add(StsUtil.convertToTimeOfDay(timeOfDay));
    // incremental in minutes within 24 hours.
    int increment = 24 * 60 / numberOfJobs;
    for (int i = 1; i < numberOfJobs; i++) {
      timeOfDay = timeOfDay.plusMinutes(increment);
      result.add(StsUtil.convertToTimeOfDay(timeOfDay));
    }

    return result;
  }

  private List<TransferJob> createJobPool(
      String projectId,
      String sourceBucket,
      String destinationBucket,
      RetentionRuleType retentionRuleType) {

    List<TransferJob> transferJobList = new ArrayList<>();
    List<TimeOfDay> timeOfDayList = generateScheduleTimeOfDay(retentionRuleType);
    for (int i = 0; i < timeOfDayList.size(); i++) {
      String description =
          String.format(
              "Pooled STS Job %d  %s %s",
              i, retentionRuleType.toString(), StsUtil.timeOfDayToString(timeOfDayList.get(i)));

      TransferSpec transferSpec =
          new TransferSpec()
              .setGcsDataSource(new GcsData().setBucketName(sourceBucket))
              .setGcsDataSink(new GcsData().setBucketName(destinationBucket))
              .setTransferOptions(
                  new TransferOptions()
                      .setDeleteObjectsFromSourceAfterTransfer(true)
                      .setOverwriteObjectsAlreadyExistingInSink(true));

      Schedule schedule =
          new Schedule()
              .setScheduleStartDate(StsUtil.convertToDate(LocalDate.now().minusDays(1)))
              .setStartTimeOfDay(timeOfDayList.get(i));

      TransferJob transferJob =
          new TransferJob()
              .setProjectId(projectId)
              .setDescription(description)
              .setTransferSpec(transferSpec)
              .setSchedule(schedule)
              .setStatus(StsUtil.STS_DISABLED_STRING);

      try {
        logger.info(
            String.format(
                "Creating %s STS job for job pool: %s ",
                retentionRuleType.toString(), transferJob.toPrettyString()));
        transferJobList.add(client.transferJobs().create(transferJob).execute());
      } catch (IOException e) {
        logger.error(
            String.format(
                "Failed to create %s STS job for %s/%s",
                retentionRuleType.toString(), projectId, sourceBucket),
            e);
        return null;
      }
    }
    return transferJobList;
  }

  private PooledStsJob saveJobPoolAndGetNextJob(
      List<TransferJob> transferJobList, String currentTime, RetentionRuleType retentionRuleType) {
    PooledStsJob nextAvailableJob = null;

    LocalTime targetStartTimeOfDay =
        LocalTime.parse(currentTime, DateTimeFormatter.ofPattern("HH:mm:ss"));

    if (transferJobList != null && !transferJobList.isEmpty()) {
      List<PooledStsJob> pooledStsJobList = new ArrayList<>();
      // the list is sorted by timeOfDay in asc order
      for (int i = 0; i < transferJobList.size(); i++) {
        TransferJob transferJob = transferJobList.get(i);
        TimeOfDay timeOfDay = transferJob.getSchedule().getStartTimeOfDay();
        PooledStsJob pooledStsJob = new PooledStsJob();
        pooledStsJob.setName(transferJob.getName());
        pooledStsJob.setProjectId(transferJob.getProjectId());
        pooledStsJob.setType(retentionRuleType.toDatabaseRepresentation());
        pooledStsJob.setSchedule(StsUtil.timeOfDayToString(timeOfDay));
        pooledStsJob.setSourceBucket(
            transferJob.getTransferSpec().getGcsDataSource().getBucketName());
        pooledStsJob.setSourceProject(transferJob.getProjectId());
        pooledStsJob.setStatus(transferJob.getStatus());
        pooledStsJob.setTargetBucket(
            transferJob.getTransferSpec().getGcsDataSink().getBucketName());
        pooledStsJob.setTargetProject(transferJob.getProjectId());

        if (StsUtil.convertToLocalTime(timeOfDay).isAfter(targetStartTimeOfDay)
            && nextAvailableJob == null) {
          // we found the job at the right scheduled time of day. it won't come here
          // after setting nextAvailableJob
          nextAvailableJob = pooledStsJob;
        }

        pooledStsJobList.add(pooledStsJob);
      }

      stsJobDao.saveOrUpdateBatch(pooledStsJobList);
      if (nextAvailableJob == null) {
        nextAvailableJob = pooledStsJobList.get(0);
      }
    }
    return nextAvailableJob;
  }

  public TransferJob findPooledJob(
      String projectId,
      String bucketName,
      @Nullable String scheduledAt,
      RetentionRuleType retentionRuleType)
      throws IOException {
    PooledStsJob pooledJob =
        stsJobDao.getJob(
            bucketName, projectId, scheduledAt, retentionRuleType.toDatabaseRepresentation());
    boolean isOnDemandPoolCreation =
        SdrsApplication.getAppConfigProperty(
                    "sts.jobPoolOnDemand." + retentionRuleType.toString().toLowerCase())
                != null
            ? true
            : false;
    if (pooledJob == null && isOnDemandPoolCreation) {
      // create STS job pool
      String destinationBucket = StsUtil.buildDestinationBucketName(bucketName);
      List<TransferJob> transferJobList =
          createJobPool(projectId, bucketName, destinationBucket, retentionRuleType);
      pooledJob = saveJobPoolAndGetNextJob(transferJobList, scheduledAt, retentionRuleType);
    }
    String jobName = null;
    if (pooledJob != null) {
      jobName = pooledJob.getName();

      logger.info(
          String.format(
              "STS job found from the pool to run at %s for %s/%s",
              pooledJob.getSchedule(), projectId, bucketName));
    } else {
      logger.error(String.format("No pooled STS job found for %s/%s", projectId, bucketName));
      return null;
    }

    if (scheduledAt != null) {
      scheduledAt = pooledJob.getSchedule();
    }

    TransferJob transferJob = StsUtil.getExistingJob(client, projectId, jobName);
    if (!isValidPooledJob(transferJob, jobName, projectId, bucketName, scheduledAt)) {
      logger.error(
          String.format(
              "Pooled job %s scheduled at %s for %s/%s is not valid",
              jobName, scheduledAt, projectId, bucketName));
      return null;
    }

    return transferJob;
  }

  private boolean isValidPooledJob(
      TransferJob pooledJob,
      String jobName,
      String projectId,
      String bucketName,
      String scheduledAt) {
    if (pooledJob == null) {
      return false;
    }

    boolean result =
        pooledJob.getName().equals(jobName)
            && pooledJob.getProjectId().equals(projectId)
            && pooledJob.getSchedule().getScheduleEndDate() == null
            && (scheduledAt == null || isScheduledAtSame(pooledJob, scheduledAt))
            && pooledJob.getTransferSpec().getGcsDataSource().getBucketName().equals(bucketName);

    return result;
  }

  private boolean isScheduledAtSame(TransferJob pooledJob, String scheduledAt) {
    if (pooledJob == null || scheduledAt == null) {
      return false;
    }

    return scheduledAt.equals(
        StsUtil.timeOfDayToString(pooledJob.getSchedule().getStartTimeOfDay()));
  }

  private void sendInactiveDatasetNotification(
      String projectId,
      String bucket,
      List<String> prefixList,
      Instant inactiveAt,
      String correlationId) {

    for (String prefix : prefixList) {
      InactiveDatasetMessage msg = new InactiveDatasetMessage();
      msg.setCorrelationId(correlationId);
      msg.setInactiveAt(inactiveAt);
      msg.setProjectId(projectId);
      msg.setTrigger(correlationId);
      msg.setDeletedDirectoryUri(
          ValidationConstants.STORAGE_PREFIX
              + bucket
              + ValidationConstants.STORAGE_SEPARATOR
              + prefix);
      try {
        PubSubMessageQueueManagerImpl.getInstance().sendInactiveDatasetMessage(msg);
      } catch (IOException e) {
        logger.error(String.format("Error sending delete notification. %s", e.getMessage()), e);
      }
    }
  }

  private String getCorrelationId() {
    String correlationId = BaseWorker.getCorrelationId();
    if (correlationId == null) {
      correlationId = UUID.randomUUID().toString();
    }
    return correlationId;
  }
}
