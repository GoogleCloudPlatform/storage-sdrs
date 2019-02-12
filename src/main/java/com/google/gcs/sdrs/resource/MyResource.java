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

package com.google.gcs.sdrs.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.google.gcs.sdrs.dao.Dao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.rule.RuleExecutor;
import com.google.gcs.sdrs.rule.RuleValidator;
import com.google.gcs.sdrs.rule.StsRuleExecutor;
import com.google.gcs.sdrs.rule.StsRuleValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Root resource (exposed at "myresource" path)
 *
 * @deprecated
 */
@Path("/myresource")
public class MyResource {

  static final private Logger logger = LoggerFactory.getLogger(MyResource.class);

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

    // The following is test code to execute the STS utility. This assumes a retention rule
    // with ID = 1 exists in the database
    Dao<RetentionRule, Integer> ruleDao = SingletonDao.getRetentionRuleDao();

    RetentionRule rule = ruleDao.findById(1);

    try{
      RuleExecutor executor = StsRuleExecutor.getInstance();
      RetentionJob job = executor.executeDatasetRule(rule);
    } catch (IOException ex) {
      logger.error("Couldn't submit rule for execution: " + ex.getMessage());
    }

    return "Got it good!";
  }
}
