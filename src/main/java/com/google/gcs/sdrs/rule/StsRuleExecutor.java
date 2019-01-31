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

package com.google.gcs.sdrs.rule;

import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleTypes;
import com.google.gcs.sdrs.util.StsUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class StsRuleExecutor implements RuleExecutor {

  private static final Logger logger = LoggerFactory.getLogger(StsRuleExecutor.class);

  public void ExecuteRule(RetentionRule rule) {
    // TODO load suffix from configuration
    String suffix = "shadow";

    // TODO generate prefixes once Tom's code is merged

    // TODO Transform rule values into STS job values for each prefix

    String projectId = rule.getProjectId();
    String name = UUID.randomUUID().toString();
    String sourceBucket = buildSourceBucketName(rule);
    String destinationBucket = buildDestinationBucketName(rule, suffix);
    LocalDate startDate = LocalDate.now();
    LocalTime startTime = LocalTime.now();

    TransferJob job;

    try {
      Storagetransfer client = StsUtility.createStsClient();
      job = StsUtility.createStsJob(
          client, projectId, name, sourceBucket, destinationBucket, startDate, startTime);
    } catch (IOException ex) {
      logger.error("Couldn't connect to STS: " + ex.getCause());
      // TODO save as error to DB. Add retry, caching?
    }

    // TODO Save retention_job object once job is submitted
  }

  private RetentionJob convertTransferJobToEntity(TransferJob job, RetentionRule rule) {
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName(job.getName());
    retentionJob.setRetentionRuleId(rule.getId());
    retentionJob.setRetentionRuleProjectId(rule.getProjectId());
    retentionJob.setRetentionRuleDataStorageName(rule.getDataStorageName());
    retentionJob.setRetentionRuleType(rule.getType().toString());
    retentionJob.setRetentionRuleVersion(rule.getVersion());

    return retentionJob;
  }

  private String buildSourceBucketName(RetentionRule rule) {

    String bucketName;

    if (rule.getType().equals(RetentionRuleTypes.DATASET)) {
      bucketName = rule.getDataStorageName();
    } else {
      // TODO How to get the bucket to operate on in the default case?
      bucketName = "default";
    }

    return bucketName;
  }

  private String buildDestinationBucketName(RetentionRule rule, String suffix) {

    String bucketName;
    if (rule.getType().equals(RetentionRuleTypes.DATASET)) {
      String dataStorageName = rule.getDataStorageName();
      String datasetName = rule.getDatasetName();
      bucketName = dataStorageName.replaceFirst("datasetName", datasetName + suffix);
    } else {
      // TODO How to get the bucket to operate on in the default case?
      bucketName = "default";
    }

    return bucketName;
  }
}
