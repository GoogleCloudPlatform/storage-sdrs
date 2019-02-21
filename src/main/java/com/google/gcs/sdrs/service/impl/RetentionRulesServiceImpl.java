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

package com.google.gcs.sdrs.service.impl;

import com.google.gcs.sdrs.JobManager.JobManager;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleType;
import com.google.gcs.sdrs.service.RetentionRulesService;
import com.google.gcs.sdrs.worker.Worker;
import com.google.gcs.sdrs.worker.impl.UpdateExternalJobWorker;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/** Service implementation for managing retention rules including mapping. */
public class RetentionRulesServiceImpl implements RetentionRulesService {
  private static final String DEFAULT_PROJECT_ID = "global-default";
  private static String defaultProjectId;
  private static final Logger logger = LoggerFactory.getLogger(RetentionRulesServiceImpl.class);

  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();

  public RetentionRulesServiceImpl() {
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      defaultProjectId = config.getString("sts.defaultProjectId");
    } catch (ConfigurationException ex) {
      logger.error("Configuration could not be read. Using default values: " + ex.getMessage());
      defaultProjectId = DEFAULT_PROJECT_ID;
    }
  }

  /**
   * Creates a new retention rule in the database
   * @param rule the {@link RetentionRuleCreateRequest} object input by the user
   * @return the {@link Integer} id of the created rule
   */
  @Override()
  public Integer createRetentionRule(RetentionRuleCreateRequest rule) {
    RetentionRule entity = mapPojoToPersistenceEntity(rule);
    int ruleId = ruleDao.save(entity);
    entity.setId(ruleId);

    if (entity.getType().equals(RetentionRuleType.DATASET)) {
      RetentionRule globalRule = ruleDao.findGlobalRuleByProjectId(defaultProjectId);
      if (globalRule != null) {
        Worker updateWorker = new UpdateExternalJobWorker(globalRule, entity.getProjectId());
        JobManager.getInstance().submitJob(updateWorker);
      }
    }

    if (entity.getType().equals(RetentionRuleType.GLOBAL)) {
      List<String> projectIds = ruleDao.getAllDatasetRuleProjectIds();
      for (String projectId : projectIds) {
        // TODO Create default STS job
      }
    }

    return ruleId;
  }

  /**
   * Updates an existing retention rule
   * @param ruleId the identifier for the rule to update
   * @param request the {@link RetentionRuleUpdateRequest} update request
   * @return the {@link RetentionRuleResponse} object
   */
  @Override
  public RetentionRuleResponse updateRetentionRule(
      Integer ruleId, RetentionRuleUpdateRequest request) {

    RetentionRule entity = ruleDao.findById(ruleId);

    entity.setVersion(entity.getVersion() + 1);
    entity.setRetentionPeriodInDays(request.getRetentionPeriod());

    ruleDao.update(entity);

    if (entity.getType().equals(RetentionRuleType.GLOBAL)) {
      List<String> projectIds = ruleDao.getAllDatasetRuleProjectIds();
      for (String projectId : projectIds) {
        Worker updateWorker = new UpdateExternalJobWorker(entity, projectId);
        JobManager.getInstance().submitJob(updateWorker);
      }
    }

    return mapRuleToResponse(entity);
  }

  private RetentionRule mapPojoToPersistenceEntity(RetentionRuleCreateRequest pojo) {
    RetentionRule entity = new RetentionRule();

    // Map over input values
    entity.setDataStorageName(pojo.getDataStorageName());
    entity.setProjectId(pojo.getProjectId());
    entity.setRetentionPeriodInDays(pojo.getRetentionPeriod());
    entity.setType(pojo.getRetentionRuleType());

    String datasetName = pojo.getDatasetName();
    if (datasetName == null) {
      datasetName = extractDatasetNameFromDataStorage(pojo.getDataStorageName());
    }
    entity.setDatasetName(datasetName);

    if (entity.getType() == RetentionRuleType.GLOBAL) {
      entity.setProjectId(defaultProjectId);
    }

    // Generate metadata
    entity.setIsActive(true);
    entity.setVersion(1);

    // TODO: pull actual user value from JWT
    entity.setUser("user");

    return entity;
  }

  private RetentionRuleResponse mapRuleToResponse(RetentionRule rule) {
    RetentionRuleResponse response = new RetentionRuleResponse();

    response.setDatasetName(rule.getDatasetName());
    response.setDataStorageName(rule.getDataStorageName());
    response.setProjectId(rule.getProjectId());
    response.setRetentionPeriod(rule.getRetentionPeriodInDays());
    response.setRuleId(rule.getId());
    response.setType(rule.getType());

    return response;
  }

  private String extractDatasetNameFromDataStorage(String dataStorageName) {
    if (dataStorageName == null) {
      return null;
    }

    String removedPrefix = dataStorageName.substring(ValidationConstants.STORAGE_PREFIX.length());
    String[] bucketAndDataset = removedPrefix.split(ValidationConstants.STORAGE_SEPARATOR, 2);

    if (bucketAndDataset.length == 2) {
      return bucketAndDataset[1];
    } else {
      return bucketAndDataset[0];
    }
  }
}
