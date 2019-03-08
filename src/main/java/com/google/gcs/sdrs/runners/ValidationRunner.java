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

package com.google.gcs.sdrs.runners;

import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.util.SdrsRequestClientUtil;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Calls the Validation service endpoint when run */
public class ValidationRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);

  /** Calls the validate job execution status endpoint */
  public void run() {
    try {
      String endpoint =
          SdrsApplication.getAppConfigProperty("scheduler.task.validationService.endpoint");

      logger.info("Making request to validation service endpoint. " + endpoint);
      Client client = ClientBuilder.newClient();
      Invocation.Builder builder = SdrsRequestClientUtil.request(client, endpoint);
      Response response = builder.post(null);
      logger.info(response.toString());
    } catch (Exception e) {
      logger.error("Failed to request Validation endpoint. " + e.getMessage());
    }
  }
}
