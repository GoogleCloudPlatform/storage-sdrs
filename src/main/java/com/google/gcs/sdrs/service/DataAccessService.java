/*
 * Copyright 2019 Google LLC. All rights reserved.
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

package com.google.gcs.sdrs.service;

import com.google.gcs.sdrs.dao.Dao;
import com.google.gcs.sdrs.dao.impl.GenericDao;
import com.google.gcs.sdrs.dao.model.RetentionExecution;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.enums.RetentionRuleTypes;

/**
 * 
 * We'll have one 1 dedicated Dao instance per table in this application
 *
 */
public class DataAccessService {

	private static Dao<RetentionRule, ?> retentionRuleDao; 
	private static Dao<RetentionJob, ?> retentionJobDao;
	private static Dao<RetentionJobValidation, ?> retentionJobValidationDao;
	private static Dao<RetentionExecution, ?> retentionExecutionDao;
	
	public DataAccessService() {
		retentionRuleDao = new GenericDao(RetentionRule.class); // inject entity class for this dao
		retentionJobDao = new GenericDao(RetentionJob.class);
		retentionJobValidationDao = new GenericDao(RetentionJobValidation.class);
		retentionExecutionDao = new GenericDao(RetentionExecution.class);
	}
	
	/**
	 * Example method to demonstrate the pattern to perform any 
	 * any needed conversion from POJO to Entity objects
	 * in order to decouple the data access layer from the rest
	 * of the application business logic tier and above
	 * 
	 * TODO remove this method/update with code from PC-118 (Implement Service Tier)
	 * @param retentionDomainObject
	 */
	public void persistRetentionRule(Object retentionDomainObject) {
		
		// PK created/managed by the DB
		RetentionRule retentionRule = new RetentionRule(); 
		retentionRule.setDatasetName("dataSetX");
		retentionRule.setRetentionPeriodInDays(90);
		retentionRule.setProjectId("coldStorageCluster");
		retentionRule.setType(RetentionRuleTypes.DATASET);
		retentionRule.setVersion(1);
		retentionRule.setIsActive(true);
		retentionRule.setUser("UserX");
		retentionRuleDao.persist(retentionRule);
	}
	
	/**
	 * TODO remove method
	 */
	public static void main (String[] args) {
		DataAccessService dataAccessService = new DataAccessService();
		dataAccessService.persistRetentionRule(new Object()); //dummy example
	}
}
