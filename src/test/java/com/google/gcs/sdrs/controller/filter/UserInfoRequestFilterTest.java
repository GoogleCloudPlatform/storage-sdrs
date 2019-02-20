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

package com.google.gcs.sdrs.controller.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserInfoRequestFilterTest {

  private UserInfoRequestFilter filter;

  @Before
  public void setup() {
    filter = new UserInfoRequestFilter();
  }

  @Test
  public void filterAbortsWith403WhenNoHeaderPresent() {
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    ContainerRequestContext mockRequestContext = spy(ContainerRequestContext.class);
    when(mockRequestContext.getHeaders()).thenReturn(requestHeaders);

    filter.filter(mockRequestContext);

    ArgumentCaptor<Response> captor = ArgumentCaptor.forClass(Response.class);
    verify(mockRequestContext).abortWith(captor.capture());
    Response response = captor.getValue();
    assertEquals(403, response.getStatus());
  }

  @Test
  public void filterCreatesUserInfo() {
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    requestHeaders.add(
        "X-Endpoint-API-UserInfo",
        "ewogICJpc3N1ZXIiOiBUT0tFTl9JU1NVRVIsCiAgImlkI"
            + "jogVVNFUl9JRCwKICAiZW1haWwiIDogVVNFUl9FTUFJTAp9");

    ContainerRequestContext mockRequestContext = spy(ContainerRequestContext.class);
    when(mockRequestContext.getHeaders()).thenReturn(requestHeaders);

    filter.filter(mockRequestContext);

    ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
    verify(mockRequestContext)
        .setProperty(eq(ContainerContextProperties.USER_INFO.toString()), captor.capture());
    UserInfo userInfo = captor.getValue();
    assertEquals("USER_EMAIL", userInfo.getEmail());
    assertEquals("TOKEN_ISSUER", userInfo.getIssuer());
    assertEquals("USER_ID", userInfo.getId());
  }
}
