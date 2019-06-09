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

package com.google.gcs.sdrs.controller;

import com.google.gcs.sdrs.dao.BaseDao;
import com.google.gcs.sdrs.service.mq.PubSubMessageQueueManagerImpl;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Controller for providing app status. */
@Path("/status")
public class AppStatusController extends BaseController{

  private static final Logger logger = LoggerFactory.getLogger(AppStatusController.class);

  @GET
  @Produces(MediaType.TEXT_PLAIN)
  public String getAppStatus() {
    return doGetAppStatus();
  }

  private String doGetAppStatus() {
    StringBuilder builder = new StringBuilder();
    builder.append("Web server is UP\n");
    if (isDatabaseLive()) {
      builder.append("Database is UP\n");
    } else {
      builder.append("Database is DOWN\n");
      logger.error("Database is down ...");
    }

    if (isPubSubLive()) {
      builder.append("PubSub is UP\n");
    } else {
      builder.append("PubSub is DOWN\n");
      logger.error("PubSub topic unreachable ...");
    }
    return builder.toString();
  }

  private boolean isPubSubLive() {
    return PubSubMessageQueueManagerImpl.getInstance().getPublisher() != null;
  }

  private boolean isDatabaseLive() {
    return BaseDao.isSessionFactoryAvailable();
  }
}
