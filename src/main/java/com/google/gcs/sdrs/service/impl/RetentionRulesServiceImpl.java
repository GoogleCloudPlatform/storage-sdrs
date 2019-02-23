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

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.controller.filter.UserInfo;
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
import com.google.gcs.sdrs.worker.impl.CancelDefaultJobWorker;
import com.google.gcs.sdrs.worker.impl.CreateDefaultJobWorker;
import com.google.gcs.sdrs.worker.impl.UpdateDefaultJobWorker;
import java.sql.SQLException;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

/** Service implementation for managing retention rules including mapping. */
public class RetentionRulesServiceImpl implements RetentionRulesService {

  JobManager jobManager;

  private static final String DEFAULT_PROJECT_ID = "global-default";
  private static final String DEFAULT_STORAGE_NAME = "global";
  private static String defaultProjectId;
  private static String defaultStorageName;

  private static final Logger logger = LoggerFactory.getLogger(RetentionRulesServiceImpl.class);

  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();

  public RetentionRulesServiceImpl() {
    defaultProjectId = SdrsApplication.getAppConfigProperty(
        "sts.defaultProjectId",
        DEFAULT_PROJECT_ID);
    defaultStorageName = SdrsApplication.getAppConfigProperty("sts.defaultStorageName", DEFAULT_STORAGE_NAME);
    jobManager = JobManager.getInstance();
  }

  /**
   * Creates a new retention rule in the database
   *
   * @param rule the {@link RetentionRuleCreateRequest} object input by the user
   * @return the {@link Integer} id of the created rule
   */
  @Override()
  public Integer createRetentionRule(RetentionRuleCreateRequest rule, UserInfo user)
      throws SQLException {
    RetentionRule entity = mapPojoToPersistenceEntity(rule, user);
    try{
      int ruleId = ruleDao.save(entity);
      entity.setId(ruleId);

      if (entity.getType().equals(RetentionRuleType.DATASET)) {
        updateParentGlobalRule(entity);
      }

      if (entity.getType().equals(RetentionRuleType.GLOBAL)) {
        List<String> projectIds = ruleDao.getAllDatasetRuleProjectIds();
        for (String projectId : projectIds) {
          Worker createDefaultWorker = new CreateDefaultJobWorker(entity, projectId);
          jobManager.submitJob(createDefaultWorker);
        }
      }

      return ruleId;
    } catch (ConstraintViolationException ex) {
      String message =
          String.format(
              "Unique constraint violation. "
                  + "A rule already exists with project id: %s, data storage name: %s",
              entity.getProjectId(), entity.getDataStorageName());
      throw new SQLException(message);
    }
  }

  /**
   * Gets a {@link RetentionRule} by projectId and dataStorageName
   *
   * @param projectId the project associated with the rule
   * @param dataStorageName the dataStorageName associated with the rule
   * @return a {@link RetentionRuleResponse} object
   */
  @Override
  public RetentionRuleResponse getRetentionRuleByBusinessKey(String projectId, String dataStorageName) {
    RetentionRule rule = ruleDao.findByBusinessKey(projectId, dataStorageName);
    return mapRuleToResponse(rule);
  }

  /**
   * Updates an existing retention rule
   *
   * @param ruleId the identifier for the rule to update
   * @param request the {@link RetentionRuleUpdateRequest} update request
   * @return the {@link RetentionRuleResponse} object
   */
  @Override
  public RetentionRuleResponse updateRetentionRule(
      Integer ruleId, RetentionRuleUpdateRequest request) throws SQLException {

    RetentionRule entity = ruleDao.findById(ruleId);

    if (entity == null) {
      throw new SQLException(String.format("No rule exists with ID: %s", ruleId));
    }

    entity.setVersion(entity.getVersion() + 1);
    entity.setRetentionPeriodInDays(request.getRetentionPeriod());

    ruleDao.update(entity);

    if (entity.getType().equals(RetentionRuleType.GLOBAL)) {
      List<String> projectIds = ruleDao.getAllDatasetRuleProjectIds();
      for (String projectId : projectIds) {
        Worker updateWorker = new UpdateDefaultJobWorker(entity, projectId);
        jobManager.submitJob(updateWorker);
      }
    }

    return mapRuleToResponse(entity);
  }

  @Override
  public Integer deleteRetentionRuleByBusinessKey(String projectId, String dataStorageName) {
    RetentionRule rule = ruleDao.findByBusinessKey(projectId, dataStorageName);
    if (rule != null) {
      int deletedRule = ruleDao.softDelete(rule);

      if (rule.getType() == RetentionRuleType.DATASET) {
        updateParentGlobalRule(rule);
      } else if (rule.getType() == RetentionRuleType.GLOBAL) {
        List<String> projectIds = ruleDao.getAllDatasetRuleProjectIds();
        for (String selectedProjectId : projectIds) {
          Worker updateWorker = new CancelDefaultJobWorker(rule, selectedProjectId);
          jobManager.submitJob(updateWorker);
        }
      }

      return deletedRule;
    }
    return null;
  }

  private void updateParentGlobalRule(RetentionRule childRule) {
    RetentionRule globalRule = ruleDao.findGlobalRuleByProjectId(defaultProjectId);
    if (globalRule != null) {
      Worker updateWorker = new UpdateDefaultJobWorker(globalRule, childRule.getProjectId());
      jobManager.submitJob(updateWorker);
    }
  }

  private RetentionRule mapPojoToPersistenceEntity(
      RetentionRuleCreateRequest pojo, UserInfo user) {
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
      entity.setDataStorageName(defaultStorageName);
    }

    entity.setUser(user.getEmail());

    // Generate metadata
    entity.setIsActive(true);
    entity.setVersion(1);

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
