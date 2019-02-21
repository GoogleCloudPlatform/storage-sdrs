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

package com.google.gcs.sdrs.runners;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gcs.sdrs.controller.pojo.ExecutionEventRequest;
import com.google.gcs.sdrs.enums.ExecutionEventType;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

/**
 * Calls the Rule execution endpoint when run
 */
public class RuleExecutionRunner implements Runnable {

  private static String SERVICE_URL;
  private static final Logger logger = LoggerFactory.getLogger(RuleExecutionRunner.class);

  public RuleExecutionRunner(){
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      SERVICE_URL = config.getString("scheduler.serviceUrl");
    } catch (ConfigurationException ex) {
      logger.error("Configuration file could not be read: " + ex.getMessage());
    }
  }

  /**
   * Calls the SDRS rule execution endpoint to run every retention rule
   */
  public void run(){
    logger.info("Making request to execution service endpoint.");

    try{
      ExecutionEventRequest requestObject = new ExecutionEventRequest();
      requestObject.setExecutionEventType(ExecutionEventType.POLICY);

      ObjectMapper jsonMapper = new ObjectMapper();
      String requestObjectJson = jsonMapper.writeValueAsString(requestObject);

      Client client = ClientBuilder.newClient();
      client.target(SERVICE_URL).path("events/execution")
          .request(MediaType.APPLICATION_JSON)
          .post(Entity.entity(requestObjectJson, MediaType.APPLICATION_JSON));
    } catch (JsonProcessingException ex) {
      logger.error("Execution request could not be sent: ", ex.getMessage());
    }
  }
}
