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
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.common.RetentionUnitType;
import com.google.gcs.sdrs.common.RetentionValue;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.dao.RetentionRuleDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.converter.DataStorageType;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.service.RetentionRulesService;
import com.google.gcs.sdrs.service.manager.JobManager;
import com.google.gcs.sdrs.util.GcsHelper;
import com.google.gcs.sdrs.util.RetentionUtil;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service implementation for managing retention rules including mapping. */
public class RetentionRulesServiceImpl implements RetentionRulesService {

  JobManager jobManager;

  private static final String DEFAULT_PROJECT_ID = "global-default";
  private static final String DEFAULT_STORAGE_NAME = "global";
  private static final String DEFAULT_UNKNOWN_USER = "unknown";
  private String defaultProjectId;
  private String defaultStorageName;

  private static final Logger logger = LoggerFactory.getLogger(RetentionRulesServiceImpl.class);

  RetentionRuleDao ruleDao = SingletonDao.getRetentionRuleDao();

  public RetentionRulesServiceImpl() {
    defaultProjectId =
        SdrsApplication.getAppConfigProperty("sts.defaultProjectId", DEFAULT_PROJECT_ID);
    defaultStorageName =
        SdrsApplication.getAppConfigProperty("sts.defaultStorageName", DEFAULT_STORAGE_NAME);
    jobManager = JobManager.getInstance();
  }

  /**
   * Creates a new retention rule in the database
   *
   * @param rule the {@link RetentionRuleCreateRequest} object input by the user
   * @return the {@link Integer} id of the created rule
   */
  @Override
  public Integer createRetentionRule(RetentionRuleCreateRequest rule, UserInfo user)
      throws SQLException, IOException {
    String bucketName = RetentionUtil.getBucketName(rule.getDataStorageName());
    if (rule.getRetentionRuleType() != RetentionRuleType.GLOBAL) {
      if (!GcsHelper.getInstance().doesBucketExist(bucketName, rule.getProjectId())) {
        throw new IOException(
            String.format("Bucket %s does not exist for project %s", bucketName, rule.getProjectId()));
      }
    }
    String userName = user.getEmail() == null ? DEFAULT_UNKNOWN_USER : user.getEmail();

    List<RetentionRule> bucketRules =
        ruleDao.findRulesByDataStorageRoot(
            rule.getProjectId(),
            RetentionUtil.getBucketName(rule.getDataStorageName()),
            rule.getRetentionRuleType(),
            true);

    RetentionRule existingRule = getExistingRule(bucketRules, rule);
    RetentionRule newRule = null;
    if (existingRule == null) {
      // This is a truly new rule
      newRule = mapPojoToPersistenceEntity(rule, userName);
      newRule.setId(ruleDao.save(newRule));
    } else if (!existingRule.getIsActive()) {
      // The rule is not new; re-use the previously deactivated rule with updated values
      updateUserInputValues(rule, userName, existingRule);
      existingRule.setIsActive(true);
      existingRule.setVersion(existingRule.getVersion() + 1);

      newRule = existingRule;
      ruleDao.update(newRule);
    }

    return newRule.getId();
  }

  private RetentionRule getExistingRule(
      List<RetentionRule> rules, RetentionRuleCreateRequest ruleRequest) throws SQLException {
    if (rules != null) {
      for (RetentionRule rule : rules) {
        String dataStorageName = ruleRequest.getDataStorageName();
        if (dataStorageName.equals(rule.getDataStorageName())) {
          if (rule.getIsActive()) {
            throw new SQLException(
                String.format(
                    "A %s rule already exists with project id: %s, data storage name: %s",
                    rule.getType().toString(), rule.getProjectId(), rule.getDataStorageName()));
          } else {
            return rule;
          }
        }

        if (rule.getIsActive()
            && (dataStorageName.contains(rule.getDataStorageName())
                || rule.getDataStorageName().contains(dataStorageName))) {
          throw new SQLException(
              String.format(
                  "A %s rule for %s already exists with project id: %s. The request for %s is not allowed for violating non-nesting rule",
                  rule.getType().toString(),
                  rule.getDataStorageName(),
                  rule.getProjectId(),
                  dataStorageName));
        }
      }
    }
    return null;
  }

  /**
   * Gets a {@link RetentionRule} by projectId and dataStorageName
   *
   * @param projectId the project associated with the rule
   * @param dataStorageName the dataStorageName associated with the rule
   * @return a {@link RetentionRuleResponse} object
   */
  @Override
  public RetentionRuleResponse getRetentionRuleByBusinessKey(
      String projectId, String dataStorageName, RetentionRuleType retentionRuleType) {
    RetentionRule rule =
        ruleDao.findByBusinessKey(projectId, dataStorageName, true, retentionRuleType);
    if (rule == null) {
      throw new EntityNotFoundException(
          String.format(
              "No rule found matching projectId: '%s' and dataStorageName: '%s'",
              projectId, dataStorageName));
    }
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

    if (entity == null || !entity.getIsActive()) {
      throw new SQLException(String.format("No rule exists with ID: %s", ruleId));
    }

    entity.setVersion(entity.getVersion() + 1);
    entity.setRetentionValue(
        buildRetentionValue(
            request.getRetentionPeriod(),
            RetentionValue.parse(entity.getRetentionValue()).getUnitTypeString()));

    ruleDao.update(entity);

    return mapRuleToResponse(entity);
  }

  @Override
  public Integer deleteRetentionRuleByBusinessKey(
      String projectId, String dataStorageName, RetentionRuleType retentionRuleType) {
    RetentionRule rule =
        ruleDao.findByBusinessKey(projectId, dataStorageName, false, retentionRuleType);
    if (rule != null) {
      int deletedRule = ruleDao.softDelete(rule);
      return deletedRule;
    }
    return null;
  }

  private RetentionRule mapPojoToPersistenceEntity(RetentionRuleCreateRequest pojo, String user) {
    RetentionRule entity = new RetentionRule();
    updateUserInputValues(pojo, user, entity);

    // Generate metadata
    entity.setIsActive(true);
    entity.setVersion(1);

    return entity;
  }

  private String buildRetentionValue(int retentionPeroid, String unitType) {
    RetentionUnitType type = RetentionUnitType.getType(unitType);
    String typeStr = RetentionUnitType.DAY.toDatabaseRepresentation();
    if (type != null) {
      typeStr = type.toDatabaseRepresentation();
    }
    return String.format("%s:%s", String.valueOf(retentionPeroid), typeStr);
  }

  private void updateUserInputValues(
      RetentionRuleCreateRequest pojo, String user, RetentionRule entity) {
    // Map over input values
    entity.setDataStorageName(pojo.getDataStorageName());
    entity.setProjectId(pojo.getProjectId());
    entity.setType(pojo.getRetentionRuleType());
    entity.setRetentionValue(
        buildRetentionValue(pojo.getRetentionPeriod(), pojo.getRetentionPeriodUnit()));
    entity.setDataStorageRoot(RetentionUtil.getBucketName(pojo.getDataStorageName()));
    entity.setDataStorageType(DataStorageType.GOOGLE_CLOUD_STORAGE.toDatabaseRepresentation());

    String datasetName = pojo.getDatasetName();
    if (datasetName == null) {
      datasetName = extractDatasetNameFromDataStorage(pojo.getDataStorageName());
    }
    entity.setDatasetName(datasetName);

    if (entity.getType() == RetentionRuleType.GLOBAL) {
      entity.setProjectId(defaultProjectId);
      entity.setDataStorageName(defaultStorageName);
    }

    entity.setUser(user);
  }

  private RetentionRuleResponse mapRuleToResponse(RetentionRule rule) {
    if (rule == null) {
      return null;
    }
    RetentionRuleResponse response = new RetentionRuleResponse();

    response.setDatasetName(rule.getDatasetName());
    response.setDataStorageName(rule.getDataStorageName());
    response.setProjectId(rule.getProjectId());
    response.setRuleId(rule.getId());
    response.setType(rule.getType());
    RetentionValue retentionValue = RetentionValue.parse(rule.getRetentionValue());
    if (retentionValue != null) {
      response.setRetentionPeriodUnit(retentionValue.getUnitTypeString());
      response.setRetentionPeriod(retentionValue.getNumber());
    }

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
