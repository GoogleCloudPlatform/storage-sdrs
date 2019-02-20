package com.google.gcs.sdrs.runners;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Calls the Validation service endpoint when run */
public class ValidationServiceRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ValidationServiceRunner.class);

  private static String DEFAULT_SERVICE_URL = "http://127.0.0.1:8080";
  private static String SERVICE_URL;

  private ValidationServiceRunner() {
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      SERVICE_URL = config.getString("scheduler.serviceUrl");
    } catch (ConfigurationException ex) {
      logger.error("Configuration file could not be read. Using defaults: " + ex.getMessage());
      SERVICE_URL = DEFAULT_SERVICE_URL;
    }

    logger.info("JobManager instance created.");
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
