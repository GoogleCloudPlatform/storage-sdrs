import os
import requests

import flask

def rpo_listener(object_finalize_event, context):

    project = os.environ.get('GCP_PROJECT', 'Specified environment variable is not set.')
    print(f"Project : {project}")
    print(f"Processing object_finalize_event: {object_finalize_event['name']}.")
    name = object_finalize_event['name']
    split_index = name.rfind('_')
    ttl = name[split_index+1:]
    print('Time to live: {}'.format(ttl))
    int_ttl = int(ttl)
    prefix_index = name.find('/')
    prefix = name[0:prefix_index]
    print('Prefix: {}'.format(prefix))
    print('Bucket: {}'.format(object_finalize_event['bucket']))
    print(f"Processing full object_finalize_event: {object_finalize_event}")
    bucket = object_finalize_event['bucket']
    bucket_prefix = 'gs://'+bucket+'/'+prefix
    print('Bucket: {}'.format(bucket))
    print('invoking REST call')
    dict_to_send = {'datasetName': prefix, 'dataStorageName': bucket_prefix, 'projectId':project, 'retentionPeriod': int_ttl, 'type': 'DATASET'}
    response = requests.post('http://104.198.4.155:8080/retentionrules/', json=dict_to_send)
    print ('Response from server: {}'.format(response.text))
    dict_from_server = response.json()
