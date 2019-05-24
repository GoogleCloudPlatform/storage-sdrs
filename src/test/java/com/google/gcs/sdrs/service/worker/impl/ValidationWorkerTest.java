package com.google.gcs.sdrs.service.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gcs.sdrs.common.RetentionJobStatusType;
import com.google.gcs.sdrs.dao.RetentionJobValidationDao;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleValidator;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(StsRuleValidator.class)
@PowerMockIgnore("javax.management.*")
public class ValidationWorkerTest {

  private RetentionJobValidationDao retentionJobValidationDaoMock;
  private StsRuleValidator ruleValidatorMock;

  private List<RetentionJob> pendingJobs;
  private List<RetentionJobValidation> stsValidations;
  private List<RetentionJobValidation> existingValidations;

  @Before
  public void setup() {
    pendingJobs = new ArrayList<>();
    pendingJobs.add(createRetentionJob("1"));
    pendingJobs.add(createRetentionJob("2"));

    existingValidations = new ArrayList<>();
    existingValidations.add(
        createRetentionJobValidation(1, 1, "job1", RetentionJobStatusType.PENDING));

    stsValidations = new ArrayList<>();
    stsValidations.add(
        createRetentionJobValidation(null, 1, "job1", RetentionJobStatusType.SUCCESS));
    stsValidations.add(
        createRetentionJobValidation(null, 2, "job2", RetentionJobStatusType.PENDING));

    retentionJobValidationDaoMock = mock(RetentionJobValidationDao.class);
    when(retentionJobValidationDaoMock.findAllPendingRetentionJobs()).thenReturn(pendingJobs);
    when(retentionJobValidationDaoMock.findAllByRetentionJobNames(any()))
        .thenReturn(existingValidations);

    ruleValidatorMock = mock(StsRuleValidator.class);
    when(ruleValidatorMock.validateRetentionJobs(any())).thenReturn(stsValidations);

    PowerMockito.mockStatic(StsRuleValidator.class);
    when(StsRuleValidator.getInstance()).thenReturn(null);
  }

  @Test
  public void doWorkRunsSuccessfully() {
    ValidationWorker worker = new ValidationWorker(UUID.randomUUID().toString());
    worker.dao = retentionJobValidationDaoMock;
    worker.stsRuleValidator = ruleValidatorMock;

    worker.doWork();

    ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
    verify(retentionJobValidationDaoMock).saveOrUpdateBatch(argument.capture());
    List<RetentionJobValidation> arguments = argument.getValue();
    assertEquals(4, arguments.size());

    RetentionJobValidation existingValidation =
        arguments.stream().filter(x -> x.getRetentionJobId().equals(1)).findFirst().orElse(null);
    assertEquals(Integer.valueOf(1), existingValidation.getId());
    assertEquals(RetentionJobStatusType.SUCCESS, existingValidation.getStatus());

    RetentionJobValidation newValidation =
        arguments.stream().filter(x -> x.getRetentionJobId().equals(2)).findFirst().orElse(null);
    assertNull(newValidation.getId());
    assertEquals(RetentionJobStatusType.PENDING, newValidation.getStatus());
  }

  private RetentionJob createRetentionJob(String retentionJobId) {
    RetentionJob job = new RetentionJob();
    job.setRetentionRuleProjectId(retentionJobId);
    return job;
  }

  private RetentionJobValidation createRetentionJobValidation(
      Integer id, Integer retentionJobId, String jobOperationName, RetentionJobStatusType status) {
    RetentionJobValidation validation = new RetentionJobValidation();
    validation.setId(id);
    validation.setRetentionJobId(retentionJobId);
    validation.setJobOperationName(jobOperationName);
    validation.setStatus(status);
    return validation;
  }
}
