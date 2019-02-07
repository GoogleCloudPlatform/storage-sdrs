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

  /** Calls the validate job execution status endpoint */
  public void run() {
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      String serviceUrl = config.getString("scheduler.serviceUrl");

      // TODO: This URL should be the load balancer URL and protected by authorization (JWT/SA) that
      // will need to be configured here
      logger.info("Making request to validation service endpoint.");
      Client client = ClientBuilder.newClient();
      client.target(serviceUrl).path("events/validation").request().post(null);

    } catch (ConfigurationException ex) {
      logger.error("Configuration file could not be read: " + ex.getMessage());
    }
  }
}
