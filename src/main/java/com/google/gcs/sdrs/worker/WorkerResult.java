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

package com.google.gcs.sdrs.worker;

import java.util.UUID;
import org.joda.time.DateTime;

/** WorkerResult class to standardize worker output */
public class WorkerResult {

  private String id;
  private String type;
  private WorkerResultStatus status;
  private DateTime startTime;
  private DateTime endTime;

  /** An enum of valid WorkerResult status values */
  public enum WorkerResultStatus {
    RUNNING,
    SUCCESS,
    FAILED
  }

  /** A constructor for the WorkerResult object */
  public WorkerResult() {
    id = UUID.randomUUID().toString();
  }

  /**
   * toString method for the WorkerResult.
   *
   * @return string representation of the WorkerResult.
   */
  @Override
  public String toString() {
    return String.format(
        "id: %1s, type: %2s, status: %3s, startTime: %4s, endTime: %5s",
        id, type, String.valueOf(status), String.valueOf(startTime), String.valueOf(endTime));
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public WorkerResultStatus getStatus() {
    return status;
  }

  public void setStatus(WorkerResultStatus status) {
    this.status = status;
  }

  public DateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(DateTime startTime) {
    this.startTime = startTime;
  }

  public DateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(DateTime endTime) {
    this.endTime = endTime;
  }
}
