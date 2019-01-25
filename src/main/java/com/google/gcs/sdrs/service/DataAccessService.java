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

public class DataAccessService {

	private static Dao<RetentionRule, ?> retentionRuleDao; // we'll have one 1 dedicated Dao instance per table in this application
	private static Dao<RetentionJob, ?> retentionJobDao;
	private static Dao<RetentionJobValidation, ?> retentionJobValidationDao;
	private static Dao<RetentionExecution, ?> retentionExecutionDao;
	
	public DataAccessService() {
		retentionRuleDao = new GenericDao(RetentionRule.class); // inject entity class this dao is responsibe for
		retentionJobDao = new GenericDao(RetentionJob.class);
		retentionJobValidationDao = new GenericDao(RetentionJobValidation.class);
		retentionExecutionDao = new GenericDao(RetentionExecution.class);
	}
	
	public void persistRetentionRule(Object retentionDomainObject) {

		// do any needed business logic conversion from POJO to Entity Object here
		RetentionRule retentionRule = new RetentionRule();
		retentionRule.setDatasetName("helloWorld");
		retentionRuleDao.persist(retentionRule);
	}
	
	public static void main (String[] args) {
		DataAccessService dataAccessService = new DataAccessService();
		dataAccessService.persistRetentionRule(new Object()); //dummy example
	}
}
