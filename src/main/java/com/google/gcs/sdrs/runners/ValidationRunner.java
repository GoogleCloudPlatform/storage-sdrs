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

package com.google.gcs.sdrs.runners;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Calls the Validation service endpoint when run */
public class ValidationRunner implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ValidationRunner.class);

  /** Calls the validate job execution status endpoint */
  public void run() {
    try {
      Configuration config = new Configurations().xml("applicationConfig.xml");
      String serviceUrl = config.getString("scheduler.serviceUrl");
      String serviceProtocol = config.getString("scheduler.serviceProtocol");

      String jwt = generateJwt(String.format("https://%s", serviceUrl));
      logger.info(String.format("JWT generated: %s", jwt));

      // TODO: This URL should be the load balancer URL
      logger.info("Making request to validation service endpoint.");
      Client client = ClientBuilder.newClient();
      client
          .target(String.format("%s://%s", serviceProtocol, serviceUrl))
          .path("events/validation")
          .request()
          .header("Authorization", String.format("Bearer %s", jwt))
          .post(null);

    } catch (ConfigurationException ex) {
      logger.error(String.format("Configuration file could not be read: %s", ex.getMessage()));
    } catch (IOException ex) {
      logger.error(String.format("Unable to generate JWT: %s", ex.getMessage()));
    }
  }

  private static String generateJwt(String audience) throws IOException {
    HttpTransport httpTransport = Utils.getDefaultTransport();
    JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
    GoogleCredential cred = GoogleCredential.getApplicationDefault(httpTransport, jsonFactory);

    Date now = new Date();
    Date expTime = new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(3600));

    JWTCreator.Builder token =
        JWT.create()
            .withIssuedAt(now)
            .withExpiresAt(expTime)
            .withIssuer(cred.getServiceAccountId())
            .withSubject(cred.getServiceAccountId())
            .withAudience(audience)
            .withClaim("email", cred.getServiceAccountId());

    // Sign the JWT with a service account
    RSAPrivateKey key = (RSAPrivateKey) cred.getServiceAccountPrivateKey();
    Algorithm algorithm = Algorithm.RSA256(null, key);
    return token.sign(algorithm);
  }
}
