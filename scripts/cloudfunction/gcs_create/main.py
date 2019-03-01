import logging
import os
import re
import requests
import urllib.parse

from common_lib import utils
from common_lib.utils import EVENTS_ENDPOINT
from common_lib.utils import RETENTION_RULES_ENDPOINT

LOGGER = logging.getLogger('sdrs_cf')
LOGGER.setLevel(os.getenv('logLevel'))
RPO_REGEX = re.compile(os.getenv('rpoPattern'))
DELETE_REGEX = re.compile(os.getenv('deleteMarkerPattern'))


def handler(event, context):
  event_attributes = event["attributes"]
  object_id = event_attributes['objectId']

  re_match = DELETE_REGEX.search(object_id)
  if re_match:
    _process_delete(re_match, event_attributes, object_id)
    return

  re_match = RPO_REGEX.search(object_id)
  if re_match:
    _process_rpo(re_match, event_attributes, object_id)
    return


def _process_delete(re_match, event_attributes, object_id):
  """Makes a request to create an immediate USER type retention job"""
  sdrs_request = utils.parse_delete_request(re_match, event_attributes,
                                            object_id)
  url = '{}/execution'.format(EVENTS_ENDPOINT)
  body = {'target': sdrs_request.data_storage_name,
          'projectId': sdrs_request.project_id,
          'type': 'USER'}
  LOGGER.debug('POST: %s', url)
  response = requests.post(url, json=body, headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)


def _process_rpo(re_match, event_attributes, object_id):
  """Find outs if the retention rule needs to be created or updated."""
  sdrs_request = utils.parse_rpo_request(re_match, event_attributes,
                                         object_id)

  # Check to see if the retention rule already exists
  url = '{}?projectId={}&dataStorageName={}&type=DATASET'.format(
      RETENTION_RULES_ENDPOINT, sdrs_request.project_id,
      urllib.parse.quote_plus(sdrs_request.data_storage_name))
  LOGGER.debug('GET: %s', url)
  response = requests.get(url, headers=utils.get_auth_header())

  if response.status_code == requests.codes.ok:
    # A 200 response means the rule already exists, so update it
    rule_id = response.json().get('ruleId')
    _process_rpo_update(rule_id, sdrs_request.retention_period)
  elif response.status_code == 404:
    # A 404 response means the rule doesn't exist, so create it
    _process_rpo_create(sdrs_request)
  else:
    LOGGER.error('Unexpected response code %s returned: %s',
                 response.status_code, response.text)


def _process_rpo_update(rule_id, retention_period):
  """Makes a request to update an existing retention rule."""
  # Build the URL for the PUT to update the retention rule
  url = '{}/{}'.format(RETENTION_RULES_ENDPOINT, rule_id)
  body = {'retentionPeriod': retention_period}
  LOGGER.debug('PUT: %s', url)
  response = requests.put(url, json=body, headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)


def _process_rpo_create(sdrs_request):
  """Makes a request to create a retention rule."""
  body = {'dataStorageName': sdrs_request.data_storage_name,
          'projectId': sdrs_request.project_id,
          'retentionPeriod': sdrs_request.retention_period,
          'type': 'DATASET'}
  LOGGER.debug('POST: %s', RETENTION_RULES_ENDPOINT)
  response = requests.post(RETENTION_RULES_ENDPOINT, json=body,
                           headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)
