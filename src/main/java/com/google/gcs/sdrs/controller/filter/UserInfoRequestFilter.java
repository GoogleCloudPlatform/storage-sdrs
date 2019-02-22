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

package com.google.gcs.sdrs.controller.filter;

import com.google.gcs.sdrs.controller.BaseController;
import com.google.gcs.sdrs.controller.pojo.ErrorResponse;
import com.google.gson.Gson;
import java.util.Base64;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Extracts the user values from the HTTP headers */
@Provider
public class UserInfoRequestFilter implements ContainerRequestFilter {

  private static final Logger logger = LoggerFactory.getLogger(UserInfoRequestFilter.class);

  /** Adds user info to the RequestContext properties */
  @Override
  public void filter(ContainerRequestContext context) {
    // ESP will add a header to authenticated incoming requests.
    String base64EncodedUserInfo = context.getHeaders().getFirst("X-Endpoint-API-UserInfo");

    // If the header is not present, the request didn't go through ESP and should be rejected.
    if (base64EncodedUserInfo == null) {
      logger.error("Attempted unauthenticated access.");
      ErrorResponse error = new ErrorResponse();
      error.setMessage("Unauthenticated");
      context.abortWith(
          Response.status(Response.Status.FORBIDDEN.getStatusCode()).entity(error).build());
      return;
    }

    String userInfoJson = new String(Base64.getDecoder().decode(base64EncodedUserInfo));
    UserInfo userInfo = new Gson().fromJson(userInfoJson, UserInfo.class);

    context.setProperty(ContainerContextProperties.USER_INFO.toString(), userInfo);
  }
}
