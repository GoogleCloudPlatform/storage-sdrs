/*
 * Copyright 2018 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the “License”);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and limitations under the License.
 *
 * Any software provided by Google hereunder is distributed “AS IS”,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, and is not intended for production use.
 *
 */

package com.google.cloudy.retention.pojo;

import java.io.Serializable;

import org.joda.time.DateTime;


public class RetentionJobLog implements Serializable {

	private static final long serialVersionUID = 5322619369454991591L;
	public static final String RUNNING="running";
	public static final String FINISHED="finished";
	public static final String FAILED="failed";
	
	public static final String STS="STS Job";
	public static final String VAL="Validation Job";
	public static final String OLM="OLM Job";
	public static final String DAL="DAL Update";
	public static final String MARKER = "STS Delete Marker";
	
	
	private String id;
	private String type;
	private String status;
	private DateTime startTime;
	private DateTime endTime;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public DateTime getStartTime() {
		return startTime;
	}
	public void setStartTime(DateTime startTime) {
		this.startTime = startTime;
	}
	public DateTime getEndTime() {
		return endTime;
	}
	public void setEndTime(DateTime endTime) {
		this.endTime = endTime;
	}
	

}
