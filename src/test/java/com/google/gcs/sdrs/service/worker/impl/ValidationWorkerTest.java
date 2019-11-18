package com.google.gcs.sdrs.service.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.gcs.sdrs.common.RetentionJobStatusType;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.dao.DmQueueDao;
import com.google.gcs.sdrs.dao.RetentionJobValidationDao;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionJobValidation;
import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleValidator;
import java.io.IOException;
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
  private DmQueueDao dmQueueDaoMock;

  private List<RetentionJob> pendingJobs;
  private List<RetentionJobValidation> stsValidations;
  private List<RetentionJobValidation> existingValidations;

  @Before
  public void setup() {
    pendingJobs = new ArrayList<>();
    RetentionJob userJob = createRetentionJob("1");
    userJob.setId(1);
    userJob.setRetentionRuleType(RetentionRuleType.USER);
    RetentionJob datasetJob = createRetentionJob("2");
    datasetJob.setId(2);
    datasetJob.setRetentionRuleType(RetentionRuleType.DATASET);
    pendingJobs.add(userJob);
    pendingJobs.add(datasetJob);

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

    List<DmRequest> dmRequests = new ArrayList<>();
    DmRequest dmRequest = new DmRequest();
    dmRequest.setRetentionJobId(1);
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
    dmRequests.add(dmRequest);
    dmQueueDaoMock = mock(DmQueueDao.class);
    when(dmQueueDaoMock.getByStatus(any())).thenReturn(dmRequests);

    PowerMockito.mockStatic(StsRuleValidator.class);
    when(StsRuleValidator.getInstance()).thenReturn(null);
  }

  @Test
  public void doWorkRunsSuccessfully() throws IOException {
    ValidationWorker worker = new ValidationWorker(UUID.randomUUID().toString());
    worker.jobValidationDao = retentionJobValidationDaoMock;
    worker.stsRuleValidator = ruleValidatorMock;
    worker.dmQueueDao = dmQueueDaoMock;

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

    ArgumentCaptor<List> dmRequestsArgument = ArgumentCaptor.forClass(List.class);

    verify(dmQueueDaoMock).saveOrUpdateBatch(dmRequestsArgument.capture());

    List<DmRequest> dmRequests = dmRequestsArgument.getValue();
    DmRequest dmRequest = dmRequests.get(0);
    assertEquals("success", dmRequest.getStatus());
    assertEquals(1, dmRequest.getRetentionJobId());
    assertEquals(1, dmRequests.size());
  }

  private RetentionJob createRetentionJob(String projectId) {
    RetentionJob job = new RetentionJob();
    job.setRetentionRuleProjectId(projectId);
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
