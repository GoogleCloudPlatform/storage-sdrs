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

import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import java.io.IOException;
import java.util.List;

public interface DmQueueDao extends Dao<DmRequest, Integer> {

  List<DmRequest> getAllAvailableRequestsByPriority();

  List<DmRequest> getByStatus(String status);

  void createRetentionJobUdpateDmStatus(RetentionJob retentionJob, List<DmRequest> dmRequests) throws IOException;
}
