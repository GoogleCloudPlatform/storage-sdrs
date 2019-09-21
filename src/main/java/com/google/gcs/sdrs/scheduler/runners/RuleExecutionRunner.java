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

package com.google.gcs.sdrs.scheduler.runners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.ExecutionEventType;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.util.SdrsRequestClientUtil;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Calls the Rule execution endpoint when run */
@Deprecated
public class RuleExecutionRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(RuleExecutionRunner.class);

  /** Calls the SDRS rule execution endpoint to run every retention rule */
  public void run() {
    logger.info("Making request to execution service endpoint.");

    try {
      ExecutionEventRequest requestObject = new ExecutionEventRequest();
      requestObject.setExecutionEventType(ExecutionEventType.POLICY);

      ObjectMapper jsonMapper = new ObjectMapper();
      String requestObjectJson = jsonMapper.writeValueAsString(requestObject);
      String endpoint =
          SdrsApplication.getAppConfigProperty("scheduler.task.ruleExecution.endpoint");

      Client client = ClientBuilder.newClient();
      Response response =
          SdrsRequestClientUtil.request(client, endpoint)
              .post(Entity.entity(requestObjectJson, MediaType.APPLICATION_JSON));
      logger.info(response.toString());
    } catch (JsonProcessingException ex) {
      logger.error("Execution request could not be sent: ", ex.getMessage());
    } catch (Exception e) {
      logger.error("Failed to request Execution endpoint: ", e.getMessage());
    }
  }
}
