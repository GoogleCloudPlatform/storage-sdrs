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

package com.google.gcs.sdrs;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gcs.sdrs.controller.logging.TokenFilter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;

/** Resource Configuration for Grizzly Http Server */
@ApplicationPath("resources")
public class AppResourceConfig extends ResourceConfig {
  public AppResourceConfig() {
    packages("com.google.gcs.sdrs");

    Logger logger = Logger.getLogger(LoggingFeature.DEFAULT_LOGGER_NAME);
    logger.setFilter(new TokenFilter());
    register(
        new LoggingFeature(
            logger,
            Level.INFO,
            LoggingFeature.Verbosity.PAYLOAD_ANY,
            100000));

    // create custom ObjectMapper
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(DeserializationFeature.FAIL_ON_NUMBERS_FOR_ENUMS);
    mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

    // create JsonProvider to provide custom ObjectMapper
    JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
    jacksonProvider.setMapper(mapper);

    // register jackson to be json provider
    // https://stackoverflow.com/questions/18317927/force-glassfish4-to-use-jackson-instead-of-moxy#18318314
    register(jacksonProvider);
  }
}
