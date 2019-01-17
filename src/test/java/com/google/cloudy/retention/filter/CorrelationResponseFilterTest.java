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
