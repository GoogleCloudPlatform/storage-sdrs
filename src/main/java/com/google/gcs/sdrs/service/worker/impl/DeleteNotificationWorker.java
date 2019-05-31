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
package com.google.gcs.sdrs.service.worker.impl;

import com.google.gcs.sdrs.controller.pojo.NotificationEventRequest;
import com.google.gcs.sdrs.service.mq.MessageQueueManager;
import com.google.gcs.sdrs.service.mq.PubSubMessageQueueManagerImpl;
import com.google.gcs.sdrs.service.mq.pojo.DeleteNotificationMessage;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult;
import com.google.gcs.sdrs.service.worker.WorkerResult.WorkerResultStatus;
import com.google.gcs.sdrs.util.RetentionUtil;
import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeleteNotificationWorker extends BaseWorker {
  private NotificationEventRequest request;
  private final Logger logger = LoggerFactory.getLogger(DeleteNotificationWorker.class);

  public DeleteNotificationWorker(NotificationEventRequest request, String correlationId) {
    super(correlationId);
    this.request = request;
  }



  @Override
  public void doWork() {
    DeleteNotificationMessage message = new DeleteNotificationMessage();
    String deletedObject = request.getDeletedObject();
    int lastForwardSlash = deletedObject.lastIndexOf("/");
    message.setProjectId(request.getProjectId());
    message.setTrigger(deletedObject.substring(lastForwardSlash + 1));
    message.setDeletedDirectoryUri(deletedObject.substring(0, lastForwardSlash));
    message.setDeletedAt(Instant.parse(request.getDeletedAt()));
    message.setCorrelationId(getUuid());

    MessageQueueManager manager = PubSubMessageQueueManagerImpl.getInstance();
    try {
      manager.sendSuccessDeleteMessage(message);
      workerResult.setStatus(WorkerResultStatus.SUCCESS);
    } catch (IOException e) {
      logger.error(String.format("Error sending delete notification: %s", e.getMessage()), e);
      workerResult.setStatus(WorkerResult.WorkerResultStatus.FAILED);
    }
   }
}
