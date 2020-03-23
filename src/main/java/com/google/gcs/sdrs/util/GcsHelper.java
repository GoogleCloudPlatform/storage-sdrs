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

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper for using GCS API */
public class GcsHelper {

  private Storage storage;
  private static GcsHelper instance;
  private static final Logger logger = LoggerFactory.getLogger(GcsHelper.class);

  private GcsHelper() throws IOException {
    GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
    StorageOptions storageOptions = StorageOptions.newBuilder().setCredentials(credentials).build();
    storage = storageOptions.getService();
    if (storage == null) {
      throw new IOException("Failed to create GCS client.");
    }
  }

  public static synchronized GcsHelper getInstance() {
    if (instance == null) {
      try {
        instance = new GcsHelper();
      } catch (IOException e) {
        logger.error("Could not establish connection with GCS: ", e);
      }
    }
    return instance;
  }

  public boolean doesBucketExist(String bucketName, String projectId) {
    if (bucketName == null || projectId == null) {
      return false;
    }

    Bucket bucket = storage.get(bucketName);
    return bucket != null && bucket.getStorage().getOptions().getProjectId().equals(projectId);
  }

  public Bucket getBucket(String bucketName) {
    return storage.get(bucketName);
  }

  public List<String> listObjectsWithPrefixInBucket(String bucketName, String prefix) {
    Page<Blob> blobs =
        storage.list(
            bucketName, BlobListOption.currentDirectory(), BlobListOption.prefix(prefix));
    List<String> objectsPath = new ArrayList<>();
    for (Blob blob: blobs.iterateAll()) {
      //only record for  directories
      if (blob.isDirectory()) {
        objectsPath.add(blob.getName());
      }
    }
    return objectsPath;
  }
}
