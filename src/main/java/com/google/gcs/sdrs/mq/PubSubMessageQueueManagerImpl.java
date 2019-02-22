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
package com.google.gcs.sdrs.mq;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.mq.events.SuccessDeleteNotificationEvent;
import com.google.gcs.sdrs.mq.events.context.EventContext;
import com.google.gcs.sdrs.mq.pojo.DeleteNotificationMessage;
import com.google.gcs.sdrs.util.RetentionUtil;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Message queue implementation using PubSub. Support sending Avro messages */
public class PubSubMessageQueueManagerImpl implements MessageQueueManager {

  private static final Logger logger = LoggerFactory.getLogger(PubSubMessageQueueManagerImpl.class);

  private Publisher publisher;
  private static PubSubMessageQueueManagerImpl instance;

  private PubSubMessageQueueManagerImpl() {}

  public static final String TOPIC_APP_CONFIG_KEY = "pubsub.topic";
  public static final String DELETE_NOTIFICAITON_EVENT_NAME = "SuccessDeleteNotificationEvent";
  public static final String AVRO_MESSAGE_VERSION = "1.0";

  public static PubSubMessageQueueManagerImpl getInstance() {
    if (instance == null) {
      synchronized (PubSubMessageQueueManagerImpl.class) {
        if (instance == null) {
          instance = new PubSubMessageQueueManagerImpl();
          String topicName = SdrsApplication.getAppConfigProperty(TOPIC_APP_CONFIG_KEY);
          if (topicName == null) {
            logger.error("Topic name is not configured");
          } else {
            try {
              instance.publisher = Publisher.newBuilder(topicName).build();
              logger.info("Pubsub publisher created for topic " + topicName);
            } catch (IOException e) {
              logger.error("Error creating pubsub publisher " + e.getMessage());
            }
          }
        }
      }
    }
    return instance;
  }

  public Publisher getPublisher() {
    return publisher;
  }

  /**
   * Send a successful delete notification message to pubsub topic.
   *
   * @param msg A {@link com.google.gcs.sdrs.mq.pojo.DeleteNotificationMessage
   *     DeleteNotificationMessage}
   */
  @Override
  public void sendSuccessDeleteMessage(DeleteNotificationMessage msg) {
    if (msg == null) {
      logger.warn("Message is null");
      return;
    }
    if (publisher == null) {
      logger.error("Pubsub publisher is null");
      return;
    }

    try {
      SuccessDeleteNotificationEvent avroMessage = convertToAvro(msg);
      if (avroMessage == null) {
        logger.error("Failed to create avro message ");
        return;
      }
      ByteString data = ByteString.copyFrom(convertToJson(avroMessage));
      PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

      ApiFuture<String> future = publisher.publish(pubsubMessage);
      ApiFutures.addCallback(
          future,
          new ApiFutureCallback<String>() {

            @Override
            public void onFailure(Throwable throwable) {
              logger.error(
                  String.format("Error publishing message: %s", new String(data.toByteArray())));
              logger.debug(throwable.getMessage());
            }

            @Override
            public void onSuccess(String messageId) {
              logger.info(
                  String.format(
                      "Successfully sending message id: %s  message: %s",
                      messageId, new String(data.toByteArray())));
            }
          },
          MoreExecutors.directExecutor());
    } catch (IOException | NullPointerException e) {
      logger.error(String.format("Failed to send message %s,  %s", msg.toString(), e.getMessage()));
    }
  }

  /**
   * Serialize Avro message to JSON
   *
   * @param record A {@link org.apache.avro.generic.GenericRecord GenericRecord} that represents an
   *     Avro message
   * @return Arroy of bytes
   * @throws IOException
   */
  private static byte[] convertToJson(GenericRecord record) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
    DatumWriter<GenericRecord> writer =
        record instanceof SpecificRecord
            ? new SpecificDatumWriter<>(record.getSchema())
            : new GenericDatumWriter<>(record.getSchema());
    writer.write(record, jsonEncoder);
    jsonEncoder.flush();
    return outputStream.toByteArray();
  }

  /**
   * Convert a POJO to Avro message
   *
   * @param msg A {@link com.google.gcs.sdrs.mq.pojo.DeleteNotificationMessage
   *     DeleteNotificationMessage}
   * @return A {@link com.google.gcs.sdrs.mq.events.SuccessDeleteNotificationEvent
   *     SuccessDeleteNotificationEvent} Avro message
   */
  private static SuccessDeleteNotificationEvent convertToAvro(DeleteNotificationMessage msg) {
    if (msg == null) {
      return null;
    }
    EventContext ctx =
        EventContext.newBuilder()
            .setName(DELETE_NOTIFICAITON_EVENT_NAME)
            .setUuid(UUID.randomUUID().toString())
            .setVersion(AVRO_MESSAGE_VERSION)
            .setCorrelationID(msg.getCorrelationId())
            .setTimestamp(new DateTime())
            .build();

    SuccessDeleteNotificationEvent event =
        SuccessDeleteNotificationEvent.newBuilder()
            .setContext(ctx)
            .setBucket(RetentionUtil.getBucketName(msg.getDeletedDirectoryUri()))
            .setDeletedAt(msg.getDeletedAt().toString())
            .setDirectory(msg.getDeletedDirectoryUri())
            .setTrigger(msg.getTrigger())
            .setProjectId(msg.getProjectId())
            .setVersion(AVRO_MESSAGE_VERSION)
            .build();

    return event;
  }

  public void shutdown() {
    if (publisher != null) {
      try {
        publisher.shutdown();
        logger.info("Pubsub publisher shutdown complete");
      } catch (Exception e) {
        logger.error("Failed to shutdown pubsub publisher");
      }
    }
  }

  public static void main(String[] args) {
    // TODO eshen remove the main before release

    Publisher publisher = null;
    try {
      DeleteNotificationMessage message = new DeleteNotificationMessage();
      message.setProjectId("sdrs-server");
      message.setTrigger(".delete_this_folder");
      message.setDeletedDirectoryUri("gs://my-bucket/my-dataset/my-dir/");
      message.setDeletedAt(Instant.now());
      message.setCorrelationId(UUID.randomUUID().toString());

      PubSubMessageQueueManagerImpl pubSubMessageQueueManager =
          PubSubMessageQueueManagerImpl.getInstance();
      pubSubMessageQueueManager.sendSuccessDeleteMessage(message);
      Thread.sleep(3000);
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (publisher != null) {
        // When finished with the publisher, shutdown to free up resources.
        try {
          publisher.shutdown();
          publisher.awaitTermination(1, TimeUnit.MINUTES);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
  }
}
