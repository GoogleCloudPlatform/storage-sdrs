package com.google.gcs.sdrs.service;

import com.google.gcs.sdrs.dao.DAO;
import com.google.gcs.sdrs.dao.impl.GenericDAO;
import com.google.gcs.sdrs.dao.model.RetentionExecution;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.dao.model.RetentionRule;

public class DataAccessService {

	private static DAO<RetentionRule, ?> retentionRuleDAO; // we'll have one 1 dedicated DAO instance per table in this application
	private static DAO<RetentionJob, ?> retentionJobDAO;
	private static DAO<RetentionJobValidation, ?> retentionJobValidationDAO;
	private static DAO<RetentionExecution, ?> retentionExecutionDAO;
	
	public DataAccessService() {
		retentionRuleDAO = new GenericDAO(RetentionRule.class); // inject entity class this dao is responsibe for
		retentionJobDAO = new GenericDAO(RetentionJob.class);
		retentionJobValidationDAO = new GenericDAO(RetentionJobValidation.class);
		retentionExecutionDAO = new GenericDAO(RetentionExecution.class);
	}
	
	public void persistRetentionRule(Object retentionDomainObject) {

		// do any needed business logic conversion from POJO to Entity Object here
		RetentionRule retentionRule = new RetentionRule();
		retentionRule.setDatasetName("helloWorld");
		retentionRuleDAO.persist(retentionRule);
	}
	
	public static void main (String[] args) {
		DataAccessService dataAccessService = new DataAccessService();
		dataAccessService.persistRetentionRule(new Object()); //dummy example
	}
}
