package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import java.util.List;

/** Defines available persistence operations for RetentionJobValidation functions */
public interface RetentionJobValidationDao extends Dao<RetentionJobValidation, Integer> {

  List<RetentionJob> findAllPendingRetentionJobs();

  List<RetentionJobValidation> findAllByRetentionJobNames(List<String> retentionJobNames);
}
