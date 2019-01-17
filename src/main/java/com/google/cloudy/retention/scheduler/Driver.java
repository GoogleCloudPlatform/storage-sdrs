package com.google.cloudy.retention.scheduler;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.HierarchicalConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.cloudy.retention.scheduler.command.Command;
import com.google.cloudy.retention.server.AsyncSocketServer;
import com.google.cloudy.retention.service.dataaccess.SqlDbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Sample driver class for this POC demonstrating how to wire-up all the components.
 *
 */
@Deprecated
public class Driver {

  private static final Logger logger = LoggerFactory.getLogger(Driver.class);

  public static void main( String[] args ) throws ConfigurationException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, FileNotFoundException, IOException {

    Configurations configs = new Configurations();

    HierarchicalConfiguration config = configs.xml("default-applicationConfig.xml");
    int sleepMins = config.getInt("serverConfig.sleepMins");
    SqlDbManager sqlDbManager = instantiateDB(config);
    GoogleCredential googleCredential = getGCPCredential(config.getString("gcp.pathToJsonCredential"));
    JobScheduler jobScheduler = JobScheduler.getInstance(config.getInt("executorService.threadPoolSize"),sleepMins, sqlDbManager);
    jobScheduler.setProjectID( config.getString("gcp.projectID")); //stopgap
    jobScheduler.setGoogleCredential(googleCredential); //stopgap TODO address this design consideration
    List<HierarchicalConfiguration> commands = config.configurationsAt("commands.command");
    for(HierarchicalConfiguration command: commands) {
      final String key = command.getString("name").trim();
      String type = command.getString("type").trim();
      jobScheduler.commandMap.put(key,(Command)
          Class.forName(type).getConstructor(JobScheduler.class).newInstance(jobScheduler));
    }
    try {
      new AsyncSocketServer(config.getString("serverConfig.address"),
          config.getInt("serverConfig.port"),config.getString("scheduler.shutdownCommand"),jobScheduler);
      while (true) {
        TimeUnit.MINUTES.sleep(sleepMins);
      }
    } catch (Exception ex) {
      logger.error(ex.toString());
    }
  }

  private static GoogleCredential getGCPCredential(String absPathToJsonFile) throws FileNotFoundException, IOException {

    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
    GoogleCredential credential =
        GoogleCredential.fromStream(new FileInputStream(absPathToJsonFile), httpTransport, jsonFactory);

    return credential;
  }
  private static SqlDbManager instantiateDB(HierarchicalConfiguration config) {
    return SqlDbManager.getInstance(config.getString("database.host"),
        config.getString("database.schema"), config.getString("database.user"),
        config.getString("database.password"), config.getInt("database.poolSize"));
  }
}
