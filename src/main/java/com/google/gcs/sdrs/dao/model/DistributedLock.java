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
package com.google.gcs.sdrs.dao.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "distributed_lock")
public class DistributedLock {
  @Id
  @Column(name = "id", updatable = false, nullable = false)
  private String id;

  @Column(name = "lock_token", nullable = false)
  private String LockToken;

  @Column(name = "lock_duration", nullable = false)
  private long lockDuration;

  @Column(name = "created_at", nullable = false)
  private Timestamp createdAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getLockToken() {
    return LockToken;
  }

  public void setLockToken(String lockToken) {
    LockToken = lockToken;
  }

  public long getLockDuration() {
    return lockDuration;
  }

  public void setLockDuration(long lockDuration) {
    this.lockDuration = lockDuration;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
  }
}
