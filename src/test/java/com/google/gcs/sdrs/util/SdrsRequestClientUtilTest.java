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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Random;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SdrsRequestClientUtilTest {

  private Client client;
  private Invocation.Builder builder;
  private WebTarget webTarget;

  @Before
  public void setup() throws IOException {
    client = mock(Client.class, Mockito.RETURNS_DEEP_STUBS);
    builder = mock(Invocation.Builder.class);
    webTarget = mock(WebTarget.class);

    RSAPrivateKey privateKey = mock(RSAPrivateKey.class);
    when(privateKey.getAlgorithm()).thenReturn("RSA");

    Random random = new Random();
    when(privateKey.getModulus()).thenReturn(new BigInteger(512, random));
    when(privateKey.getPrivateExponent()).thenReturn(new BigInteger(512, random));

    MockGoogleCredential.Builder credentialBuilder = new MockGoogleCredential.Builder();
    credentialBuilder.setServiceAccountPrivateKey(privateKey);
    credentialBuilder.setServiceAccountId("test_service_account_id@google.com");
    GoogleCredential testCredential = new MockGoogleCredential(credentialBuilder);
    SdrsRequestClientUtil.credentialsUtil = mock(CredentialsUtil.class);
    when(SdrsRequestClientUtil.credentialsUtil.getCredentials()).thenReturn(testCredential);

    when(client.target(anyString())).thenReturn(webTarget);
    when(webTarget.queryParam(anyString(), any())).thenReturn(webTarget);
    when(webTarget.path(anyString())).thenReturn(webTarget);
    when(webTarget.request()).thenReturn(builder);
    when(builder.header(anyString(), anyString())).thenReturn(builder);
  }

  @Test
  public void bearerAuthHeaderInRequest() {
    SdrsRequestClientUtil.request(client, "something").post(null);

    verify(builder).header(eq("Authorization"), startsWith("Bearer ey"));
  }

  @Test
  public void usesConfiguredProtocolAndServiceUrl() {
    SdrsRequestClientUtil.serviceUrl = "url";
    SdrsRequestClientUtil.protocol = "http";
    SdrsRequestClientUtil.port = "80";

    SdrsRequestClientUtil.request(client, "something").post(null);

    verify(client).target(eq("http://url:80"));
  }

  @Test
  public void usesPath() {
    SdrsRequestClientUtil.request(client, "something").post(null);

    verify(webTarget).path(eq("something"));
  }
}
