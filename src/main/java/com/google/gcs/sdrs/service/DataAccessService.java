package com.google.gcs.sdrs.service;

import com.google.gcs.sdrs.dao.DAO;
import com.google.gcs.sdrs.dao.impl.GenericDAO;
import com.google.gcs.sdrs.dao.model.RetentionRule;

public class DataAccessService {

	private static DAO retentionRuleDAO; // we'll have one 1 dedicated stood up DAO in this application per table 
	
	public DataAccessService() {
		retentionRuleDAO = new GenericDAO(RetentionRule.class); // inject entity class this dao is responsibe for
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
