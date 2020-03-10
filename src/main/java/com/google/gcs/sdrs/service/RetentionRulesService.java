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

package com.google.gcs.sdrs.service;

import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.controller.filter.UserInfo;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleCreateRequest;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleResponse;
import com.google.gcs.sdrs.controller.pojo.RetentionRuleUpdateRequest;
import com.google.gcs.sdrs.dao.model.RetentionRule;

import java.io.IOException;
import java.sql.SQLException;

/** Service implementation for managing retention rules. */
public interface RetentionRulesService {

  /**
   * Creates a retention rule and returns its ID
   *
   * @param rule the request object input by the user
   * @param user the user who initiated the request
   * @return The identifier for the created rule
   */
  Integer createRetentionRule(RetentionRuleCreateRequest rule, UserInfo user)
      throws SQLException, IOException;

  /**
   * Gets the retention rule with the provided values
   *
   * @param projectId       the project associated with the rule
   * @param dataStorageName the dataStorageName associated with the rule
   */
  RetentionRuleResponse getRetentionRuleByBusinessKey(
      String projectId, String dataStorageName, RetentionRuleType retentionRuleType);

  /**
   * Updates a retention rule and returns the rule with updates
   *
   * @param ruleId  the identifier for the rule to update
   * @param request the update request
   * @return the updated retention rule
   */
  RetentionRuleResponse updateRetentionRule(Integer ruleId, RetentionRuleUpdateRequest request)
      throws SQLException;

  /**
   * Deletes the retention rule with the provided values
   *
   * @param projectId       the project associated with the rule
   * @param dataStorageName the dataStorageName associated with the rule
   */
  Integer deleteRetentionRuleByBusinessKey(
      String projectId, String dataStorageName, RetentionRuleType retentionRuleType);

  /**
   * Gets the retention rule with the provided values
   *
   * @param ruleId the identifier for the retention rule
   * @return the retention rule
   * @throws SQLException
   */
  RetentionRule getRetentionRuleByRuleId(Integer ruleId) throws SQLException;
}
