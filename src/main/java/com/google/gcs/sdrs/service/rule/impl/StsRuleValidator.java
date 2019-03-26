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

package com.google.gcs.sdrs.service.rule.impl;

import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.model.Operation;
import com.google.gcs.sdrs.RetentionJobStatusType;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.service.rule.RuleValidator;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.StsUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An implementation of the RuleValidtor interface that uses STS */
public class StsRuleValidator implements RuleValidator {

  public static StsRuleValidator instance;
  static CredentialsUtil credentialsUtil = CredentialsUtil.getInstance();

  Storagetransfer client;
  private static final Logger logger = LoggerFactory.getLogger(StsRuleValidator.class);

  /**
   * Gets the singleton instance of the StsRuleValidator
   *
   * @return the singleton instance of the StsRuleValidator
   */
  public static StsRuleValidator getInstance() {
    if (instance == null) {
      try {
        instance = new StsRuleValidator();
      } catch (IOException ex) {
        logger.error("Could not establish connection with STS: ", ex.getMessage());
      }
    }

    return instance;
  }

  /**
   * Validates the status of a given {@link RetentionJob} against STS
   *
   * @param job the {@link RetentionJob} to validate
   * @return a {@link RetentionJobValidation} record
   */
  public @Nullable RetentionJobValidation validateRetentionJob(RetentionJob job) {
    List<RetentionJob> jobList = new ArrayList<>();
    jobList.add(job);
    List<RetentionJobValidation> validationList = validateRetentionJobs(jobList);
    if (validationList.size() == 0) {
      return null;
    }
    return validationList.get(0);
  }

  /**
   * Validates a {@link List} of {@link RetentionJob} objects against STS
   *
   * @param jobs the {@link List} of {@link RetentionJob} objects to validate
   * @return a {@link List} of {@link RetentionJobValidation} records
   */
  public List<RetentionJobValidation> validateRetentionJobs(List<RetentionJob> jobs) {

    if (jobs.size() == 0) {
      return new ArrayList<>();
    }

    List<String> jobNames = new ArrayList<>();
    HashMap<String, Integer> jobIdStsIdMap = new HashMap<>();

    // Get the first project ID. All jobs to validate must have the same project ID.
    String projectId = jobs.get(0).getRetentionRuleProjectId();
    for (RetentionJob job : jobs) {
      if (!job.getRetentionRuleProjectId().equalsIgnoreCase(projectId)) {
        String message =
            String.format(
                "The list of jobs to validate contains multiple Project IDs:"
                    + "%s, %s. All retention jobs to validate must have the same project ID",
                projectId, job.getRetentionRuleProjectId());
        logger.error(message);
        throw new IllegalArgumentException(message);
      }
      // the job name that is required in the request has a different format than the response
      String stsJobId = job.getName().substring(job.getName().indexOf("/") + 1);
      jobIdStsIdMap.put(stsJobId, job.getId());
      jobNames.add(job.getName());
    }

    List<Operation> jobOperations = StsUtil.getSubmittedStsJobs(client, projectId, jobNames);
    List<RetentionJobValidation> validationRecords = new ArrayList<>();
    for (Operation operation : jobOperations) {

      String stsJobId = extractStsJobId(operation.getName());
      int jobId = jobIdStsIdMap.get(stsJobId);

      validationRecords.add(convertOperationToJobValidation(operation, jobId));
    }

    return validationRecords;
  }

  RetentionJobValidation convertOperationToJobValidation(Operation operation, int jobId) {
    RetentionJobValidation validation = new RetentionJobValidation();
    validation.setJobOperationName(operation.getName());
    validation.setRetentionJobId(jobId);
    if (!operation.getDone()) {
      validation.setStatus(RetentionJobStatusType.PENDING);
    } else if (operation.getResponse() != null) {
      validation.setStatus(RetentionJobStatusType.SUCCESS);
      logger.info(
          String.format(
              "STS Operation %s Successful: %s",
              operation.getName(), operation.getResponse().toString()));
    } else {
      validation.setStatus(RetentionJobStatusType.ERROR);
      logger.info(
          String.format(
              "STS Operation %s failed: %s",
              operation.getName(), operation.getError().getMessage()));
    }

    return validation;
  }

  String extractStsJobId(String operationName) throws StringIndexOutOfBoundsException {
    /* The response operation name is in the format
     * "transferOperation/transferJob-<GCP_Job_id>-<GCP_Operation_id>"
     * We need to get the GCP_Job_Id only
     */
    int firstHyphenIndex = operationName.indexOf("-");
    int lastHyphenIndex = operationName.lastIndexOf("-");
    if (firstHyphenIndex == -1 || lastHyphenIndex == -1 || firstHyphenIndex == lastHyphenIndex) {
      logger.error("The STS name format has changed: " + operationName);
      throw new StringIndexOutOfBoundsException();
    }

    return operationName.substring(firstHyphenIndex + 1, lastHyphenIndex);
  }

  private StsRuleValidator() throws IOException {
    client = StsUtil.createStsClient(credentialsUtil.getCredentials());
  }
}
