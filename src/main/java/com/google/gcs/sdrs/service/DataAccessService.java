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
