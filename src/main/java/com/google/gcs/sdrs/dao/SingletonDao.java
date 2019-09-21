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

package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.impl.DmQueueDaoImpl;
import com.google.gcs.sdrs.dao.impl.LockDaoImpl;
import com.google.gcs.sdrs.dao.impl.PooledStsJobDaoImpl;
import com.google.gcs.sdrs.dao.impl.RetentionJobDaoImpl;
import com.google.gcs.sdrs.dao.impl.RetentionJobValidationDaoImpl;
import com.google.gcs.sdrs.dao.impl.RetentionRuleDaoImpl;

/** Class to manage singleton DAO instances. */
public class SingletonDao {

  private static RetentionRuleDao retentionRuleDao;
  private static RetentionJobDao retentionJobDao;
  private static RetentionJobValidationDao retentionJobValidationDao;
  private static PooledStsJobDao pooledStsJobDao;
  private static DmQueueDao dmQueueDao;
  private static LockDao lockDao;

  public static synchronized RetentionRuleDao getRetentionRuleDao() {
    if (retentionRuleDao == null) {
      retentionRuleDao = new RetentionRuleDaoImpl();
    }
    return retentionRuleDao;
  }

  public static synchronized PooledStsJobDao getPooledStsJobDao() {
    if (pooledStsJobDao == null) {
      pooledStsJobDao = new PooledStsJobDaoImpl();
    }
    return pooledStsJobDao;
  }

  public static synchronized RetentionJobDao getRetentionJobDao() {
    if (retentionJobDao == null) {
      retentionJobDao = new RetentionJobDaoImpl();
    }
    return retentionJobDao;
  }

  public static synchronized RetentionJobValidationDao getRetentionJobValidationDao() {
    if (retentionJobValidationDao == null) {
      retentionJobValidationDao = new RetentionJobValidationDaoImpl();
    }
    return retentionJobValidationDao;
  }

  public static synchronized DmQueueDao getDmQueueDao() {
    if (dmQueueDao == null) {
      dmQueueDao = new DmQueueDaoImpl();
    }
    return dmQueueDao;
  }

  public static synchronized LockDao getLockDao() {
    if (lockDao == null) {
      lockDao = new LockDaoImpl();
    }
    return lockDao;
  }
}
