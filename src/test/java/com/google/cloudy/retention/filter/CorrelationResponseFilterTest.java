package com.google.cloudy.retention.filter;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static com.google.cloudy.retention.filter.ContainerContextProperties.CORRELATION_UUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CorrelationResponseFilterTest {

  private CorrelationResponseFilter filter;

  @Before
  public void setup() {
    filter = new CorrelationResponseFilter();
  }

  @Test
  public void filterAddsCorrelationToHeader() {
    String correlation = "12345";
    ContainerRequestContext mockRequestContext = mock(ContainerRequestContext.class);
    when(mockRequestContext.getProperty(CORRELATION_UUID.toString())).thenReturn(correlation);
    ContainerResponseContext mockResponseContext = mock(ContainerResponseContext.class);
    MultivaluedMap<String, Object> responseHeaders = new MultivaluedHashMap<>();
    when(mockResponseContext.getHeaders()).thenReturn(responseHeaders);

    filter.filter(mockRequestContext, mockResponseContext);

    assertEquals(responseHeaders.getFirst("correlation-uuid"), correlation);
  }
}
