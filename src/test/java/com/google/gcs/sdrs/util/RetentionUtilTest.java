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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RetentionUtilTest {

  private String dataStorageName = "gs://test";
  private String expectedBucketName = "test";
  String suffix = "shadow";

  @Test
  public void getBucketNameRootTest(){
    String result = RetentionUtil.getBucketName(dataStorageName);
    assertEquals(expectedBucketName, result);
  }

  @Test
  public void getBucketNameTrailingSlashTest(){
    dataStorageName = dataStorageName.concat("/");
    String result = RetentionUtil.getBucketName(dataStorageName);

    assertEquals(expectedBucketName, result);
  }

  @Test
  public void getBucketNameWithPathTest(){
    dataStorageName = dataStorageName.concat("/test/myLog");
    String result = RetentionUtil.getBucketName(dataStorageName);

    assertEquals(expectedBucketName, result);
  }

  @Test
  public void getBucketNameNullTest(){
    String result = RetentionUtil.getBucketName(null);

    assertEquals("", result);
  }

  @Test
  public void getBucketNameWithSuffix(){
    String result = RetentionUtil.getBucketName(dataStorageName, suffix);;

    assertEquals(expectedBucketName.concat(suffix), result);
  }

  @Test
  public void getBucketNameWithSuffixAndTrailingSlash(){
    dataStorageName = dataStorageName.concat("/");
    String result = RetentionUtil.getBucketName(dataStorageName, suffix);

    assertEquals(expectedBucketName.concat(suffix), result);
  }

  @Test
  public void getBucketNameWithSuffixAndPathTest(){
    dataStorageName = dataStorageName.concat("/dataset/myLog");
    String result = RetentionUtil.getBucketName(dataStorageName, suffix);

    assertEquals(expectedBucketName.concat(suffix), result);
  }

  @Test
  public void getDatasetPathTest(){
    String path = "/dataset/myLog";
    dataStorageName = dataStorageName.concat(path);

    String result = RetentionUtil.getDatasetPath(dataStorageName);
    String expected = path.replaceFirst("/", "");

    assertEquals(expected, result);
  }

  @Test
  public void getDatasetPathBucketOnlyTest(){
    String result = RetentionUtil.getDatasetPath(dataStorageName);

    assertEquals("", result);
  }

  @Test
  public void getDatasetPathTrailingSlash(){
    String result = RetentionUtil.getDatasetPath(dataStorageName.concat("/"));

    assertEquals("", result);
  }

  @Test
  public void getDatasetPathNull(){
    String result = RetentionUtil.getDatasetPath(null);

    assertEquals("", result);
  }

  @Test
  public void getDatasetPathSameName(){
    String expected = "test";
    String fullPath = dataStorageName + "/" + expected;
    String result = RetentionUtil.getDatasetPath(fullPath);

    assertEquals(expected, result);
  }
}
