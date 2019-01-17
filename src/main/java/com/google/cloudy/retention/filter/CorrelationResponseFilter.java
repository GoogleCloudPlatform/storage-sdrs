package com.google.cloudy.retention.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import static com.google.cloudy.retention.filter.ContainerContextProperties.CORRELATION_UUID;

/**
 * Includes the correlation-uuid in the response headers.
 */
@Provider
public class CorrelationResponseFilter implements ContainerResponseFilter {

  /**
   * Adds the correlation-uuid to the ResponseContext headers object
   */
  @Override
  public void filter(ContainerRequestContext requestContext,
                     ContainerResponseContext responseContext) {
    String correlationUuid = (String) requestContext.getProperty(CORRELATION_UUID.toString());
    responseContext.getHeaders().add("correlation-uuid", correlationUuid);
  }
}
