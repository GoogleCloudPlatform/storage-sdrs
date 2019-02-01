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
import com.google.gcs.sdrs.util.StsUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;

public class StsRuleExecutor implements RuleExecutor {

  private static final Logger logger = LoggerFactory.getLogger(StsRuleExecutor.class);

  public Collection<RetentionJob> executeDatasetRule(RetentionRule rule) {

    String suffix = getSuffixFromConfig();

    Collection<String> sourceBuckets = new ArrayList<>();

    // TEST CODE START
    sourceBuckets.add("gs://jk_test_bucket/2018/12/31/00");
    // END TEST CODE

    Collection<RetentionJob> jobRecords = new LinkedList<>();
    // TODO generate prefixes once Tom's code is merged

    for (String bucketName : sourceBuckets) {

      String projectId = rule.getProjectId();
      String name = UUID.randomUUID().toString();
      String sourceBucket = bucketName;
      String destinationBucket = buildDestinationBucketName(rule, sourceBucket, suffix);
      LocalDate startDate = LocalDate.now();
      LocalTime startTime = LocalTime.now();

      TransferJob job;

      try {
        Storagetransfer client = StsUtility.createStsClient();
        job = StsUtility.createStsJob(
            client, projectId, name, sourceBucket, destinationBucket, startDate, startTime);

        jobRecords.add(convertTransferJobToEntity(job, rule));
      } catch (IOException ex) {
        logger.error("Couldn't connect to STS: " + ex.getCause());
        // TODO How do we want to handle failed STS jobs?
      }
    }

    return jobRecords;
  }

  public Collection<RetentionJob> executeDefaultRule(RetentionRule rule, Collection<RetentionRule> affectedDatasetRules) {

    String suffix = getSuffixFromConfig();
    Collection<RetentionJob> jobRecords = new LinkedList<>();

    for (RetentionRule datasetRule : affectedDatasetRules) {

      Collection<String> sourceBuckets = new ArrayList<>();
      // TODO generate prefixes once Tom's code is merged

      // TEST CODE START
      sourceBuckets.add("gs://jk_test_bucket/2018/12/31/00");
      // END TEST CODE

      for (String bucketName : sourceBuckets) {

        String projectId = datasetRule.getProjectId();
        String name = UUID.randomUUID().toString();
        String sourceBucket = bucketName;
        String destinationBucket = buildDestinationBucketName(datasetRule, sourceBucket, suffix);
        LocalDate startDate = LocalDate.now();
        LocalTime startTime = LocalTime.now();

        TransferJob job;

        try {
          Storagetransfer client = StsUtility.createStsClient();
          job = StsUtility.createStsJob(
              client, projectId, name, sourceBucket, destinationBucket, startDate, startTime);

          jobRecords.add(convertTransferJobToEntity(job, rule, datasetRule));
        } catch (IOException ex) {
          logger.error("Couldn't connect to STS: " + ex.getCause());
          // TODO How do we want to handle failed STS jobs?
        }
      }
    }

    return jobRecords;
  }

  private String getSuffixFromConfig(){
    // TODO load suffix from configuration
    return "shadow";
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

  private RetentionJob convertTransferJobToEntity(
      TransferJob job, RetentionRule defaultRule, RetentionRule affectedDatasetRule) {
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName(job.getName());
    retentionJob.setRetentionRuleId(defaultRule.getId());
    retentionJob.setRetentionRuleProjectId(affectedDatasetRule.getProjectId());
    retentionJob.setRetentionRuleDataStorageName(affectedDatasetRule.getDataStorageName());
    retentionJob.setRetentionRuleType(defaultRule.getType().toString());
    retentionJob.setRetentionRuleVersion(affectedDatasetRule.getVersion());

    return retentionJob;
  }

  private String buildDestinationBucketName(
      RetentionRule rule, String sourceBucket, String suffix) {

    String datasetName = rule.getDatasetName();
    String destinationBucket = sourceBucket.replaceFirst(
        datasetName, datasetName + suffix);

    return destinationBucket;
  }
}
