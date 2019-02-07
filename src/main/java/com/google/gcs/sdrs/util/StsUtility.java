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
import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.api.services.storagetransfer.v1.model.TransferOptions;
import com.google.api.services.storagetransfer.v1.model.TransferSpec;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

/**
 * Manages the concrete integration with STS
 */
public class StsUtility {

  /**
   * Creates an instance of the STS Client
   */
  public static Storagetransfer createStsClient() throws IOException {
    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault(httpTransport, jsonFactory);
    return createStorageTransferClient(httpTransport, jsonFactory, credential);
  }

  /**
   * Submits a job to be executed by STS
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
  public static TransferJob createStsJob(Storagetransfer client,
                                         String projectId,
                                         String sourceBucket,
                                         String destinationBucket,
                                         List<String> prefixes,
                                         String description,
                                         ZonedDateTime startDateTime)
      throws IOException {
    Date date = createDate(startDateTime.toLocalDate());
    TimeOfDay time = createTimeOfDay(startDateTime.toLocalTime());
    TransferJob transferJob =
        new TransferJob()
            .setProjectId(projectId)
            .setDescription(description)
            .setTransferSpec(
                new TransferSpec()
                    .setGcsDataSource(new GcsData().setBucketName(sourceBucket))
                    .setGcsDataSink(new GcsData().setBucketName(destinationBucket))
                    .setObjectConditions(new ObjectConditions().setIncludePrefixes(prefixes))
                    .setTransferOptions(
                        new TransferOptions()
                            .setDeleteObjectsFromSourceAfterTransfer(false)
                            .setOverwriteObjectsAlreadyExistingInSink(true)))
            .setSchedule(
                new Schedule()
                    .setScheduleStartDate(date)
                    .setStartTimeOfDay(time)
                    .setScheduleEndDate(date))
            .setStatus("ENABLED");

    return client.transferJobs().create(transferJob).execute();
  }

  /**
   * Creates a recurring scheduled STS job
   * @param client the {@link Storagetransfer} client to use
   * @param projectId the project ID of the target GCP project
   * @param sourceBucket the name of the bucket from which you want to move
   * @param destinationBucket the name of the bucket to which you want to move
   * @param prefixesToExclude a {@link List} of prefixes to exclude from the job
   * @param description the description of the job as it will appear in the STS console
   * @param startDateTime a {@link ZonedDateTime} of when you would like the job to begin
   * @return the {@link TransferJob} object that is created
   * @throws IOException when the client or job cannot be created
   */
  public static TransferJob createDefaultStsJob(Storagetransfer client,
                                                  String projectId,
                                                  String sourceBucket,
                                                  String destinationBucket,
                                                  List<String> prefixesToExclude,
                                                  String description,
                                                  ZonedDateTime startDateTime,
                                                  int retentionInDays)
      throws IOException{
    Date date = createDate(startDateTime.toLocalDate());
    TimeOfDay time = createTimeOfDay(startDateTime.toLocalTime());
    TransferJob transferJob =
        new TransferJob()
            .setProjectId(projectId)
            .setDescription(description)
            .setTransferSpec(
                new TransferSpec()
                    .setGcsDataSource(new GcsData().setBucketName(sourceBucket))
                    .setGcsDataSink(new GcsData().setBucketName(destinationBucket))
                    .setObjectConditions(
                        new ObjectConditions()
                            .setExcludePrefixes(prefixesToExclude)
                            .setMinTimeElapsedSinceLastModification(
                                convertRetentionInDaysToDuration(retentionInDays)))
                    .setTransferOptions(
                        new TransferOptions()
                            .setDeleteObjectsFromSourceAfterTransfer(false)
                            .setOverwriteObjectsAlreadyExistingInSink(true)))
            .setSchedule(
                new Schedule()
                    .setScheduleStartDate(date)
                    .setStartTimeOfDay(time))
            .setStatus("ENABLED");

    return client.transferJobs().create(transferJob).execute();
  }

  private static Storagetransfer createStorageTransferClient(
      HttpTransport httpTransport, JsonFactory jsonFactory, GoogleCredential credential) {

    Preconditions.checkNotNull(httpTransport);
    Preconditions.checkNotNull(jsonFactory);
    Preconditions.checkNotNull(credential);

    credential = credential.createScoped(StorageScopes.all());
    credential = credential.createScoped(StoragetransferScopes.all());

    // In some cases, you need to add the scope explicitly.
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(StorageScopes.all());
      credential = credential.createScoped(StoragetransferScopes.all());
    }

    HttpRequestInitializer initializer = new RetryHttpInitializerWrapper(credential);
    return new Storagetransfer.Builder(httpTransport, jsonFactory, initializer)
        .setApplicationName("sdrs")
        .build();
  }

  private static String convertRetentionInDaysToDuration(int retentionInDays) {
    int ONE_DAY_IN_SECS = 3600 * 24;
    return (retentionInDays * ONE_DAY_IN_SECS) + "s";
  }

  private static Date createDate(LocalDate startDate) {
    Date googleDate = new Date();
    googleDate.setYear(startDate.getYear());
    googleDate.setMonth(startDate.getMonthValue());
    googleDate.setDay(startDate.getDayOfMonth());

    return googleDate;
  }

  private static TimeOfDay createTimeOfDay(LocalTime startTime) {
    TimeOfDay timeOfDay = new TimeOfDay();
    timeOfDay.setHours(startTime.getHour());
    timeOfDay.setMinutes(startTime.getMinute());
    timeOfDay.setSeconds(startTime.getSecond());

    return timeOfDay;
  }
}
