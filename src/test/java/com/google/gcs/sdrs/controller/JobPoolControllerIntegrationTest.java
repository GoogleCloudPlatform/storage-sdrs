package com.google.gcs.sdrs.controller;

import java.io.IOException;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.StoragetransferScopes;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.common.base.Preconditions;
import com.google.gcs.sdrs.util.RetryHttpInitializerWrapper;
import com.google.gcs.sdrs.util.StsUtil;

/**
 * 
 * Integration test that achieves four things:
 * 
 * 1) Programmatically creates STS Jobs in GCP
 * 2) Invokes a REST endpoint to send metadata to SDRS
 * 3) Verifies that the STS metadata is accessible via SDRS
 * 4) Deletes the STS Jobs and associated metadata from the cloud and SDRS
 * 
 */
public class JobPoolControllerIntegrationTest {
	
	 private List<TransferJob> transferJobs;
	 static Storagetransfer client = createStorageTransferClient();
	

	 private void setup () throws IOException {
		 client = createStorageTransferClient();
		 transferJobs = new ArrayList<TransferJob>(10);
	 }

	 
	   /**
	     * Create a Storage Transfer client using application default credentials and other default
	     * settings.
	     *
	     * @return a Storage Transfer client
	     * @throws IOException there was an error obtaining application default credentials
	     */
	    public static Storagetransfer createStorageTransferClient() {
	        HttpTransport httpTransport = Utils.getDefaultTransport();
	        JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
	        GoogleCredential credential = null;
	        try {
	            credential = GoogleCredential.getApplicationDefault(httpTransport, jsonFactory);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return createStorageTransferClient(httpTransport, jsonFactory, credential);
	    }
	 
	 
@Test
public void runIntegrationTest() throws IOException {
	setup();
	createSTSJobs();
	sendMetadataToSDRS();
	assertSeedingInSDRS();
	cleanUp();
}



private static Storagetransfer createStorageTransferClient(
        HttpTransport httpTransport, JsonFactory jsonFactory, GoogleCredential credential) {
    Preconditions.checkNotNull(httpTransport);
    Preconditions.checkNotNull(jsonFactory);
    Preconditions.checkNotNull(credential);

    // In some cases, you need to add the scope explicitly.
    if (credential.createScopedRequired()) {
        credential = credential.createScoped(StoragetransferScopes.all());
    }
    // Please use custom HttpRequestInitializer for automatic
    // retry upon failures. We provide a simple reference
    // implementation in the "Retry Handling" section.
    HttpRequestInitializer initializer = new RetryHttpInitializerWrapper(credential);
    return new Storagetransfer.Builder(httpTransport, jsonFactory, initializer)
            .setApplicationName("storagetransfer-sample")
            .build();
}

protected void createSTSJobs() {
	 TransferJob transferJob = null;
	 List<String> prefixes = new ArrayList<String>(); // empty list 
	 ZonedDateTime scheduledTime = ZonedDateTime.now(Clock.systemUTC());
	 
	   try {
	        transferJob =
	            StsUtil.createDefaultStsJob(
	                client,
	                "sdrs-server",
	                "ds-bucket-dev",
	                "ds-dev-rpo",
	                prefixes,
	                "test job description",
	                scheduledTime,
	                10);
	        transferJobs.add(transferJob);
	      } catch (IOException e) {
	        System.out.println(
	            String.format(
	                "Failed to create STS job")+e.getMessage());
	      }
}

protected void sendMetadataToSDRS() {
	if (transferJobs != null) {
		System.out.println("Looping through the transfer jobs and invoking RESTful endpoint");
		for(TransferJob transferJob:transferJobs) {
			System.out.println(transferJob.getDescription());
		}
	}
}

protected void assertSeedingInSDRS() {
	
}

protected void cleanUp() {
	//deletes actual sts job from cloud
	
	// invokes delete endpoint 
}

}