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

package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.RetentionJobStatusType;
import com.google.gcs.sdrs.dao.RetentionJobValidationDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleValidator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A worker class to do the validation */
public class ValidationWorker extends BaseWorker {

  private final Logger logger = LoggerFactory.getLogger(ValidationWorker.class);

  RetentionJobValidationDao dao = SingletonDao.getRetentionJobValidationDao();
  StsRuleValidator stsRuleValidator = StsRuleValidator.getInstance();

  public ValidationWorker() {
    super();
  }

  /**
   * Find all retention jobs that need to have their status validated, query their status from STS
   * and then update the DB with the status.
   */
  @Override
  public void doWork() {
    List<RetentionJob> retentionJobs = dao.findAllPendingRetentionJobs();

    if (retentionJobs.size() > 0) {
      // An STS job status query can only be done on one project id at a time, so split the list
      // into lists of projects grouped by project id
      Map<String, List<RetentionJob>> jobsByProject =
          retentionJobs.stream()
              .collect(Collectors.groupingBy(RetentionJob::getRetentionRuleProjectId));

      Map<String, List<RetentionJobValidation>> stsValidations = new HashMap<>();
      for (List<RetentionJob> jobs : jobsByProject.values()) {
        // Get validation results from STS for each group of jobs
        List<RetentionJobValidation> retentionJobValidations =
            stsRuleValidator.validateRetentionJobs(jobs);
        // Combine all retentionJobValidation results from STS into one map by JobName so we can
        // quickly search it later on
        retentionJobValidations.stream()
            .forEach(
                validation -> {
                  String jobOperationName = validation.getJobOperationName();
                  if (stsValidations.containsKey(jobOperationName)) {
                    stsValidations.get(jobOperationName).add(validation);
                  } else {
                    List<RetentionJobValidation> validationSet = new ArrayList<>();
                    validationSet.add(validation);
                    stsValidations.put(jobOperationName, validationSet);
                  }
                });
      }

      if (stsValidations.size() > 0) {
        // Our map of STS validations may or may not already exist in the DB. We need to query the
        // DB for each one to see if it exists.
        List<RetentionJobValidation> existingValidations =
            dao.findAllByRetentionJobNames(new ArrayList<>(stsValidations.keySet()));

        // For each validation that exists in the DB, update the matching STS validation with the Id
        // so it can be properly updated
        for (RetentionJobValidation existingValidation : existingValidations) {
          stsValidations.get(existingValidation.getJobOperationName()).stream()
              .forEach(
                  validation -> {
                    if (existingValidation.getRetentionJobId().intValue()
                        == validation.getRetentionJobId().intValue()) {
                        validation.setId(existingValidation.getId());

                    }
                  });
        }

        List<RetentionJobValidation> finalValidationList =
            stsValidations.values().stream()
                .reduce(
                    (v1, v2) -> {
                      v1.addAll(v2);
                      return v1;
                    })
                .get();

        dao.saveOrUpdateBatch(finalValidationList);
      }
    }
    workerResult.setStatus(WorkerResult.WorkerResultStatus.SUCCESS);
  }
}
