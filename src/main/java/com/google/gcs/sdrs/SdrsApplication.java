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

import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.impl.RetentionRuleDaoImpl;
import com.google.gcs.sdrs.dao.model.DistributedLock;
import com.google.gcs.sdrs.scheduler.JobScheduler;
import com.google.gcs.sdrs.scheduler.runners.DMBatchProcessingRunner;
import com.google.gcs.sdrs.service.mq.PubSubMessageQueueManagerImpl;
import com.google.gcs.sdrs.service.worker.impl.DmBatchProcessingWorker;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.logging.LogManager;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/** The main startup class for the SDRS service. */
public class SdrsApplication {

  private static final Logger logger = LoggerFactory.getLogger(SdrsApplication.class);

  private static HttpServer server;
  private static Configuration xmlConfig;

  static {
    LogManager.getLogManager().reset();
    SLF4JBridgeHandler.install();
  }

  public static final int DEFAULT_DM_RUNNER_INITIAL_DELAY = 0;
  public static final int DEFAULT_DM_RUNNER_FREQUENCY = 60;
  public static final TimeUnit DEFAULT_DM_RUNNER_TIMEUNIT = TimeUnit.MINUTES;

  /**
   * Starts the SDRS service
   *
   * @param args Application startup arguments
   */
  public static void main(String[] args) {
    logger.info("Starting SDRS...");

    getAppConfig();

    // if web server fails to start, consider it a fatal error and exit the application with error
    startWebServer();
    registerPubSub();
    connectDatabase();
    initDmDistributedLock();
    scheduleDmProcessingRunner();

    /*   if (Boolean.valueOf(getAppConfigProperty("scheduler.enabled", "false"))) {
      scheduleExecutionServiceJob();
      scheduleValidationServiceJob();
    }*/
  }

  /** Triggers the shutdown hook and gracefully shuts down the SDRS service */
  static void shutdown() {
    System.exit(0);
  }

  /** Loads necessary configurations and starts the web server */
  private static void startWebServer() {
    try {

      // Read config values
      Boolean useHttps = xmlConfig.getBoolean("serverConfig.useHttps");
      String hostName = xmlConfig.getString("serverConfig.address");
      int port = xmlConfig.getInt("serverConfig.port");
      long shutdownGracePeriodInSeconds =
          xmlConfig.getLong("serverConfig.shutdownGracePeriodInSeconds");

      // Build server URI
      URI baseUri =
          UriBuilder.fromUri((useHttps ? "https://" : "http://") + hostName + "/")
              .port(port)
              .build();

      server = GrizzlyHttpServerFactory.createHttpServer(baseUri, new AppResourceConfig());

      // Register shutdown hook so the monitoring thread is killed when the app is stopped
      Runtime.getRuntime()
          .addShutdownHook(
              new Thread(
                  new ServerShutdownHook(server, shutdownGracePeriodInSeconds, false),
                  "shutdownHook"));

      server.start();
      logger.info("SDRS Web Server Started.");
    } catch (IOException ex) {
      logger.error("An error occurred during web server start up: " + ex.getCause());
      if (server != null && server.isStarted()) {
        server.shutdownNow();
      }
      System.exit(1);
    }
  }

  private static void initDmDistributedLock() {
    LockDao daoDao = SingletonDao.getLockDao();
    DistributedLock distributedLock = daoDao.initLock(DmBatchProcessingWorker.DM_LOCK_ID);
    if (distributedLock == null) {
      logger.error(
          String.format(
              "Failed to initialize distributed lock for DM batch processing %s",
              DmBatchProcessingWorker.DM_LOCK_ID));
    } else {
      logger.info(
          String.format(
              "DM batch processing lock has been initialized:  tokenName=%s, duration=%d, createdAt=%s",
              distributedLock.getLockToken(),
              distributedLock.getLockDuration(),
              distributedLock.getCreatedAt().toInstant().toString()));
    }
  }

  private static void scheduleDmProcessingRunner() {
    JobScheduler scheduler = JobScheduler.getInstance();

    int initialDelay =
        Integer.valueOf(
            getAppConfigProperty(
                "scheduler.task.dmBatchProcessing.initialDelay",
                String.valueOf(DEFAULT_DM_RUNNER_INITIAL_DELAY)));
    int frequency =
        Integer.valueOf(
            getAppConfigProperty(
                "scheduler.task.dmBatchProcessing.frequency",
                String.valueOf(DEFAULT_DM_RUNNER_FREQUENCY)));
    TimeUnit timeUnit =
        TimeUnit.valueOf(
            getAppConfigProperty(
                "scheduler.task.dmBatchProcessing.timeUnit", DEFAULT_DM_RUNNER_TIMEUNIT.name()));

    scheduler.submitScheduledJob(new DMBatchProcessingRunner(), initialDelay, frequency, timeUnit);
    logger.info("DM batch processing runner scheduled successfully.");
  }

  public static Configuration getAppConfig() {
    if (xmlConfig == null) {
      try {
        xmlConfig = new Configurations().xml("applicationConfig.xml");
      } catch (ConfigurationException ex) {
        logger.error("Failed to load applicationConfig.xml");
      }
    }
    return xmlConfig;
  }

  public static String getAppConfigProperty(String key) {
    return getAppConfigProperty(key, null);
  }

  public static String getAppConfigProperty(String key, String defaultValue) {
    Configuration config = getAppConfig();
    String propertyValue = config.getString(key);
    if (isPropertyValueToken(propertyValue)) {
      // get property value from environment if the value is a replacement token
      propertyValue = getPropertyValueFromEnv(propertyValue);
    }
    if (propertyValue == null) {
      propertyValue = defaultValue;
    }
    return propertyValue;
  }

  private static boolean isPropertyValueToken(String value) {
    // config property token is included in ${}. i.e ${PUBSUB_TOPIC}
    return value != null && value.startsWith("${");
  }

  private static String getPropertyValueFromEnv(String token) {
    String envVariable = token.substring(2, token.length() - 1);
    return System.getenv(envVariable);
  }

  private static void registerPubSub() {
    if (PubSubMessageQueueManagerImpl.getInstance().getPublisher() == null) {
      logger.error("Failed to register PubSub topic");
    } else {
      logger.info("PubSub topic registered");
    }
  }

  private static void connectDatabase() {
    RetentionRuleDaoImpl retentionRuleDao = new RetentionRuleDaoImpl();
    retentionRuleDao.findGlobalRuleByProjectId("");
    if (retentionRuleDao.isSessionFactoryAvailable()) {
      logger.info("Database is connected");
    } else {
      logger.error("Failed to connect to database");
    }
  }
}
