import os
import requests

#for url encoding
import urllib.parse

import flask

def rpo_delete_listener(object_delete_event, context):

  project = os.environ.get('GCP_PROJECT', 'Specified environment variable is not set.')
  print(f"Project : {project}")
  print(f"Processing object_delete_event: {object_delete_event['name']}.")
  name = object_delete_event['name']
  print( name.rfind('_'))
  split_index = name.rfind('_')
  ttl = name[split_index+1:]
  print (ttl)
  int_ttl = int(ttl)
  prefix_index = name.find('/')
  print(prefix_index)
  prefix = name[0:prefix_index]
  print(prefix)
  print('Bucket: {}'.format(object_delete_event['bucket']))
  print(f"Processing full object_delete_event: {object_delete_event}")
  bucket = object_delete_event['bucket']
  bucket_prefix = 'gs://'+bucket+'/'+prefix
  print(bucket)
  print ('data:','invoking REST call')
  dict_to_send = {'datasetName': prefix, 'dataStorageName': bucket_prefix, 'projectId':project, 'retentionPeriod': int_ttl, 'type': 'DATASET'}

  # vpc format http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/
  print(" goodbye world ")
  # below is the format needed for our VPC cloud endpoints setup, note the port 80
  #response = requests.get('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/myresource/')

  encoded = urllib.parse.quote_plus(bucket_prefix)
  print ('encoded: ',encoded)

  url = 'http://104.198.4.155:8080/retentionrules/deleteByBusinessKey?project={}&bucket={}&dataSet={}'.format(project, encoded, prefix)
  print ('url: ', url)
  delete_response = requests.delete(url)
  print ('response from server:', delete_response)
  print ('response code: ', delete_response.status_code)

