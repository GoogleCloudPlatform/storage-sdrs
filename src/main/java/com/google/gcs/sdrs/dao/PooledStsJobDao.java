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

package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.PooledStsJob;
import java.util.List;

public interface PooledStsJobDao extends Dao<PooledStsJob, Integer> {

  List<PooledStsJob> getAllPooledStsJobsByBucketName(String bucketName, String projectId);

  Boolean deleteAllJobsByBucketName(String sourceBucket, String sourceProject);

  PooledStsJob findPooledStsJobByNameAndProject(String name, String projectId);

  /**
   * Query job list based on bucketName, projectId and type first, then filter results by
   * scheduleTimeOfDay before return.
   * 1. If query result is null or empty, return empty list.
   * 2. scheduleTimeOfDay == null return all query results.
   * 3. If scheduleTimeOfDay >= lastJob of the day, return all jobs with share
   *    the latest schedule time.
   * 4. Find out the next schedule time after scheduleTimeOfDay params, return all
   *    jobs that share the the next schedule time.
   *
   * @param bucketName
   * @param projectId
   * @param scheduleTimeOfDay
   * @param type
   * @return
   */
  List<PooledStsJob> getJobList(
          String bucketName, String projectId, String scheduleTimeOfDay, String type);

}
