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

import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.dao.Dao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleTypes;
import com.google.gcs.sdrs.service.RetentionRulesService;

import static com.google.gcs.sdrs.controller.RetentionRulesController.STORAGE_SEPARATOR;
import static com.google.gcs.sdrs.controller.RetentionRulesController.STORAGE_PREFIX;

/** Service implementation for managing retention rules including mapping. */
public class RetentionRulesServiceImpl implements RetentionRulesService {
  private static final String DEFAULT_PROJECT_ID = "global-default";

  Dao<RetentionRule, Integer> dao = SingletonDao.getRetentionRuleDao();

  @Override()
  public Integer createRetentionRule(RetentionRuleCreateRequest rule) {
    RetentionRule entity = mapPojoToPersistenceEntity(rule);
    return dao.save(entity);
  }

  private RetentionRule mapPojoToPersistenceEntity(RetentionRuleCreateRequest pojo) {
    RetentionRule entity = new RetentionRule();

    // Map over input values
    entity.setDataStorageName(pojo.getDataStorageName());
    entity.setProjectId(pojo.getProjectId());
    entity.setRetentionPeriodInDays(pojo.getRetentionPeriod());
    entity.setType(pojo.getType());

    String datasetName = pojo.getDatasetName();
    if (datasetName == null) {
      datasetName = extractDatasetNameFromDataStorage(pojo.getDataStorageName());
    }
    entity.setDatasetName(datasetName);

    if (entity.getType() == RetentionRuleTypes.GLOBAL) {
      entity.setProjectId(DEFAULT_PROJECT_ID);
    }

    // Generate metadata
    entity.setIsActive(true);
    entity.setVersion(1);

    // TODO: pull actual user value from JWT
    entity.setUser("user");

    return entity;
  }

  private String extractDatasetNameFromDataStorage(String dataStorageName) {
    if (dataStorageName == null) {
      return null;
    }

    String removedPrefix = dataStorageName.substring(STORAGE_PREFIX.length());
    String[] bucketAndDataset = removedPrefix.split(STORAGE_SEPARATOR, 2);

    if (bucketAndDataset.length == 2) {
      return bucketAndDataset[1];
    } else {
      return bucketAndDataset[0];
    }
  }
}
