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
 */

package com.google.cloudy.retention.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import org.junit.Before;
import org.junit.Test;

import static com.google.cloudy.retention.filter.ContainerContextProperties.CORRELATION_UUID;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.matches;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
