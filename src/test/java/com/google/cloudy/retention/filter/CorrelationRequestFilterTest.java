package com.google.cloudy.retention.filter;

import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import static com.google.cloudy.retention.filter.ContainerContextProperties.CORRELATION_UUID;
import static org.mockito.Mockito.*;

public class CorrelationRequestFilterTest {

  private CorrelationRequestFilter filter;

  @Before
  public void setup() {
    filter = new CorrelationRequestFilter();
  }

  @Test
  public void filterAddsCorrelationToContextProperty() {
    String correlation = "12345";
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    requestHeaders.add("correlation-uuid", correlation);
    ContainerRequestContext mockRequestContext = spy(ContainerRequestContext.class);
    when(mockRequestContext.getHeaders()).thenReturn(requestHeaders);

    filter.filter(mockRequestContext);

    verify(mockRequestContext).setProperty(CORRELATION_UUID.toString(), correlation);
  }

  @Test
  public void filterCreatesCorrelationWhenMissing() {
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    ContainerRequestContext mockRequestContext = spy(ContainerRequestContext.class);
    when(mockRequestContext.getHeaders()).thenReturn(requestHeaders);

    filter.filter(mockRequestContext);

    verify(mockRequestContext)
        .setProperty(
            eq(CORRELATION_UUID.toString()),
            // UUID regex
            matches("[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}"));
  }
}
