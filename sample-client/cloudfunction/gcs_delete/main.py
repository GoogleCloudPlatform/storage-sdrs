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

import logging
import os
import re
import requests
import urllib.parse

from common_lib import utils
from common_lib.utils import EVENTS_NOTIFICATION_ENDPOINT
from common_lib.utils import PROJECT_ID
from common_lib.utils import RETENTION_RULES_ENDPOINT

LOGGER = logging.getLogger('sdrs_cf_gcs_delete')
LOGGER.setLevel(os.getenv('logLevel'))
RPO_REGEX = re.compile(os.getenv('rpoPattern'))
DELETE_REGEX = re.compile(os.getenv('deleteMarkerPattern'))
SUCCESS_REGEX = re.compile(os.getenv('successMarkerPattern'))


def handler(event, context):
  event_attributes = event['attributes']
  object_id = event_attributes['objectId']

  re_match = DELETE_REGEX.search(object_id)
  if re_match:
    _process_delete_success(re_match, event_attributes, object_id)
    return

  re_match = RPO_REGEX.search(object_id)
  if re_match:
    _process_rpo(re_match, event_attributes, object_id)
    return

  re_match = SUCCESS_REGEX.search(object_id)
  if re_match:
    _process_delete_success(re_match, event_attributes, object_id)
    return


def _process_delete_success(re_match, event_attributes, object_id):
  """Makes a request to notify about the deletion of a directory or dataset"""
  body = {'deletedObject': 'gs://{}/{}'.format(event_attributes['bucketId'],
                                               object_id),
          'projectId': PROJECT_ID,
          'deletedAt': event_attributes['eventTime']}
  LOGGER.debug('POST: %s', EVENTS_NOTIFICATION_ENDPOINT)
  LOGGER.debug('Body: %s', body)
  response = requests.post(EVENTS_NOTIFICATION_ENDPOINT, json=body,
                           headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)


def _process_rpo(re_match, event_attributes, object_id):
  """Makes a request to delete a retention rule"""
  sdrs_request = utils.parse_rpo_request(re_match, event_attributes,
                                         object_id)

  url = '{}?projectId={}&dataStorageName={}&type=DATASET'.format(
      RETENTION_RULES_ENDPOINT, sdrs_request.project_id,
      urllib.parse.quote_plus(sdrs_request.data_storage_name))
  LOGGER.debug('DELETE: %s', url)
  response = requests.delete(url, headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)
