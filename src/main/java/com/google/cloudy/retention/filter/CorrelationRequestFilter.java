package com.google.cloudy.retention.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;
import java.util.UUID;

import static com.google.cloudy.retention.filter.ContainerContextProperties.CORRELATION_UUID;

/**
 * Extracts the correlation-uuid from the HTTP headers to make it available to the application
 */
@Provider
public class CorrelationRequestFilter implements ContainerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(CorrelationRequestFilter.class);

  /**
   * Adds the correlation_uuid to the RequestContext properties, creating it if it doesn't exist.
   */
  @Override
  public void filter(ContainerRequestContext context) {
    String correlationUuid = context.getHeaders().getFirst("correlation-uuid");
    if (correlationUuid == null) {
      correlationUuid = UUID.randomUUID().toString();
      logger.info(String.format("Generating new Correlation UUID: %s", correlationUuid));
    }
    context.setProperty(CORRELATION_UUID.toString(), correlationUuid);
  }
}
