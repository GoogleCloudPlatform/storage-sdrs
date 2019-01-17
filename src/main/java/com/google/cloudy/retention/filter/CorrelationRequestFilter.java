package com.google.cloudy.retention.filter;

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

  /**
   * Adds the correlation_uuid to the RequestContext properties, creating it if it doesn't exist.
   */
  @Override
  public void filter(ContainerRequestContext context) {
    String correlationUuid = context.getHeaders().getFirst("correlation-uuid");
    if (correlationUuid == null) {
      correlationUuid = UUID.randomUUID().toString();
    }
    context.setProperty(CORRELATION_UUID.toString(), correlationUuid);
  }
}
