# Copyright 2019 Google LLC. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may not
# use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
# Any software provided by Google hereunder is distributed "AS IS", WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.

import json
import logging
import time

import googleapiclient.discovery
import google.auth.jwt

from apiclient.discovery import build
from google.auth import jwt
from oauth2client.client import GoogleCredentials



# note this SA needs this role: roles/iam.serviceAccountTokenCreator
# see https://cloud.google.com/iam/docs/understanding-service-accounts
LOGGER = logging.getLogger('sdrs_provisioning_cli')
credentials = GoogleCredentials.get_application_default()
#print(dir(credentials))
sa_email = credentials.service_account_email
#print('email '+sa_email)
JWT = None


def get_auth_header(endpoint):
  """Returns an authorization header that can be attached to a request."""
  return {'Authorization': 'Bearer {}'.format(_get_jwt(endpoint))}


def _get_jwt(endpoint):
  """Checks to see if the global JWT is still valid and either returns it or
  generates a new one."""
  global JWT
  if JWT is None:
    JWT = _generate_jwt(endpoint)
  else:
    try:
      # This will throw a ValueError if the JWT is expired by over 5 min
      decoded = jwt.decode(JWT, verify=False)

      # Err on the side of caution and just create a new JWT if we're at expiry
      if time.time() >= decoded['exp']:
        JWT = _generate_jwt(endpoint)
    except ValueError:
      JWT = _generate_jwt(endpoint)
  return JWT


def _generate_jwt(endpoint):
  """Generates a signed JWT using the currently running service account credential."""
  service = googleapiclient.discovery.build(serviceName='iam', version='v1',
                                            cache_discovery=False, credentials=credentials)
  now = int(time.time())
  payload_json = json.dumps({
    'iat': now,
    # expires after one hour
    'exp': now + 3600,
    # iss is the service account email
    'iss': sa_email,
    # sub is required for cloud endpoints and must match iss
    'sub': sa_email,
    'email': sa_email,
    # aud is the URL of the target service
    'aud': endpoint
  })

  slist = service.projects().serviceAccounts().signJwt(
      name='projects/-/serviceAccounts/{}'.format(sa_email),
      body={'payload': payload_json})
  resp = slist.execute()
  return resp['signedJwt']



