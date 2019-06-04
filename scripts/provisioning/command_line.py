# Copyright 2019, Google, Inc.
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Command-line sample that creates pooled STS jobs and syncs with SDRS.

Note the start times are in UTC (GMT-7 for PDT months)
Example command line arguments from the prompt:
'python command_line.py create sdrs-server 2019/05/15 ds-dev-rpo ds-bucket-dev'
For more information, see the README.md.
"""

import argparse
import datetime
import json
import logging
import requests
import sys
import utils

import googleapiclient.discovery

from google.cloud import storage

logging.basicConfig()
logging.getLogger('googleapicliet.discovery_cache').setLevel(logging.ERROR)
LOGGER = logging.getLogger('sdrs_provisioning_cli')

# Edit the RESTful endpoint to your desired deployment environment
SDRS_POOL_ENDPOINT = 'http://localhost:8080/stsjobpool/'

# [START main]
def main(command, project_id, start_date, source_bucket,
         sink_bucket):
    storage_client = storage.Client()
    #utils.generate_jwt()
    #print(storage_client)
    try:
        bucket = storage_client.get_bucket(source_bucket)
        print("Bucket exists, proceeding")
    except Exception as e:
        LOGGER.error("Exception, exiting program " + str(e))
        sys.exit() 
    if command == 'create':
        pooled_sts_jobs = _create_sts_jobs_for_bucket(project_id, start_date, source_bucket,
            sink_bucket, 'dataset')
        _register_sdrs_sts_jobs(source_bucket, project_id, pooled_sts_jobs)
    elif command == 'delete':
        _delete_sts_jobs_for_bucket(project_id, source_bucket)
    else:
         print("Unknown command " + str(command))   
         sys.exit() 
# [END main]
    
# [START _create_sts_jobs_for_bucket]
def _create_sts_jobs_for_bucket(project_id, start_date, source_bucket,
         sink_bucket, job_type):
    storagetransfer = googleapiclient.discovery.build('storagetransfer', 'v1', cache_discovery=False)
    sts_jobs = []
    number_of_jobs = 25
    i = 0
    while i < number_of_jobs:
        description = 'Pooled STS Job ' + str(i) + ' for bucket ' + source_bucket
        if i == 24:
            # create the default job - number 25
            job_type = 'default'
            start_time_string = '{:02d}:59:59'.format(23)
        else:
            start_time_string = '{:02d}:00:00'.format(i)
        
        start_time = datetime.datetime.strptime(start_time_string, '%H:%M:%S')
        #Transfer time is in UTC Time (24hr) HH:MM:SS.
        transfer_job = {    
        'description': description,
        'status': 'DISABLED',
        'projectId': project_id,
        'schedule': {
            'scheduleStartDate': {
                'day': start_date.day,
                'month': start_date.month,
                'year': start_date.year
            },
            'startTimeOfDay': {
                'hours': start_time.hour,
                'minutes': start_time.minute,
                'seconds': start_time.second
            }
        },
        'transferSpec': {
            'gcsDataSource': {
                'bucketName': source_bucket
            },
            'gcsDataSink': {
                'bucketName': sink_bucket
            }
        }
        }
        try:
            result = storagetransfer.transferJobs().create(body=transfer_job).execute()
            pooled_sts_job = {    
            'name': result.get("name"),
            'status': result.get("status"),
            'type': job_type,
            'projectId': result.get("projectId"),
            'sourceBucket': result.get("transferSpec").get("gcsDataSource").get("bucketName"),
            'sourceProject': project_id,
            'targetBucket': result.get("transferSpec").get("gcsDataSink").get("bucketName"),
            'schedule': start_time_string
            }
            sts_jobs.append(pooled_sts_job)
            #if i == 12:
            #    raise Exception ('Forced error on API execution')
            i += 1
        except Exception as e:
            # If an exception is encountered during any API iteration, roll back the transaction and error out
            LOGGER.error("Exception, rolling back and exiting program " + str(e))
            _exit_creation_with_cleanup(sts_jobs) 
    print("Successfully created STS job pool in the cloud, standby")
    return sts_jobs
# [END _create_sts_jobs_for_bucket]

# [START _exit_creation_with_cleanup]
def _exit_creation_with_cleanup(sts_jobs):
    storagetransfer = googleapiclient.discovery.build('storagetransfer', 'v1', cache_discovery=False)
    for sts_job in sts_jobs:
        job_name = sts_job.get("name")  
        update_transfer_job_request_body = {
        'project_id': sts_job.get("projectId"),
        'update_transfer_job_field_mask': 'status',
        'transfer_job': {    
        'status': 'DELETED'
        }
        }
        request = storagetransfer.transferJobs().patch(jobName=job_name, body=update_transfer_job_request_body)
        response = request.execute()
    sys.exit()
# [END _exit_creation_with_cleanup]

# [START _delete_sts_jobs_for_bucket]
def _delete_sts_jobs_for_bucket(project_id, source_bucket):
    storagetransfer = googleapiclient.discovery.build('storagetransfer', 'v1', cache_discovery=False)
    # For the bucket, get the list of sts jobs to delete from SDRS
    sts_jobs = _get_pooled_sts_jobs(project_id, source_bucket)
    # Use the name of the jobs to delete them from the cloud
    for sts_job in sts_jobs:
        job_name = sts_job.get("name")  
        update_transfer_job_request_body = {
        'project_id': project_id,
        'update_transfer_job_field_mask': 'status',
        'transfer_job': {    
        'status': 'DELETED'
        }
        }
        request = storagetransfer.transferJobs().patch(jobName=job_name, body=update_transfer_job_request_body)
        response = request.execute()
    #Finally, delete the STS job records from SDRS
    print("Deleted STS Jobs from the cloud, now deleting from SDRS metadata, standby")
    _unregister_sdrs_sts_jobs(project_id, source_bucket)
# [END _delete_sts_jobs_for_bucket]

# [START _get_pooled_sts_jobs]
def _get_pooled_sts_jobs(project_id, source_bucket):
  """Makes a request to get the pooled STS jobs from SDRS."""
  url = '{}?sourceBucket={}&sourceProject={}'.format(
      SDRS_POOL_ENDPOINT, source_bucket, project_id)
  LOGGER.debug('GET: %s', url)
  response = requests.get(url, headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)
  if response.status_code == requests.codes.ok:
    return response.json()
  else:
    LOGGER.error('Unexpected response code %s returned: %s',
                 response.status_code, response.text)
# [END _get_pooled_sts_jobs]

# [START _register_sdrs_sts_jobs]
def _register_sdrs_sts_jobs(source_bucket, project_id, pooled_sts_jobs):
  """Makes a request to register the STS job with SDRS so it can be utilized."""
  url = '{}?sourceBucket={}&sourceProject={}'.format(
      SDRS_POOL_ENDPOINT, source_bucket, project_id)
  print("Registering STS job pool with SDRS, standby")
  LOGGER.debug('POST: %s', url)
  LOGGER.debug('Body: %s', pooled_sts_jobs)
  response = requests.post(url, json=pooled_sts_jobs, headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)
  if response.status_code == requests.codes.ok:
    print('Successful provisioning of jobs with SDRS: {}'.format(
        response.text))
  else:
    LOGGER.error('Unexpected response code %s returned: %s',
                 response.status_code, response.text)
    LOGGER.error("Rolling back and exiting program")
    _exit_creation_with_cleanup(pooled_sts_jobs) 
# [END _register_sdrs_sts_jobs]

# [START _unregister_sdrs_sts_jobs]
def _unregister_sdrs_sts_jobs(project_id, source_bucket):
  """Makes a request to unregister the STS job pool from SDRS."""
  url = '{}?sourceBucket={}&sourceProject={}'.format(
      SDRS_POOL_ENDPOINT, source_bucket, project_id)
  LOGGER.debug('DELETE: %s', url)
  response = requests.delete(url, headers=utils.get_auth_header())
  LOGGER.debug('Response: %s', response.text)
  if response.status_code == requests.codes.ok:
    print('Successful unregistering of STS jobs with SDRS: {}'.format(
        response.text))
  else:
    LOGGER.error('Unexpected response code %s returned: %s',
                 response.status_code, response.text)
# [END _unregister_sdrs_sts_jobs]

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('command', help='create or delete.')
    parser.add_argument('project_id', help='Your Google Cloud project ID.')
    parser.add_argument('start_date', help='Date YYYY/MM/DD.')
    parser.add_argument('source_bucket', help='Source GCS bucket name.')
    parser.add_argument('sink_bucket', help='Target GCS bucket name.')

    args = parser.parse_args()
    start_date = datetime.datetime.strptime(args.start_date, '%Y/%m/%d')

    main(
        args.command,
        args.project_id,
        start_date,
        args.source_bucket,
        args.sink_bucket)
# [END all]
