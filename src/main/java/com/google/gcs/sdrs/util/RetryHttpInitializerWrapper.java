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
 *
 */

package com.google.gcs.sdrs.util;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler.BackOffRequired;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.client.util.Sleeper;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RetryHttpInitializerWrapper will automatically retry upon RPC failures, preserving the
 * auto-refresh behavior of the Google Credentials.
 */
public class RetryHttpInitializerWrapper implements HttpRequestInitializer {

  private static final Logger logger = LoggerFactory.getLogger(RetryHttpInitializerWrapper.class);
  private final Credential wrappedCredential;
  private final Sleeper sleeper;
  private boolean backOffRequiredAlways;
  private static final int MILLIS_PER_MINUTE = 60 * 1000;

  /**
   * A constructor using the default Sleeper.
   *
   * @param wrappedCredential the credential used to authenticate with a Google Cloud Platform
   *     project
   */
  public RetryHttpInitializerWrapper(Credential wrappedCredential) {
    this(wrappedCredential, Sleeper.DEFAULT, false);
  }

  /**
   * A constructor used only for testing.
   *
   * @param wrappedCredential the credential used to authenticate with a Google Cloud Platform
   *     project
   * @param sleeper a user-supplied Sleeper
   */
  RetryHttpInitializerWrapper(
      Credential wrappedCredential, Sleeper sleeper, boolean backOffRequiredAlways) {
    this.wrappedCredential = Preconditions.checkNotNull(wrappedCredential);
    this.sleeper = sleeper;
    this.backOffRequiredAlways = backOffRequiredAlways;
  }

  /**
   * A constructor using default Sleeper and allow back off retry setting
   *
   * @param wrappedCredential the credential used to authenticate with a Google Cloud Platform
   *     project
   * @param backOffRequiredAlways whether or not exponential backoff retry is always on or for sever
   *     error only
   */
  public RetryHttpInitializerWrapper(Credential wrappedCredential, boolean backOffRequiredAlways) {
    this(wrappedCredential, Sleeper.DEFAULT, backOffRequiredAlways);
  }

  /**
   * Initialize an HttpRequest.
   *
   * @param request an HttpRequest that should be initialized
   */
  public void initialize(HttpRequest request) {
    request.setReadTimeout(2 * MILLIS_PER_MINUTE); // 2 minutes read timeout
    final HttpUnsuccessfulResponseHandler backoffHandler =
        new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()).setSleeper(sleeper);
    if (backOffRequiredAlways) {
      ((HttpBackOffUnsuccessfulResponseHandler) backoffHandler)
          .setBackOffRequired(BackOffRequired.ALWAYS);
    }
    request.setInterceptor(wrappedCredential);
    request.setUnsuccessfulResponseHandler(
        (final HttpRequest unsuccessfulRequest,
            final HttpResponse response,
            final boolean supportsRetry) -> {
          if (wrappedCredential.handleResponse(unsuccessfulRequest, response, supportsRetry)) {
            // If credential decides it can handle it, the return code or message indicated
            // something specific to authentication, and no backoff is desired.
            return true;
          } else if (backoffHandler.handleResponse(unsuccessfulRequest, response, supportsRetry)) {
            // Otherwise, we defer to the judgement of our internal backoff handler.
            logger.info("Retrying " + unsuccessfulRequest.getUrl().toString());
            return true;
          } else {
            return false;
          }
        });

    request.setIOExceptionHandler(
        new HttpBackOffIOExceptionHandler(new ExponentialBackOff()).setSleeper(sleeper));
  }
}
