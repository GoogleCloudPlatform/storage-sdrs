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
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import java.io.IOException;

public class CredentialsUtil {

  private static CredentialsUtil instance;

  private CredentialsUtil() {}

  public static CredentialsUtil getInstance() {
    if (instance == null) {
      instance = new CredentialsUtil();
    }
    return instance;
  }

  public GoogleCredential getCredentials() throws IOException {
    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
    GoogleCredential credential =
        GoogleCredential.getApplicationDefault(httpTransport, jsonFactory);
    return credential;
  }
}
