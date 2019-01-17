package com.google.pso;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("resources")
public class AppResourceConfig extends ResourceConfig {
  public AppResourceConfig() {
    packages("com.google.pso", "com.google.cloudy.retention");

    register(
        new LoggingFeature(
            Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME),
            Level.INFO,
            LoggingFeature.Verbosity.PAYLOAD_TEXT,
            100000));

    // create custom ObjectMapper
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
    mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

    // create JsonProvider to provide custom ObjectMapper
    JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
    jacksonProvider.setMapper(mapper);

    // register jackson to be json provider
    // https://stackoverflow.com/questions/18317927/force-glassfish4-to-use-jackson-instead-of-moxy#18318314
    register(jacksonProvider);
  }
}
