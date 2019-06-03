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
import time

from google.auth import jwt
import googleapiclient.discovery

_ENDPOINT = 'todo_take_as_argument'
_SERVICE_ACCOUNT_EMAIL = 'salguerod@sdrs-server.iam.gserviceaccount.com'
# note this SA needs this role: roles/iam.serviceAccountTokenCreator
# see https://cloud.google.com/iam/docs/understanding-service-accounts


JWT = None


def get_auth_header():
  """Returns an authorization header that can be attached to a request."""
  return {'Authorization': 'Bearer {}'.format(_get_jwt())}


def _get_jwt():
  """Checks to see if the global JWT is still valid and either returns it or
  generates a new one."""
  global JWT
  if JWT is None:
    JWT = _generate_jwt()
  else:
    try:
      # This will throw a ValueError if the JWT is expired by over 5 min
      decoded = jwt.decode(JWT, verify=False)

      # Err on the side of caution and just create a new JWT if we're at expiry
      if time.time() >= decoded['exp']:
        JWT = _generate_jwt()
    except ValueError:
      JWT = _generate_jwt()
  return JWT


def _generate_jwt():
  """Generates a signed JWT using the currently running service account."""
  service = googleapiclient.discovery.build(serviceName='iam', version='v1',
                                            cache_discovery=False)
  now = int(time.time())
  payload_json = json.dumps({
    'iat': now,
    # expires after one hour
    'exp': now + 3600,
    # iss is the service account email
    'iss': _SERVICE_ACCOUNT_EMAIL,
    # sub is required for cloud endpoints and must match iss
    'sub': _SERVICE_ACCOUNT_EMAIL,
    'email': _SERVICE_ACCOUNT_EMAIL,
    # aud is the URL of the target service
    'aud': _ENDPOINT
  })

  slist = service.projects().serviceAccounts().signJwt(
      name='projects/-/serviceAccounts/{}'.format(_SERVICE_ACCOUNT_EMAIL),
      body={'payload': payload_json})
  resp = slist.execute()
  return resp['signedJwt']

