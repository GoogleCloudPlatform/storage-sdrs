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

package com.google.gcs.sdrs.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.StoragetransferScopes;
import com.google.api.services.storagetransfer.v1.model.Date;
import com.google.api.services.storagetransfer.v1.model.GcsData;
import com.google.api.services.storagetransfer.v1.model.ObjectConditions;
import com.google.api.services.storagetransfer.v1.model.Operation;
import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.api.services.storagetransfer.v1.model.TransferOptions;
import com.google.api.services.storagetransfer.v1.model.TransferSpec;
import com.google.api.services.storagetransfer.v1.model.UpdateTransferJobRequest;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gson.Gson;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages the concrete integration with STS */
public class StsUtil {

  public static final String STS_ENABLED_STRING = "ENABLED";
  public static final String STS_DISABLED_STRING = "DISABLED";
  public static final String TRANSFER_OPERATION_STRING = "transferOperations";
  public static final String JOB_TYPE_STS = "STS";
  public static final String DEFAULT_SHADOW_BUCKET_EXTENSION = "shadow";
  public static final String DEFAULT_PROJECT_ID = "global-default";
  public static final String DEFAULT_MAX_PREFIX_COUNT = "1000";
  public static final String DEFAULT_LOOKBACK_IN_DAYS = "365";
  public static final String[] DEFAULT_LOG_CAT_BUCKET_PREFIX = {
    "2017/", "2018/", "2019/", "2020/", "2021"
  };

  public static final String DEFAULT_STS_JOB_POOL_NUMBER = "24";
  public static final int MAX_USER_STS_JOB_POOL_NUMBER = 96;
  public static final int MAX_DATASET_STS_JOB_POOL_NUMBER = 24;
  public static final String SHADOW_BUCKET_EXTENTION =
      SdrsApplication.getAppConfigProperty(
          "sts.shadowBucketExtension", DEFAULT_SHADOW_BUCKET_EXTENSION);
  public static final boolean IS_SHADOW_BUCKET_EXTENTION_PREFIX =
      Boolean.valueOf(
          SdrsApplication.getAppConfigProperty("sts.shadowBucketExtensionPrefix", "true"));

  public static final int MAX_PREFIX_COUNT =
      Integer.valueOf(
          SdrsApplication.getAppConfigProperty("sts.maxPrefixCount", DEFAULT_MAX_PREFIX_COUNT));

  public static final int STS_LOOKBACK_DAYS =
      Integer.valueOf(
          SdrsApplication.getAppConfigProperty("sts.maxLookBackInDays", DEFAULT_LOOKBACK_IN_DAYS));

  public static final boolean IS_STS_JOBPOOL_ONLY =
      Boolean.valueOf(SdrsApplication.getAppConfigProperty("sts.jobPoolOnly", "true"));

  public static final String SCHEDULE_TIME_DATE_TIME_FORMATTER = "HH:mm:ss";

  private static final Logger logger = LoggerFactory.getLogger(StsUtil.class);

  /** Creates an instance of the STS Client */
  public static Storagetransfer createStsClient(GoogleCredential credential) {
    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
    return createStorageTransferClient(httpTransport, jsonFactory, credential);
  }

  /**
   * Submits a job to be executed by STS
   *
   * @param client the {@link Storagetransfer} client to use
   * @param projectId the project ID of the target GCP project
   * @param sourceBucket the bucket from which you want to move
   * @param destinationBucket the destination bucket you want to move to
   * @param prefixes a {@link List} of all prefixes to include in the transfer job
   * @param description the description of the transfer job
   * @param startDateTime the {@link ZonedDateTime} when you want the job to start
   * @return the {@link TransferJob} object that is created
   * @throws IOException when the client or job cannot be created
   */
  public static TransferJob createStsJob(
      Storagetransfer client,
      String projectId,
      String sourceBucket,
      String destinationBucket,
      List<String> prefixes,
      String description,
      ZonedDateTime startDateTime)
      throws IOException {

    // Subtracting a day to force STS to trigger the job immediately
    TransferJob transferJob =
        buildTransferJob(
            projectId,
            sourceBucket,
            destinationBucket,
            prefixes,
            description,
            startDateTime,
            true,
            false,
            null);

    logger.info(
        String.format("Creating one time transfer job in STS: %s", transferJob.toPrettyString()));

    return client.transferJobs().create(transferJob).execute();
  }

  /**
   * Creates a recurring scheduled STS job
   *
   * @param client the {@link Storagetransfer} client to use
   * @param projectId the project ID of the target GCP project
   * @param sourceBucket the name of the bucket from which you want to move
   * @param destinationBucket the name of the bucket to which you want to move
   * @param prefixesToExclude a {@link List} of prefixes to exclude from the job
   * @param description the description of the job as it will appear in the STS console
   * @param startDateTime a {@link ZonedDateTime} of when you would like the job to begin
   * @param retentionInDays the period in days for which to retain records
   * @return the {@link TransferJob} object that is created
   * @throws IOException when the client or job cannot be created
   */
  public static TransferJob createDefaultStsJob(
      Storagetransfer client,
      String projectId,
      String sourceBucket,
      String destinationBucket,
      List<String> prefixesToExclude,
      String description,
      ZonedDateTime startDateTime,
      Integer retentionInDays)
      throws IOException {

    TransferJob transferJob =
        buildTransferJob(
            projectId,
            sourceBucket,
            destinationBucket,
            prefixesToExclude,
            description,
            startDateTime,
            false,
            true,
            retentionInDays);

    logger.info(
        String.format("Creating recurring transfer job in STS: %s", transferJob.toPrettyString()));

    return client.transferJobs().create(transferJob).execute();
  }

  /**
   * Updates an existing transfer job within STS
   *
   * @param client the {@link Storagetransfer} client to use
   * @param jobToUpdate the {@link TransferJob} object to submit as an update
   * @return the updated {@link TransferJob} object
   * @throws IOException when the client connection can't be established or the request fails
   */
  public static TransferJob updateExistingJob(
      Storagetransfer client, TransferJob jobToUpdate, String jobName, String projectId)
      throws IOException {

    UpdateTransferJobRequest requestBody = new UpdateTransferJobRequest();

    requestBody.setProjectId(projectId);
    requestBody.setTransferJob(jobToUpdate);

    Storagetransfer.TransferJobs.Patch request = client.transferJobs().patch(jobName, requestBody);

    logger.info(
        String.format(
            "Updating transfer job %s in STS: %s", jobName, jobToUpdate.toPrettyString()));

    return request.execute();
  }

  /**
   * Gets the record from STS for an existing job
   *
   * @param client the {@link Storagetransfer} client to use
   * @param projectId the project ID of the target GCP project
   * @param jobName the name of the transfer job to retrieve
   * @return the {@link TransferJob} object, if it exists
   * @throws IOException when the client can't establish a connection
   */
  public static TransferJob getExistingJob(Storagetransfer client, String projectId, String jobName)
      throws IOException {
    Storagetransfer.TransferJobs.Get request = client.transferJobs().get(jobName);
    request.setProjectId(projectId);

    return request.execute();
  }

  /**
   * Gets the information about submitted STS job operations
   *
   * @param client the {@link Storagetransfer} client to use for the request
   * @param projectId a {@link String} of the project ID to search
   * @param retentionJobs a {@link List} of jobs to retrieve
   * @return a {@link List} of {@link Operation} objects associated with the given jobs
   */
  public static List<Operation> getStsJobOperations(
      Storagetransfer client, String projectId, List<RetentionJob> retentionJobs) {

    List<Operation> operations = new ArrayList<>();
    Set<String> jobNames = new HashSet<>();

    for (RetentionJob job : retentionJobs) {

      try {
        if (jobNames.contains(job.getName())) {
          continue;
        } else {
          jobNames.add(job.getName());
        }
        List<String> jobNameList = new ArrayList<>();
        jobNameList.add(job.getName());
        // All STS jobs are daily run. Get the operations for the past 5 days.
        Storagetransfer.TransferOperations.List operationRequest =
            client
                .transferOperations()
                .list(TRANSFER_OPERATION_STRING)
                .setFilter(buildOperationFilterString(projectId, jobNameList))
                .setPageSize(5);

        List<Operation> operationsPerJob = operationRequest.execute().getOperations();
        Operation operationClosestToJobCreatedAtTime = null;
        Instant closestTime = Instant.MAX;
        if (operationsPerJob != null) {
          // loop through operations to find the one closest to when the
          // job is scheduled.
          for (Operation operation : operationsPerJob) {
            String opeationStartTimeString = operation.getMetadata().get("startTime").toString();
            Instant operationStartTime = Instant.parse(opeationStartTimeString);
            Instant retentionJobCreatedAtTime = job.getCreatedAt().toInstant();
            if (operationStartTime.isAfter(retentionJobCreatedAtTime)) {
              if (operationClosestToJobCreatedAtTime == null
                  || operationStartTime.isBefore(closestTime)) {
                operationClosestToJobCreatedAtTime = operation;
                closestTime = operationStartTime;
              }
            }
          }
        }

        if (operationClosestToJobCreatedAtTime != null) {
          operations.add(operationClosestToJobCreatedAtTime);
        }
      } catch (IOException ex) {
        logger.error("Could not establish connection with STS: ", ex.getMessage());
      }
    }

    return operations;
  }

  /**
   * Converts an int value to a Google Duration string
   *
   * @param retentionInDays the retention period in days
   * @return the Google Duration string
   */
  public static @NotNull String convertRetentionInDaysToDuration(int retentionInDays) {
    int ONE_DAY_IN_SECS = 3600 * 24;
    return (retentionInDays * ONE_DAY_IN_SECS) + "s";
  }

  public static TransferJob buildTransferJob(
      String projectId,
      String sourceBucket,
      String destinationBucket,
      List<String> prefixes,
      String description,
      ZonedDateTime startDateTime,
      Boolean isOneTimeSchedule,
      Boolean isExcludePrefixes,
      Integer retentionInDays) {
    return new TransferJob()
        .setProjectId(projectId)
        .setDescription(description)
        .setTransferSpec(
            buildTransferSpec(
                sourceBucket, destinationBucket, prefixes, isExcludePrefixes, retentionInDays))
        .setSchedule(buildSchedule(startDateTime, isOneTimeSchedule))
        .setStatus(STS_ENABLED_STRING);
  }

  public static TransferSpec buildTransferSpec(
      String sourceBucket,
      String destinationBucket,
      List<String> prefixes,
      Boolean isExcludePrefixes,
      Integer retentionInDays) {
    return new TransferSpec()
        .setGcsDataSource(new GcsData().setBucketName(sourceBucket))
        .setGcsDataSink(new GcsData().setBucketName(destinationBucket))
        .setObjectConditions(buildObjectConditions(prefixes, isExcludePrefixes, retentionInDays))
        .setTransferOptions(
            new TransferOptions()
                // flip the delete flag to false if you want to test without actual deleting  your
                // data
                .setDeleteObjectsFromSourceAfterTransfer(true)
                .setOverwriteObjectsAlreadyExistingInSink(true));
  }

  public static ObjectConditions buildObjectConditions(
      List<String> prefixes, Boolean isExcludePrefixes, Integer retentionInDays) {

    ObjectConditions objectConditions = new ObjectConditions();

    if (retentionInDays != null) {
      objectConditions.setMinTimeElapsedSinceLastModification(
          convertRetentionInDaysToDuration(retentionInDays));
    }

    if (isExcludePrefixes) {
      objectConditions.setExcludePrefixes(prefixes);
    } else {
      objectConditions.setIncludePrefixes(prefixes);
    }

    return objectConditions;
  }

  public static Schedule buildSchedule(ZonedDateTime startDateTime, Boolean isOneTimeSchedule) {

    // Subtracting a day to force STS to trigger the job immediately
    Date date = convertToDate(startDateTime.toLocalDate().minusDays(1));

    Schedule schedule = new Schedule().setScheduleStartDate(date);

    if (isOneTimeSchedule) {
      schedule.setScheduleEndDate(date);
    }

    return schedule;
  }

  private static String buildOperationFilterString(String projectId, List<String> jobNames) {
    return String.format(
        "{\"project_id\": \"%s\", \"job_names\": %s}", projectId, new Gson().toJson(jobNames));
  }

  private static Storagetransfer createStorageTransferClient(
      HttpTransport httpTransport, JsonFactory jsonFactory, GoogleCredential credential) {

    Preconditions.checkNotNull(httpTransport);
    Preconditions.checkNotNull(jsonFactory);
    Preconditions.checkNotNull(credential);

    // In some cases, you need to add the scope explicitly.
    if (credential.createScopedRequired()) {
      Set<String> scopes = new HashSet<>();
      scopes.addAll(StorageScopes.all());
      scopes.addAll(StoragetransferScopes.all());
      credential = credential.createScoped(scopes);
    }

    HttpRequestInitializer initializer = new RetryHttpInitializerWrapper(credential, true);
    return new Storagetransfer.Builder(httpTransport, jsonFactory, initializer)
        .setApplicationName("sdrs")
        .build();
  }

  public static @NotNull Date convertToDate(LocalDate startDate) {
    Date googleDate = new Date();
    googleDate.setYear(startDate.getYear());
    googleDate.setMonth(startDate.getMonthValue());
    googleDate.setDay(startDate.getDayOfMonth());

    return googleDate;
  }

  public static @NotNull TimeOfDay convertToTimeOfDay(LocalTime startTime) {
    TimeOfDay timeOfDay = new TimeOfDay();
    timeOfDay.setHours(startTime.getHour());
    timeOfDay.setMinutes(startTime.getMinute());
    timeOfDay.setSeconds(startTime.getSecond());

    return timeOfDay;
  }

  public static String timeOfDayToString(TimeOfDay timeOfDay) {
    if (timeOfDay == null) {
      return "00:00:00";
    }
    int hours = timeOfDay.getHours() != null ? timeOfDay.getHours() : 0;
    int minutes = timeOfDay.getMinutes() != null ? timeOfDay.getMinutes() : 0;
    int seconds = timeOfDay.getSeconds() != null ? timeOfDay.getSeconds() : 0;
    return String.format("%02d:%02d:%02d", hours, minutes, seconds);
  }

  public static LocalTime convertToLocalTime(TimeOfDay timeOfDay) {
    int hours = timeOfDay.getHours() != null ? timeOfDay.getHours() : 0;
    int minutes = timeOfDay.getMinutes() != null ? timeOfDay.getMinutes() : 0;
    int seconds = timeOfDay.getSeconds() != null ? timeOfDay.getSeconds() : 0;
    return LocalTime.of(hours, minutes, seconds);
  }

  public static String convertPrefixToString(List<String> prefixes) {
    if (prefixes == null || prefixes.isEmpty()) {
      return null;
    }

    return prefixes.stream().reduce((a, b) -> a + ";" + b).get();
  }

  public static String buildDestinationBucketName(
      String sourceBucketName, String appended, boolean isPrefix) {
    return isPrefix ? appended + sourceBucketName : sourceBucketName + appended;
  }

  public static String buildDestinationBucketName(String sourceBucketName) {
    return buildDestinationBucketName(
        sourceBucketName, SHADOW_BUCKET_EXTENTION, IS_SHADOW_BUCKET_EXTENTION_PREFIX);
  }

  public static boolean isJobPoolOndemandCreation(RetentionRuleType retentionRuleType) {
    return SdrsApplication.getAppConfigProperty(
            "sts.jobPoolOnDemand." + retentionRuleType.toString().toLowerCase())
                    != null
                    ? true
                    : false;
  }
}
