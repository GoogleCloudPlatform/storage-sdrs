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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Calls the Validation service endpoint when run */
public class ValidationRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);

  private static String SERVICE_URL;

  private ValidationRunner() {
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      SERVICE_URL = config.getString("scheduler.serviceUrl");
    } catch (ConfigurationException ex) {
      logger.error("Configuration file could not be read: " + ex.getMessage());
    }
  }

  /** Calls the validate job execution status endpoint */
  public void run() {
    // TODO: This URL should be the load balancer URL and protected by authorization (JWT/SA) that
    // will need to be configured here
    logger.info("Making request to validation service endpoint.");
    Client client = ClientBuilder.newClient();
    client.target(SERVICE_URL).path("events/validation").request().post(null);
  }
}
