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

package com.google.gcs.sdrs;

import com.google.gcs.sdrs.server.ServerShutdownHook;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;

/**
 * The main startup class for the SDRS service.
 */
public class SdrsApplication {

  private static final Logger logger = LoggerFactory.getLogger(SdrsApplication.class);

  private static HttpServer server;

  /**
   * Starts the SDRS service
   * @param args Application startup arguments
   */
  public static void main(String[] args) {
    logger.info("Starting SDRS...");
    startWebServer();
  }

  /**
   * Triggers the shutdown hook and gracefully shuts down the SDRS service
   */
  static void shutdown() {
    System.exit(0);
  }

  /**
   * Loads necessary configurations and starts the web server
   */
  private static void startWebServer() {
    try {

      // Read config values
      Configuration xmlConfig =
          new Configurations().xml("applicationConfig.xml");
      Boolean useHttps = xmlConfig.getBoolean("serverConfig.useHttps");
      String hostName = xmlConfig.getString("serverConfig.address");
      int port = xmlConfig.getInt("serverConfig.port");
      long shutdownGracePeriodInSeconds = xmlConfig
          .getLong("serverConfig.shutdownGracePeriodInSeconds");

      // Build server URI
      URI baseUri = UriBuilder.fromUri((useHttps ? "https://" : "http://") + hostName + "/")
          .port(port).build();

      server = GrizzlyHttpServerFactory.createHttpServer(baseUri, new AppResourceConfig());

      // Register shutdown hook so the monitoring thread is killed when the app is stopped
      Runtime.getRuntime().addShutdownHook(new Thread(
          new ServerShutdownHook(server, shutdownGracePeriodInSeconds), "shutdownHook"));

      server.start();
      logger.info("SDRS Web Server Started.");
    } catch (ConfigurationException ex) {
      logger.error("The server could not start because the configuration file could not be read: "
          + ex.getCause());
    } catch (IOException ex) {
      logger.error("An error occurred during web server start up: " + ex.getCause());
    }
  }

  private static void createDatabaseConnection() {
    // TODO Register database connection once data layer is complete
  }

  private static void registerPubSub() {
    // TODO Create pubsub connection once pub sub utility is complete
  }
}
