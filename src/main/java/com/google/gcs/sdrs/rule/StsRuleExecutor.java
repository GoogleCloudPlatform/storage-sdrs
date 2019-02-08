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
import com.google.gcs.sdrs.enums.RetentionRuleType;
import com.google.gcs.sdrs.util.PrefixGeneratorUtility;
import com.google.gcs.sdrs.util.StsUtility;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * An implementation of the Rule Executor interface that uses STS
 */
public class StsRuleExecutor implements RuleExecutor {

  private final String DEFAULT_SUFFIX = "shadow";
  private String suffix;
  Storagetransfer client;

  private static final Logger logger = LoggerFactory.getLogger(StsRuleExecutor.class);

  /**
   * STS Rule Executor constructor that reads the bucket suffix from the configuration file
   * @throws IOException when the STS Client cannot be instantiated
   */
  public StsRuleExecutor() throws IOException {
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      suffix = config.getString("sts.suffix");
      client = StsUtility.createStsClient();
    } catch (ConfigurationException ex) {
      logger.error("Configuration could not be read. Using default values: " + ex.getMessage());
      suffix = DEFAULT_SUFFIX;
    }
  }

  /**
   * Executes a dataset retention rule
   * @param rule The {@link RetentionRule} to execute
   * @return A {@link RetentionJob} object
   * @throws IOException when communication can't be established with STS
   * @throws IllegalArgumentException when the rule is a global rule
   */
  @Override
  public RetentionJob executeDatasetRule(RetentionRule rule)
      throws IOException, IllegalArgumentException{

    if (rule.getType().equals(RetentionRuleType.GLOBAL)) {
      throw new IllegalArgumentException("GLOBAL retention rule type is invalid for this function");
    }

    ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(Clock.systemUTC());

    List<String> prefixes = PrefixGeneratorUtility.generateTimePrefixes(rule.getDatasetName(),
        zonedDateTimeNow, zonedDateTimeNow.minusDays(rule.getRetentionPeriodInDays()));
    //prefixes.add("testDataset/2018/12/31/12");

    String projectId = rule.getProjectId();
    String sourceBucket = formatDataStorageName(rule.getDataStorageName());
    String destinationBucket = formatDataStorageName(rule.getDataStorageName(), suffix);

    String description = String.format(
        "Rule %s %s", rule.getId(), zonedDateTimeNow.toString());

    logger.debug(
        String.format("Creating STS job with projectId: %s, " +
                "description: %s, source: %s, destination: %s",
            projectId,
            description,
            sourceBucket,
            destinationBucket));

    TransferJob job =
        StsUtility.createStsJob(
            client,
            projectId,
            sourceBucket,
            destinationBucket,
            prefixes,
            description,
            zonedDateTimeNow);

    return buildRetentionJobEntity(job.getName(), rule);
  }

  /**
   * @param defaultRule the default rule to execute
   * @param bucketDatasetRules any dataset rules that exist within the same bucket
   *                             as the default rule
   * @param scheduledTime the recurring time at which you want the default rule to execute
   * @return A {@link Collection} of {@link RetentionJob} records
   */
  @Override
  public RetentionJob executeDefaultRule(
      RetentionRule defaultRule,
      Collection<RetentionRule> bucketDatasetRules,
      ZonedDateTime scheduledTime)
      throws IOException, IllegalArgumentException {

    if (defaultRule.getType().equals(RetentionRuleType.DATASET)) {
      String message = "DATASET retention rule type is invalid for this function";
      logger.error(message);
      throw new IllegalArgumentException(message);
    }

    List<String> prefixesToExclude = new ArrayList<>();
    for (RetentionRule datasetRule : bucketDatasetRules) {
      // Adds the dataset folder to the exclude list as the retention is already being handled
      // by the dataset rule. No need to generate the full prefix here.
      prefixesToExclude.add(datasetRule.getDatasetName());
    }

    // STS has a restriction of 1000 values in any prefix collection
    if (prefixesToExclude.size() > 1000) {
      String message = "There are too many dataset rules associated with this bucket. " +
          "A maximum of 1000 rules are allowed.";
      logger.error(message);
      throw new IllegalArgumentException(message);
    }

    String projectId = defaultRule.getProjectId();
    // if the default rule doesn't have a projectId, get it from a child dataset rule
    if (defaultRule.getProjectId() == null) {
      Optional<RetentionRule> childRuleWithProject =
          bucketDatasetRules.stream().filter(r -> !r.getProjectId().isEmpty()).findFirst();
      if (childRuleWithProject.isPresent()) {
        projectId = childRuleWithProject.get().getProjectId();
      } else {
        String message = "STS job could not be created. No projectId found.";
        logger.error(message);
        throw new IllegalArgumentException(message);
      }
    }

    String sourceBucket = formatDataStorageName(defaultRule.getDataStorageName());
    String destinationBucket = formatDataStorageName(defaultRule.getDataStorageName(), suffix);

    String description = String.format(
        "Rule %s %s", defaultRule.getId().toString(), scheduledTime.toString());

    logger.debug(
        String.format("Creating STS job with for rule %s, projectId: %s, " +
                "description: %s, source: %s, destination: %s",
            defaultRule.getId(),
            projectId,
            description,
            sourceBucket,
            destinationBucket));

    TransferJob job =
        StsUtility.createDefaultStsJob(
            client,
            projectId,
            sourceBucket,
            destinationBucket,
            prefixesToExclude,
            description,
            scheduledTime,
            defaultRule.getRetentionPeriodInDays());

    return buildRetentionJobEntity(job.getName(), defaultRule);
  }

  String formatDataStorageName(String dataStorageName) {

    dataStorageName = dataStorageName.replaceFirst("gs://","");

    if (dataStorageName.endsWith("/")) {
      dataStorageName = dataStorageName.replaceAll("/", "");
    }

    return dataStorageName;
  }

  String formatDataStorageName(String dataStorageName, String suffix) {
    return formatDataStorageName(dataStorageName).concat(suffix);
  }

  RetentionJob buildRetentionJobEntity(String jobName, RetentionRule rule) {
    RetentionJob retentionJob = new RetentionJob();
    retentionJob.setName(jobName);
    retentionJob.setRetentionRuleId(rule.getId());
    retentionJob.setRetentionRuleProjectId(rule.getProjectId());
    retentionJob.setRetentionRuleDataStorageName(rule.getDataStorageName());
    retentionJob.setRetentionRuleType(rule.getType());
    retentionJob.setRetentionRuleVersion(rule.getVersion());

    return retentionJob;
  }
}
