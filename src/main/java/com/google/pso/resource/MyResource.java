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

package com.google.pso.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.cloudy.retention.JobManager.JobManager;
import com.google.cloudy.retention.worker.BaseWorker;
import com.google.cloudy.retention.worker.DemoWorker;
import com.google.pso.domain.Car;
import com.google.pso.service.ServiceManager;

/**
 * Root resource (exposed at "myresource" path)
 * @deprecated
 */
@Path("/myresource")
public class MyResource {

    static final private Logger logger = LoggerFactory.getLogger(MyResource.class);
    static final private JobManager jobManager = JobManager.getInstance();

    protected ServiceManager service;


    public MyResource() {
        init();
    }

    protected void init() {
        //TODO - make fancy with dependency injection later
        service = ServiceManager.getInstance();
    }

    /**
     * Method handling HTTP GET requests. The returned object will be sent
     * to the client as "text/plain" media type.
     *
     * @return String that will be returned as a text/plain response.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getIt() {
        logger.debug("Get Got Gotten");
        BaseWorker worker = new DemoWorker();
        jobManager.submitJob(worker);
        return "Got it good!";
    }

    @GET
    @Path("/name")
    @Produces(MediaType.TEXT_PLAIN)
    public String getName() {
        return "Hello David Salguero";
    }


    /**
     * TODO expose path parameters and query parameters
     *
     * @return
     */
    @GET
    @Path("/cars")
    @Produces({MediaType.APPLICATION_JSON})
    public List<Car> getCars() {
        logger.debug("Get Cars");
        return service.getCars();
    }


    /**
     *  Serialize Avro message to JSON
     */
    private static byte[] convertToJson(GenericRecord record) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Encoder jsonEncoder = EncoderFactory.get().jsonEncoder(record.getSchema(), outputStream);
            DatumWriter<GenericRecord> writer = record instanceof SpecificRecord ?
                    new SpecificDatumWriter<>(record.getSchema()) :
                    new GenericDatumWriter<>(record.getSchema());
            writer.write(record, jsonEncoder);
            jsonEncoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
