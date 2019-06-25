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
package com.google.gcs.sdrs.service.mq;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.service.mq.events.InactiveDatasetNotificationEvent;
import com.google.gcs.sdrs.service.mq.events.SuccessDeleteNotificationEvent;
import com.google.gcs.sdrs.service.mq.pojo.DeleteNotificationMessage;
import com.google.gcs.sdrs.service.mq.pojo.InactiveDatasetMessage;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Message queue implementation using PubSub. Support sending Avro messages */
public class PubSubMessageQueueManagerImpl implements MessageQueueManager {

  private static final Logger logger = LoggerFactory.getLogger(PubSubMessageQueueManagerImpl.class);

  private Publisher publisher;
  private static PubSubMessageQueueManagerImpl instance;

  private PubSubMessageQueueManagerImpl() {}

  public static final String TOPIC_APP_CONFIG_KEY = "pubsub.topic";

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
   * @param msg A {@link com.google.gcs.sdrs.service.mq.pojo.DeleteNotificationMessage
   *     DeleteNotificationMessage}
   */
  @Override
  public void sendSuccessDeleteMessage(DeleteNotificationMessage msg) throws IOException {
    if (msg == null) {
      logger.warn("Message is null");
      return;
    }
    if (publisher == null) {
      logger.error("Pubsub publisher is null");
      return;
    }

    try {
      SuccessDeleteNotificationEvent avroMessage = msg.convertToAvro();
      if (avroMessage == null) {
        logger.error("Failed to create avro message ");
        return;
      }
      ByteString data = ByteString.copyFrom(convertToJson(avroMessage));
      sendPubSubMessage(data);
    } catch (IOException | NullPointerException e) {
      logger.error(
          String.format("Failed to send message %s,  %s", msg.toString(), e.getMessage()), e);
      throw new IOException(String.format("Failed to send message %s", msg.toString()));
    }
  }

  /**
   * Send a successful inactive dataset notification message to pubsub topic.
   *
   * @param msg A {@link com.google.gcs.sdrs.service.mq.pojo.InactiveDatasetMessage
   *     DeleteNotificationMessage}
   */
  @Override
  public void sendInactiveDatasetMessage(InactiveDatasetMessage msg) throws IOException {
    if (msg == null) {
      logger.warn("Message is null");
      return;
    }
    if (publisher == null) {
      logger.error("Pubsub publisher is null");
      return;
    }

    try {
      InactiveDatasetNotificationEvent avroMessage = msg.convertToAvro();
      if (avroMessage == null) {
        logger.error("Failed to create avro message ");
        return;
      }
      ByteString data = ByteString.copyFrom(convertToJson(avroMessage));
      sendPubSubMessage(data);
    } catch (IOException | NullPointerException e) {
      logger.error(
          String.format("Failed to send message %s,  %s", msg.toString(), e.getMessage()), e);
      throw new IOException(String.format("Failed to send message %s", msg.toString()));
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

  private void sendPubSubMessage(ByteString data) {
    PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(data).build();

    ApiFuture<String> future = publisher.publish(pubsubMessage);
    ApiFutures.addCallback(
        future,
        new ApiFutureCallback<String>() {

          @Override
          public void onFailure(Throwable throwable) {
            logger.error(
                String.format(
                    "Error publishing message: %s %s",
                    new String(data.toByteArray()), throwable.getMessage()));
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
}
