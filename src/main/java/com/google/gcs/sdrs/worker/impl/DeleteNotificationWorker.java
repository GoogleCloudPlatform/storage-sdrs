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
package com.google.gcs.sdrs.worker.impl;

import com.google.gcs.sdrs.controller.pojo.NotificationEventRequest;
import com.google.gcs.sdrs.mq.MessageQueueManager;
import com.google.gcs.sdrs.mq.PubSubMessageQueueManagerImpl;
import com.google.gcs.sdrs.mq.pojo.DeleteNotificationMessage;
import com.google.gcs.sdrs.worker.BaseWorker;
import java.time.Instant;

public class DeleteNotificationWorker extends BaseWorker {
  private NotificationEventRequest request;
  private String correlationId;

  public DeleteNotificationWorker(NotificationEventRequest request, String correlationId) {
    super();
    this.request = request;
    this.correlationId = correlationId;
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
    message.setCorrelationId(correlationId);

    MessageQueueManager manager = PubSubMessageQueueManagerImpl.getInstance();
    manager.sendSuccessDeleteMessage(message);
  }
}
