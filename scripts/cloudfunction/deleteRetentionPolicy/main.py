import os
import requests

#for url encoding 
#import urllib.parse

import flask

def rpo_listener(object_finalize_event, context):
    
    project = os.environ.get('GCP_PROJECT', 'Specified environment variable is not set.')
    print(f"Project : {project}")
    print(f"Processing object_finalize_event: {object_finalize_event['name']}.")
    name = object_finalize_event['name']
    print( name.rfind('_'))
    split_index = name.rfind('_')
    ttl = name[split_index+1:]
    print (ttl)
    int_ttl = int(ttl)
    prefix_index = name.find('/')
    print(prefix_index)
    prefix = name[0:prefix_index]
    print(prefix)
    print('Bucket: {}'.format(object_finalize_event['bucket']))
    print(f"Processing full object_finalize_event: {object_finalize_event}")
    bucket = object_finalize_event['bucket']
    bucket_prefix = 'gs://'+bucket+'/'+prefix
    print(bucket)
    print ('data:','invoking REST call')
    dict_to_send = {'datasetName': prefix, 'dataStorageName': bucket_prefix, 'projectId':project, 'retentionPeriod': int_ttl, 'type': 'DATASET'}
  
    # vpc format http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/
    print(" hello world again 02/11/2019")
    # below is the format needed for our VPC cloud endpoints setup, note the port 80
    #response = requests.get('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/myresource/')
    #http://localhost:8080/retentionrules/deleteByBusinessKey?project=sdrs-server&bucket=gs:%2F%2Fds-dev-rpo%2FdataSetY&dataSet=dataSetY
    #todo need to do URL encoding UTF-8
    #delete_response = requests.delete('http://localhost:8080/retentionrules/deleteByBusinessKey?project={project}&bucket={buck_prefix}&dataSet={prefix}')
    #print ('response from server:',response.text)
    #dict_from_server = response.json()
  
    