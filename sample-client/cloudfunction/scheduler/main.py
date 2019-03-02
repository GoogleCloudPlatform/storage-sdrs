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

import base64
import logging
import os
import requests

from common_lib import utils
from common_lib.utils import EVENTS_EXECUTION_ENDPOINT
from common_lib.utils import EVENTS_VALIDATION_ENDPOINT

LOGGER = logging.getLogger('sdrs_cf_scheduler')
LOGGER.setLevel(os.getenv('logLevel'))


def handler(event, context):
  """Takes in a pubsub message and invokes a POST based on the message"""
  pub_sub_message = base64.b64decode(event['data']).decode('utf-8')

  if pub_sub_message == 'executor':
    LOGGER.debug('POST: %s', EVENTS_EXECUTION_ENDPOINT)
    response = requests.post(EVENTS_EXECUTION_ENDPOINT, json={'type': 'POLICY'},
                             headers=utils.get_auth_header())
    LOGGER.debug('Response: %s', response.text)

  elif pub_sub_message == 'validator':
    LOGGER.debug('POST: %s', EVENTS_VALIDATION_ENDPOINT)
    response = requests.post(EVENTS_VALIDATION_ENDPOINT,
                             headers=utils.get_auth_header())
    LOGGER.debug('Response: %s', response.text)

  else:
    LOGGER.warn('Unexpected message from PubSub: %s', pub_sub_message)
  return