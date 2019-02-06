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
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * An implementation of the Rule Executor interface that uses STS
 */
public class StsRuleExecutor implements RuleExecutor {

  private static final Logger logger = LoggerFactory.getLogger(StsRuleExecutor.class);

  /**
   * Executes a dataset retention rule
   * @param rule The retention rule to execute
   * @return A RetentionJob object
   * @throws IOException when communication can't be established with STS
   */
  @Override
  public RetentionJob executeDatasetRule(RetentionRule rule) throws IOException{

    String suffix = getSuffixFromConfig();

    // Prefix = datasetName + generated path i.e. dataset/2018/12/31/12
    List<String> prefixes = new ArrayList<>();
    prefixes.add("testDataset/2018/12/31/12");
    // TODO generate prefixes once Tom's code is merged

    String projectId = rule.getProjectId();
    String sourceBucket = formatDataStorageName(rule.getDataStorageName());
    String destinationBucket = formatDataStorageName(rule.getDataStorageName(), suffix);
    LocalDateTime dateTimeNow = LocalDateTime.now(Clock.systemUTC());
    String description = String.format(
        "Rule %s %s", rule.getId().toString(), dateTimeNow.toString());

    logger.debug(
        String.format("Creating STS job with projectId: %s, " +
                "description: %s, source: %s, destination: %s",
            projectId,
            description,
            sourceBucket,
            destinationBucket));

    Storagetransfer client = StsUtility.createStsClient();
    TransferJob job =
        StsUtility.createStsJob(
            client,
            projectId,
            sourceBucket,
            destinationBucket,
            prefixes,
            description,
            dateTimeNow);

    return buildRetentionJobEntity(job.getName(), rule);
  }

  /**
   * NOT IMPLEMENTED! Executes a default retention rule.
   * @param rule the default rule to execute
   * @param affectedDatasetRules any dataset rules that exist within the same bucket
   *                             as the default rule
   * @return A collection of RetentionJob records
   */
  public Collection<RetentionJob> executeDefaultRule(RetentionRule rule, Collection<RetentionRule> affectedDatasetRules) {

    String suffix = getSuffixFromConfig();
    Collection<RetentionJob> jobRecords = new LinkedList<>();

//    for (RetentionRule datasetRule : affectedDatasetRules) {
//
//      Collection<String> sourceBuckets = new ArrayList<>();
//      // TODO generate prefixes once Tom's code is merged
//
//      // TEST CODE START
//      sourceBuckets.add("gs://jk_test_bucket/2018/12/31/00");
//      // END TEST CODE
//
//      for (String bucketName : sourceBuckets) {
//
//        String projectId = datasetRule.getProjectId();
//        String name = UUID.randomUUID().toString();
//        String sourceBucket = bucketName;
//        String destinationBucket = buildDestinationBucketName(datasetRule, sourceBucket, suffix);
//        LocalDate startDate = LocalDate.now();
//        LocalTime startTime = LocalTime.now();
//
//        TransferJob job;
//
//        try {
//          Storagetransfer client = StsUtility.createStsClient();
//          job = StsUtility.createStsJob(
//              client, projectId, name, sourceBucket, destinationBucket, startDate, startTime);
//
//          jobRecords.add(buildRetentionJobEntity(job, rule, datasetRule));
//        } catch (IOException ex) {
//          logger.error("Couldn't connect to STS: " + ex.getCause());
//          // TODO How do we want to handle failed STS jobs?
//        }
//      }
//    }

    return jobRecords;
  }

  private String getSuffixFromConfig(){
    // TODO load suffix from configuration
    return "shadow";
  }

  private String formatDataStorageName(String dataStorageName) {

    dataStorageName = dataStorageName.replaceFirst("gs://","");

    if (dataStorageName.endsWith("/")) {
      dataStorageName = dataStorageName.replaceAll("/", "");
    }

    return dataStorageName;
  }

  private String formatDataStorageName(String dataStorageName, String suffix) {
    return formatDataStorageName(dataStorageName).concat(suffix);
  }

  private RetentionJob buildRetentionJobEntity(String jobName, RetentionRule rule) {
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName(jobName);
    retentionJob.setRetentionRuleId(rule.getId());
    retentionJob.setRetentionRuleProjectId(rule.getProjectId());
    retentionJob.setRetentionRuleDataStorageName(rule.getDataStorageName());
    retentionJob.setRetentionRuleType(rule.getType().toString());
    retentionJob.setRetentionRuleVersion(rule.getVersion());

    return retentionJob;
  }

  private RetentionJob buildRetentionJobEntity(
      String jobName, RetentionRule defaultRule, RetentionRule affectedDatasetRule) {
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName(jobName);
    retentionJob.setRetentionRuleId(defaultRule.getId());
    retentionJob.setRetentionRuleProjectId(affectedDatasetRule.getProjectId());
    retentionJob.setRetentionRuleDataStorageName(affectedDatasetRule.getDataStorageName());
    retentionJob.setRetentionRuleType(defaultRule.getType().toString());
    retentionJob.setRetentionRuleVersion(affectedDatasetRule.getVersion());

    return retentionJob;
  }
}
