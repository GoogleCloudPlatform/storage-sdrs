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

package com.google.cloudy.retention;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloudy.retention.server.ServerShutdownHook;

/**
 * Main class.
 * This will be modified in future iterations.
 */
@Deprecated
public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  // Base URI the Grizzly HTTP server will listen on
  public static final String BASE_URI = "http://0.0.0.0:8080/myapp/";

  /**
   * @return Grizzly HTTP server.
   * @deprecated Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   */
  public static HttpServer startServer() {
    // create a resource config that scans for JAX-RS resources and providers
    // in com.google.cloudy.retention. packages

    ResourceConfig config =
        new ResourceConfig().packages("com.google.cloudy.retention.");

    // create and start a new instance of grizzly http server
    // exposing the Jersey application at BASE_URI
    return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
  }

  /**
   * Main method. Configures and starts the grizzly HTTP server
   *
   * @param args Standard java main method signature
   */
  public static void main(String[] args) {
    String hostName = args[0];
    logger.info("hostname from args is " + hostName);

    URI baseUri = UriBuilder.fromUri("http://" + hostName + "/").port(8080).build();

    HttpServer server = GrizzlyHttpServerFactory.createHttpServer(baseUri, new AppResourceConfig());

    try {
      HierarchicalConfiguration xmlConfig = new Configurations().xml("default-applicationConfig.xml");
      long shutdownGracePeriodInSeconds = xmlConfig.getLong("serverConfig.shutdownGracePeriodInSeconds");

      Runtime.getRuntime().addShutdownHook(new Thread(new ServerShutdownHook(server, shutdownGracePeriodInSeconds), "shutdownHook"));
    } catch (ConfigurationException ex) {
      logger.error("Unable to load settings from configuration file on server start: ", ex);
    }

    try {
      logger.info("Starting grizzly server...");
      server.start();
    } catch (Exception e) {
      logger.error("There was an error while starting the HTTP server: ", e.getCause());
    }
  }
}
