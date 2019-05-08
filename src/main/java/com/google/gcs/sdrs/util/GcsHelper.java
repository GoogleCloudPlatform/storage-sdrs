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

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Preconditions;
import com.google.api.services.storage.Storage;
import com.google.api.services.storage.StorageScopes;
import com.google.api.services.storage.model.Bucket;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper for using GCS API */
public class GcsHelper {

  private Storage stroageClient;
  private static GcsHelper instance;
  private static final Logger logger = LoggerFactory.getLogger(GcsHelper.class);

  private GcsHelper() throws IOException {
    stroageClient = createStorageClient(CredentialsUtil.getInstance().getCredentials());
  }

  public static synchronized GcsHelper getInstance() {
    if (instance == null) {
      try {
        instance = new GcsHelper();
      } catch (IOException ex) {
        logger.error("Could not establish connection with GCS: ", ex.getMessage());
        logger.error("Underlying error: ", ex.getCause().getMessage());
      }
    }
    return instance;
  }

  public Bucket getBucket(String bucketName) {
    Bucket bucket = null;
    try {
      Storage.Buckets.Get request = stroageClient.buckets().get(bucketName);
      bucket = request.execute();
    } catch (IOException e) {
      logger.error("Failed to get GCS bucket");
    }

    return bucket;
  }

  /** Creates an instance of the GCS Client */
  private Storage createStorageClient(GoogleCredential credential) {
    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
    return createStorageClient(httpTransport, jsonFactory, credential);
  }

  private Storage createStorageClient(
      HttpTransport httpTransport, JsonFactory jsonFactory, GoogleCredential credential) {

    Preconditions.checkNotNull(httpTransport);
    Preconditions.checkNotNull(jsonFactory);
    Preconditions.checkNotNull(credential);

    // In some cases, you need to add the scope explicitly.
    if (credential.createScopedRequired()) {
      Set<String> scopes = new HashSet<>();
      scopes.addAll(StorageScopes.all());
      credential = credential.createScoped(scopes);
    }

    HttpRequestInitializer initializer = new RetryHttpInitializerWrapper(credential, false);
    return new Storage.Builder(httpTransport, jsonFactory, initializer)
        .setApplicationName("sdrs")
        .build();
  }
}
