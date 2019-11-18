package com.google.gcs.sdrs.service.worker.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.api.services.storagetransfer.v1.model.ObjectConditions;
import com.google.api.services.storagetransfer.v1.model.Schedule;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.api.services.storagetransfer.v1.model.TransferSpec;
import com.google.gcs.sdrs.dao.DmQueueDao;
import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.impl.LockDaoImpl;
import com.google.gcs.sdrs.dao.model.DistributedLock;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.StsUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({StsRuleExecutor.class, StsUtil.class, SingletonDao.class, CredentialsUtil.class})
@PowerMockIgnore("javax.management.*")
public class DmBatchProcessingWorkerTest {
  private LockDao lockDaoMock;
  private DmQueueDao dmQueueDaoMock;
  private StsRuleExecutor ruleExecutorMock;
  private CredentialsUtil credentialsUtilMock;
  private String uuid;

  @Before
  public void setUp() {
    lockDaoMock = mock(LockDaoImpl.class);
    dmQueueDaoMock = mock(DmQueueDao.class);
    ruleExecutorMock = mock(StsRuleExecutor.class);

    // mock static methods
    credentialsUtilMock = mock(CredentialsUtil.class);
    PowerMockito.mockStatic(CredentialsUtil.class);
    when(CredentialsUtil.getInstance()).thenReturn(credentialsUtilMock);

    PowerMockito.mockStatic((StsUtil.class));
    when(StsUtil.createStsClient(any())).thenReturn(null);
    when(StsUtil.buildTransferSpec(any(), any(), any(), any(), any())).thenReturn(null);

    PowerMockito.mockStatic(StsRuleExecutor.class);
    when(StsRuleExecutor.getInstance()).thenReturn(ruleExecutorMock);
    when(StsRuleExecutor.buildRetentionJobEntity(any(), any(), any(), any()))
        .thenReturn(new RetentionJob());

    PowerMockito.mockStatic(SingletonDao.class);
    when(SingletonDao.getDmQueueDao()).thenReturn(dmQueueDaoMock);
    when(SingletonDao.getLockDao()).thenReturn(lockDaoMock);

    when(lockDaoMock.obtainLock(any(), anyInt(), any())).thenReturn(new DistributedLock());

    uuid = UUID.randomUUID().toString();
  }

  @After
  public void tearDown() {}

  @Test
  public void testDoWorkPendingDmRequests() throws IOException {
    TransferJob transferJob = createBasicTransferJob();

    List<DmRequest> dmRequests = new ArrayList<>();
    DmRequest dmRequest = new DmRequest();
    dmRequest.setProjectId("projectId");
    dmRequest.setDataStorageRoot("bucket");
    dmRequest.setDataStorageName("gs://bucket/dataset/dir1");
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_PENDING);
    dmRequests.add(dmRequest);

    dmRequest = new DmRequest();
    dmRequest.setProjectId("projectId");
    dmRequest.setDataStorageRoot("bucket");
    dmRequest.setDataStorageName("gs://bucket/dataset/dir2");
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_PENDING);
    dmRequests.add(dmRequest);

    // mock
    when(dmQueueDaoMock.getAllAvailableRequestsByPriority()).thenReturn(dmRequests);
    when(ruleExecutorMock.findPooledJob(any(), any(), any(), any())).thenReturn(transferJob);
    when(StsUtil.updateExistingJob(any(), any(), any(), any())).thenReturn(transferJob);
    doNothing().when(dmQueueDaoMock).createRetentionJobUdpateDmStatus(any(), any());

    // execute
    DmBatchProcessingWorker worker = new DmBatchProcessingWorker(uuid);
    worker.doWork();

    // verify
    ArgumentCaptor<RetentionJob> retentionJobArgument = ArgumentCaptor.forClass(RetentionJob.class);

    ArgumentCaptor<List> dmRequestsArgument = ArgumentCaptor.forClass(List.class);

    verify(dmQueueDaoMock)
        .createRetentionJobUdpateDmStatus(
            retentionJobArgument.capture(), dmRequestsArgument.capture());
    List<DmRequest> dmRequestsArgumentValue = dmRequestsArgument.getValue();

    assertEquals(2, dmRequestsArgumentValue.size());
    assertEquals(dmRequests.get(0).getStatus(), DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
    assertEquals(
        dmRequests.get(0).getDataStorageName(),
        dmRequestsArgumentValue.get(0).getDataStorageName());

    assertEquals(dmRequests.get(1).getStatus(), DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
    assertEquals(
        dmRequests.get(1).getDataStorageName(),
        dmRequestsArgumentValue.get(1).getDataStorageName());
  }

  @Test
  public void testDoWorkPendingRetryDmRequests() throws IOException {
    TransferJob transferJob = createBasicTransferJob();

    Timestamp now = new Timestamp(System.currentTimeMillis());

    List<DmRequest> dmRequests = new ArrayList<>();
    DmRequest dmRequest = new DmRequest();
    dmRequest.setProjectId("projectId");
    dmRequest.setDataStorageRoot("bucket");
    dmRequest.setDataStorageName("gs://bucket/dataset/dir1");
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_PENDING);
    dmRequests.add(dmRequest);

    dmRequest = new DmRequest();
    dmRequest.setProjectId("projectId");
    dmRequest.setDataStorageRoot("bucket");
    dmRequest.setDataStorageName("gs://bucket/dataset/dir2");
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATIUS_RETRY);
    dmRequest.setCreatedAt(now);
    dmRequest.setNumberOfRetry(1);
    dmRequests.add(dmRequest);

    // mock
    when(StsUtil.updateExistingJob(any(), any(), any(), any())).thenReturn(transferJob);
    when(dmQueueDaoMock.getAllAvailableRequestsByPriority()).thenReturn(dmRequests);
    when(ruleExecutorMock.findPooledJob(any(), any(), any(), any())).thenReturn(transferJob);
    doNothing().when(dmQueueDaoMock).createRetentionJobUdpateDmStatus(any(), any());

    // execute
    DmBatchProcessingWorker worker = new DmBatchProcessingWorker(uuid);
    worker.doWork();

    // verify
    ArgumentCaptor<RetentionJob> retentionJobArgument = ArgumentCaptor.forClass(RetentionJob.class);

    ArgumentCaptor<List> dmRequestsArgument = ArgumentCaptor.forClass(List.class);

    verify(dmQueueDaoMock)
        .createRetentionJobUdpateDmStatus(
            retentionJobArgument.capture(), dmRequestsArgument.capture());
    List<DmRequest> dmRequestsArgumentValue = dmRequestsArgument.getValue();

    assertEquals(2, dmRequestsArgumentValue.size());
    assertEquals(dmRequests.get(0).getStatus(), DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
    assertEquals(
        dmRequests.get(0).getDataStorageName(),
        dmRequestsArgumentValue.get(0).getDataStorageName());

    assertEquals(dmRequests.get(1).getStatus(), DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
    assertEquals(
        dmRequests.get(1).getDataStorageName(),
        dmRequestsArgumentValue.get(1).getDataStorageName());
    assertEquals(2, dmRequestsArgumentValue.get(1).getNumberOfRetry());
  }

  @Test
  public void testDoWorkAppendToExistingPrefixList() throws IOException {
    TransferJob transferJob = createBasicTransferJob();
    // create includePrefix list for existing STS job
    ObjectConditions objectConditions = new ObjectConditions();
    List<String> includePrefixList = new ArrayList<>();
    includePrefixList.add("dataset2/dir1");
    includePrefixList.add("dataset2/dir2");
    objectConditions.setIncludePrefixes(includePrefixList);
    transferJob.getTransferSpec().setObjectConditions(objectConditions);

    // create DM requests
    Timestamp now = new Timestamp(System.currentTimeMillis());
    List<DmRequest> dmRequests = new ArrayList<>();
    DmRequest dmRequest = new DmRequest();
    dmRequest.setProjectId("projectId");
    dmRequest.setDataStorageRoot("bucket");
    dmRequest.setDataStorageName("gs://bucket/dataset/dir1");
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_PENDING);
    dmRequests.add(dmRequest);

    dmRequest = new DmRequest();
    dmRequest.setProjectId("projectId");
    dmRequest.setDataStorageRoot("bucket");
    dmRequest.setDataStorageName("gs://bucket/dataset/dir2");
    dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATIUS_RETRY);
    dmRequest.setCreatedAt(now);
    dmRequest.setNumberOfRetry(1);
    dmRequests.add(dmRequest);

    when(StsUtil.updateExistingJob(any(), any(), any(), any())).thenReturn(transferJob);
    when(dmQueueDaoMock.getAllAvailableRequestsByPriority()).thenReturn(dmRequests);
    when(ruleExecutorMock.findPooledJob(any(), any(), any(), any())).thenReturn(transferJob);
    doNothing().when(dmQueueDaoMock).createRetentionJobUdpateDmStatus(any(), any());

    DmBatchProcessingWorker worker = new DmBatchProcessingWorker(uuid);
    worker.doWork();

    ArgumentCaptor<List> includePrefixArgument = ArgumentCaptor.forClass(List.class);
    PowerMockito.verifyStatic(StsUtil.class);
    StsUtil.buildTransferSpec(any(), any(), includePrefixArgument.capture(), any(), any());

    assertEquals(4, includePrefixArgument.getValue().size());
  }

  @Test
  public void testDoWorkMoreThanAllowedDmRequests() throws IOException {
    TransferJob transferJob = createBasicTransferJob();
    // create includePrefix list for existing STS job

    // create DM requests
    List<DmRequest> dmRequests = new ArrayList<>();

    for (int i = 0; i < StsUtil.MAX_PREFIX_COUNT + 100; i++) {
      DmRequest dmRequest = new DmRequest();
      dmRequest.setProjectId("projectId");
      dmRequest.setDataStorageRoot("bucket");
      dmRequest.setDataStorageName("gs://bucket/dataset/dir" + i);
      dmRequest.setStatus(DatabaseConstants.DM_REQUEST_STATUS_PENDING);
      dmRequests.add(dmRequest);
    }

    when(StsUtil.updateExistingJob(any(), any(), any(), any())).thenReturn(transferJob);
    when(dmQueueDaoMock.getAllAvailableRequestsByPriority()).thenReturn(dmRequests);
    when(ruleExecutorMock.findPooledJob(any(), any(), any(), any())).thenReturn(transferJob);
    doNothing().when(dmQueueDaoMock).createRetentionJobUdpateDmStatus(any(), any());

    DmBatchProcessingWorker worker = new DmBatchProcessingWorker(uuid);
    worker.doWork();

    ArgumentCaptor<List> includePrefixArgument = ArgumentCaptor.forClass(List.class);
    PowerMockito.verifyStatic(StsUtil.class);
    StsUtil.buildTransferSpec(any(), any(), includePrefixArgument.capture(), any(), any());

    assertEquals(StsUtil.MAX_PREFIX_COUNT, includePrefixArgument.getValue().size());
    assertTrue(dmRequests.size() > StsUtil.MAX_PREFIX_COUNT);
  }

  private TransferJob createBasicTransferJob() {
    TransferJob transferJob = new TransferJob();
    transferJob.setStatus(StsUtil.STS_ENABLED_STRING);
    TransferSpec transferSpec = new TransferSpec();
    transferJob.setTransferSpec(transferSpec);
    transferJob.setLastModificationTime(
        ZonedDateTime.now(Clock.systemUTC()).minusMinutes(10).toString());
    transferJob.setSchedule(
        new Schedule()
            .setStartTimeOfDay(new TimeOfDay().setHours(10).setMinutes(10).setSeconds(10).setNanos(10)));
    return transferJob;
  }
}
