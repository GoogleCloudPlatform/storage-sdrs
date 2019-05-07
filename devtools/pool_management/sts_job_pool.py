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

import googleapiclient.discovery
import json
import datetime
import mysql.connector
from mysql.connector import Error


def create_transfer_client():
    return googleapiclient.discovery.build('storagetransfer', 'v1')


def build_db_connection(host, database, user, password):
    try:
        mySQLconnection = mysql.connector.connect(host=host,
                                                  database=database,
                                                  user=user,
                                                  password=password)
        return mySQLconnection
    except Error as e:
        print("Error while connecting to MySQL", e)


def create_sts_pool(source_bucket, sink_bucket, project_id, db_conn):
    start_date = datetime.datetime.now()

    # create 24 dataset job
    for i in range(24):
        description = '{} {:02d}:00:00'.format(bucket, i)
        time_of_day = '{:02d}:00:00'.format(i)
        start_time = datetime.datetime.strptime(time_of_day, '%H:%M:%S')
        transfer_job = create_sts_job(description, project_id, start_date, start_time, source_bucket, sink_bucket)
        create_db_sts_job(db_conn, transfer_job['name'], time_of_day, 'dataset', source_bucket)

    # create one default job
    time_of_day = '23:59:59'
    description = '{} {}'.format(bucket, time_of_day)
    start_time = datetime.datetime.strptime(time_of_day, '%H:%M:%S')
    transfer_job = create_sts_job(description, project_id, start_date, start_time, source_bucket, sink_bucket)
    create_db_sts_job(db_conn, transfer_job['name'], time_of_day, 'default', source_bucket)


def create_db_sts_job(db_conn, job_name, time_of_day, type, source_bucket):
    try:
        cursor = db_conn.cursor(prepared=True)
        sql_insert_query = """ INSERT INTO `pooled_sts_job`
                              (`name`, `project_id`, `type`, `schedule`, `source_bucket`, `source_project`, `status`) VALUES 
                              (%s, %s, %s, %s, %s, %s, %s)"""
        insert_tuple = (job_name, project_id, type, time_of_day, source_bucket, project_id, 'active')

        cursor.execute(sql_insert_query, insert_tuple)
        db_conn.commit()
        cursor.close()
    except mysql.connector.Error as error:
        db_conn.rollback()
        print("Failed to insert into MySQL table {}".format(error))


def create_sts_job(description, project_id, start_date, start_time, source_bucket,
                   sink_bucket):
    """Create a daily transfer from Standard to Nearline Storage class."""
    storagetransfer = googleapiclient.discovery.build('storagetransfer', 'v1')

    # Edit this template with desired parameters.
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

    result = storagetransfer.transferJobs().create(body=transfer_job).execute()
    print('Returned transferJob: {}'.format(
        json.dumps(result, indent=4)))
    return result


if __name__ == '__main__':
    with open('config.json', 'r') as f:
        config = json.load(f)

    host = config['database']['host']
    schema = config['database']['schema']
    user = config['database']['user']
    password = config['database']['password']
    db_conn = build_db_connection(host, schema, user, password)
    for pool in config['pool']:
        project_id = pool['project_id']
        for bucket in pool['buckets']:
            ext = config['shadow_bucket_ext']['ext']
            sink_bucket = bucket + ext if config['shadow_bucket_ext']['suffix'] else ext + bucket
            create_sts_pool(bucket, sink_bucket, project_id, db_conn)

    if (db_conn.is_connected()):
        db_conn.close()
        print("MySQL connection is closed")