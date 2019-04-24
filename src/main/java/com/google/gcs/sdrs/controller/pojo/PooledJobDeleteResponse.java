package com.google.gcs.sdrs.controller.pojo;

public class PooledJobDeleteResponse extends BaseHttpResponse {
	
	 private boolean success;

	  public boolean isSuccess() {
	    return success;
	  }

	  public void setSuccess(boolean success) {
	    this.success = success;
	  }
}
