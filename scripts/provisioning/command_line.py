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

# [START all]

# TODO
# 2) Wire in call to fire off to SDRS REST end point 
# 3) make it work for many buckets (iterate over data set)
#

"""Command-line sample that creates pooled STS jobs and syncs with SDRS.

Note the start times are in UTC (GMT-7 for PDT months)

For more information, see the README.md.
"""

import argparse
import datetime
import json
import logging
import requests

import googleapiclient.discovery

LOGGER = logging.getLogger('sdrs_provisioning_cli')
SDRS_POOL_ENDPOINT = 'http://localhost:8080/stsjobpool/'

# [START main]
def main(project_id, start_date, source_bucket,
         sink_bucket):
    pooled_sts_jobs = _create_sts_jobs_for_bucket(project_id, start_date, source_bucket,
         sink_bucket)
    _sync_sdrs_sts_jobs(pooled_sts_jobs)

# [END main]

# [START _create_sts_jobs_for_bucket]
def _create_sts_jobs_for_bucket(project_id, start_date, source_bucket,
         sink_bucket):
    storagetransfer = googleapiclient.discovery.build('storagetransfer', 'v1')
    sts_jobs = []
    frequency = 24
    i = 0
    while i < frequency:
        job_name = 'Pooled STS Job ' + str(i) + ' for bucket ' + source_bucket
        print(job_name)
        #UTC Time (24hr) HH:MM:SS.
        start_time_string = '{:02d}:00:01'.format(i)
        start_time = datetime.datetime.strptime(start_time_string, '%H:%M:%S')
        
        transfer_job = {    
        'description': job_name,
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

        result = storagetransfer.transferJobs().create(body=transfer_job).execute()
        print('Returned transferJob: {}'.format(
        json.dumps(result, indent=4)))
        pooled_sts_job = {    
        'name': job_name,
        'status': 'DISABLED',
        'projectId': project_id,
        'sourceBucket': source_bucket,
        'targetBucket': sink_bucket,
        'schedule': start_time_string
        }
        sts_jobs.append(pooled_sts_job)
        i += 1
    return sts_jobs
# [END _create_sts_jobs_for_bucket]

def _sync_sdrs_sts_jobs(pooled_sts_jobs):
  """Makes a request to register the STS job with SDRS so it can be utilized."""
  
  LOGGER.debug('POST: %s', SDRS_POOL_ENDPOINT)
  LOGGER.debug('Body: %s', pooled_sts_jobs)
  response = requests.post(SDRS_POOL_ENDPOINT, json=pooled_sts_jobs)
  LOGGER.debug('Response: %s', response.text)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('project_id', help='Your Google Cloud project ID.')
    parser.add_argument('start_date', help='Date YYYY/MM/DD.')
    parser.add_argument('source_bucket', help='Source GCS bucket name.')
    parser.add_argument('sink_bucket', help='Target GCS bucket name.')

    args = parser.parse_args()
    start_date = datetime.datetime.strptime(args.start_date, '%Y/%m/%d')

    main(
        args.project_id,
        start_date,
        args.source_bucket,
        args.sink_bucket)
# [END all]
