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
import static org.junit.Assert.assertNull;
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
  public void filterAddsEmptyUserInfoWhenNoHeaderPresent() {
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    ContainerRequestContext mockRequestContext = spy(ContainerRequestContext.class);
    when(mockRequestContext.getHeaders()).thenReturn(requestHeaders);

    filter.filter(mockRequestContext);

    ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
    verify(mockRequestContext)
        .setProperty(eq(ContainerContextProperties.USER_INFO.toString()), captor.capture());
    UserInfo userInfo = captor.getValue();
    assertNull(userInfo.getEmail());
  }

  @Test
  public void filterCreatesUserInfo() {
    MultivaluedMap<String, String> requestHeaders = new MultivaluedHashMap<>();
    requestHeaders.add(
        "Authorization",
        "Bearer eyJ0eXAiOiAiSldUIiwgImFsZyI6ICJSUzI1NiIsICJraWQiOiAiODE2MmU1ZGJkZWUzZDk2MWZhYWFj"
            + "NzVjYjZmODdiYWM0MDVhZGJiNyJ9.eyJpYXQiOiAxNTUwODU4NDUyLCAiZXhwIjogMTU1MDg2MjA1MiwgIm"
            + "lzcyI6ICJ0Zmxlbm5pa2VuQHNkcnMtc2VydmVyLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwgImF1ZCI6I"
            + "CJodHRwczovL3NkcnMtYXBpLmVuZHBvaW50cy5zZHJzLXNlcnZlci5jbG91ZC5nb29nIiwgInN1YiI6ICJ0"
            + "Zmxlbm5pa2VuQHNkcnMtc2VydmVyLmlhbS5nc2VydmljZWFjY291bnQuY29tIiwgImVtYWlsIjogInRmbGV"
            + "ubmlrZW5Ac2Rycy1zZXJ2ZXIuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20ifQ==.CKNx-3FguEv2OSoGwWvLu"
            + "MHpFPd9jY3HoQ86Jxb0UZxGx2WDlhLgCTSi0ruZ5eINC3EJr3qJ9zv3MFmEnA4DGk1nFJdzXJfD-VL5Q56o"
            + "W-WjleL1ZiPRMwcoSdfjRVsmr15tb0Y10Z1DWFWNNSo8sUvfRkOqnaZCx1vm-yLH3t5B7XqWZCuAE-TkX2i"
            + "KYnValkrqVlgiqDkuMCmiehIHcogMB7DjZKStRPlxOmm_RfT--Pj_o_Fax47jNoDxfpQiGmUO1Zqe33dJkf"
            + "KHC96spwlc7p8ulUrpzIDHlL0Ek7da88pAWRIf24ojrhYVm6ldmwvzR8ZW-i-CZyl9wHyQrA==");

    ContainerRequestContext mockRequestContext = spy(ContainerRequestContext.class);
    when(mockRequestContext.getHeaders()).thenReturn(requestHeaders);

    filter.filter(mockRequestContext);

    ArgumentCaptor<UserInfo> captor = ArgumentCaptor.forClass(UserInfo.class);
    verify(mockRequestContext)
        .setProperty(eq(ContainerContextProperties.USER_INFO.toString()), captor.capture());
    UserInfo userInfo = captor.getValue();
    assertEquals("tflenniken@sdrs-server.iam.gserviceaccount.com", userInfo.getEmail());
  }
}
