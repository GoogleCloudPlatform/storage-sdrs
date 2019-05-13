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
# 1) Pass in only germane arguments for 1 bucket
# 2) Wire in call to fire off to SDRS REST end point 
# 3) make it work for many buckets (iterate over data set)
#

"""Command-line sample that creates a daily transfer from a standard
GCS bucket to a Nearline GCS bucket for objects untouched for 30 days.

This sample is used on this page:

    https://cloud.google.com/storage/transfer/create-transfer

For more information, see README.md.
"""

import argparse
import datetime
import json

import googleapiclient.discovery


# [START main]
def main(project_id, start_date, source_bucket,
         sink_bucket):
    storagetransfer = googleapiclient.discovery.build('storagetransfer', 'v1')
    # set i to 24 or 12 depending on frequency
    frequency = 24
    i = 0
    while i < frequency:
        job_name = 'Pooled STS Job ' + str(i) + ' for bucket ' + source_bucket
        print(job_name)
        #start_time_string = str(i) + ':00:00'
        #print(start_time_string)
            # Edit this template with desired parameters.
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
        i += 1


# [END main]


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description=__doc__,
        formatter_class=argparse.RawDescriptionHelpFormatter)
    #parser.add_argument('description', help='Transfer description.')
    parser.add_argument('project_id', help='Your Google Cloud project ID.')
    parser.add_argument('start_date', help='Date YYYY/MM/DD.')
    #parser.add_argument('frequency', help='Frequency hourly.')
    #parser.add_argument('start_time', help='UTC Time (24hr) HH:MM:SS.')
    parser.add_argument('source_bucket', help='Source GCS bucket name.')
    parser.add_argument('sink_bucket', help='Target GCS bucket name.')

    args = parser.parse_args()
    start_date = datetime.datetime.strptime(args.start_date, '%Y/%m/%d')
    #start_time = datetime.datetime.strptime(args.start_time, '%H:%M:%S')

    main(
        #args.description,
        args.project_id,
        start_date,
        #args.frequency,
        #start_time,
        args.source_bucket,
        args.sink_bucket)
# [END all]
