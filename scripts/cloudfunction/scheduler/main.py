def scheduler_pubsub(pub_sub_event, context):
    
    project = os.environ.get('GCP_PROJECT', 'Specified environment variable is not set.')
    print(f"Project : {project}")
    print(f"Processing pub_sub_event: {pub_sub_event['name']}.")
    pub_sub_message = base64.b64decode(pub_sub_event['data']).decode('utf-8')
    print(f"Pubsub message : {pub_sub_message}")
    type='POLICY'
    dict_to_send = {'type':type}
  
    print("Invoking REST call via cloud function")
    
    if pub_sub_message == 'executor':
        print ('Invoking the executor')
        response = requests.post('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/events/execution/', json=dict_to_send)
        print ('Response from server: {}'.format(response.text))
    elif pub_sub_message == 'validator':
        print('Invoking the validator')
        response = requests.post('http://sdrs-api.endpoints.sdrs-server.cloud.goog:80/events/validation/')
        print ('Response from server: {}'.format(response.text))
    else:
        print('Nothing to schedule')