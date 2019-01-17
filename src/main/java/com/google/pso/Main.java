package com.google.pso;

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

import com.google.pso.server.ServerShutdownHook;

/** Main class. */
@Deprecated
public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  // Base URI the Grizzly HTTP server will listen on
  public static final String BASE_URI = "http://0.0.0.0:8080/myapp/";

  /**
   * @deprecated Starts Grizzly HTTP server exposing JAX-RS resources defined in this application.
   * @return Grizzly HTTP server.
   */
  public static HttpServer startServer() {
    // create a resource config that scans for JAX-RS resources and providers
    // in com.google.pso. and com.google.cloudy.retention. packages

    ResourceConfig config =
        new ResourceConfig().packages("com.google.pso.", "com.google.cloudy.retention.");

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

    try{
      logger.info("Starting grizzly server...");
      server.start();
    } catch (Exception e) {
      logger.error("There was an error while starting the HTTP server: ", e.getCause());
    }
  }
}
