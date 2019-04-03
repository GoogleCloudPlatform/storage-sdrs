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

package com.google.gcs.sdrs.service.rule;

import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;

public interface RuleExecutor {

  List<RetentionJob> executeDatasetRule(Collection<RetentionRule> datasetRules, String projectId)
      throws IOException, IllegalArgumentException;

  List<RetentionJob> executeDefaultRule(
      RetentionRule defaultRule,
      Collection<RetentionRule> bucketDatasetRules,
      ZonedDateTime scheduledTime)
      throws IOException, IllegalArgumentException;

  List<RetentionJob> updateDefaultRule(
      List<RetentionJob> defaultJobs,
      RetentionRule defaultRule,
      Collection<RetentionRule> bucketDatasetRules)
      throws IOException, IllegalArgumentException;

  List<RetentionJob> cancelDefaultJobs(List<RetentionJob> jobs, RetentionRule defaultRule)
      throws IOException, IllegalArgumentException;
}
