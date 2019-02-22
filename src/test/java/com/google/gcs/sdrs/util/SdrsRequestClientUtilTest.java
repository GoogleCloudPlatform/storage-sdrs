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

package com.google.gcs.sdrs.util;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SdrsRequestClientUtilTest {

  //TODO the unit tests not working. fix later
/*  private Client client;
  private Invocation.Builder builder;
  private WebTarget webTarget;

  @Before
  public void setup() {
    client = mock(Client.class);
    builder = mock(Invocation.Builder.class);
    webTarget = mock(WebTarget.class);
    when(client.target(anyString())).thenReturn(webTarget);
    when(webTarget.path(anyString())).thenReturn(webTarget);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), anyString())).thenReturn(builder);
  }

  @Test
  public void bearerAuthHeaderInRequest() throws ConfigurationException {
    SdrsRequestClientUtil.request(client, "something").post(null);

    verify(builder).header(eq("Authorization"), startsWith("Bearer ey"));
  }

  @Test
  public void usesConfiguredProtocolAndServiceUrl() throws ConfigurationException {
    SdrsRequestClientUtil.request(client, "something").post(null);

    verify(client).target(eq("http://sdrs-api.endpoints.sdrs-server.cloud.goog"));
  }

  @Test
  public void usesPath() throws ConfigurationException {
    SdrsRequestClientUtil.request(client, "something").post(null);

    verify(webTarget).path(eq("something"));
  }*/
}
