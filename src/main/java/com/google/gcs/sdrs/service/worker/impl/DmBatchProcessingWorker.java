package com.google.gcs.sdrs.service.worker.impl;

import com.google.api.services.storagetransfer.v1.Storagetransfer;
import com.google.api.services.storagetransfer.v1.model.ObjectConditions;
import com.google.api.services.storagetransfer.v1.model.TimeOfDay;
import com.google.api.services.storagetransfer.v1.model.TransferJob;
import com.google.gcs.sdrs.SdrsApplication;
import com.google.gcs.sdrs.common.RetentionRuleType;
import com.google.gcs.sdrs.controller.validation.ValidationConstants;
import com.google.gcs.sdrs.dao.DmQueueDao;
import com.google.gcs.sdrs.dao.LockDao;
import com.google.gcs.sdrs.dao.SingletonDao;
import com.google.gcs.sdrs.dao.model.DistributedLock;
import com.google.gcs.sdrs.dao.model.DmRequest;
import com.google.gcs.sdrs.dao.model.RetentionJob;
import com.google.gcs.sdrs.dao.model.RetentionRule;
import com.google.gcs.sdrs.dao.util.DatabaseConstants;
import com.google.gcs.sdrs.service.worker.BaseWorker;
import com.google.gcs.sdrs.service.worker.WorkerResult.WorkerResultStatus;
import com.google.gcs.sdrs.service.worker.rule.impl.StsRuleExecutor;
import com.google.gcs.sdrs.util.CredentialsUtil;
import com.google.gcs.sdrs.util.RetentionUtil;
import com.google.gcs.sdrs.util.StsUtil;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DmBatchProcessingWorker extends BaseWorker {
  private static final Logger logger = LoggerFactory.getLogger(DmBatchProcessingWorker.class);
  private DmQueueDao dmQueueDao;
  private LockDao lockDao;
  private Storagetransfer client;

  public static final int DEFAULT_DM_LOCK_TIMEOUT = 60000; // one minute by default
  public static final int DEFAULT_DM_MAX_RETRY = 5;
  public static final int DM_MAX_RETRY =
      Integer.valueOf(
          SdrsApplication.getAppConfigProperty(
              "scheduler.task.dmBatchProcessing.maxRetry", String.valueOf(DEFAULT_DM_MAX_RETRY)));
  public static final int DM_LOCK_TIMEOUT =
      Integer.valueOf(
          SdrsApplication.getAppConfigProperty(
              "lock.dm.timeout", String.valueOf(DEFAULT_DM_LOCK_TIMEOUT)));
  public static final String DEFAULT_DM_LOCK_ID = "dm-batch";
  public static final String DM_LOCK_ID =
      SdrsApplication.getAppConfigProperty("lock.dm.id", DEFAULT_DM_LOCK_ID);

  public DmBatchProcessingWorker(String correlationId) {
    super(correlationId);
    try {
      client = StsUtil.createStsClient(CredentialsUtil.getInstance().getCredentials());
    } catch (IOException e) {
      logger.error("Failed to create STS client.", e);
    }
    dmQueueDao = SingletonDao.getDmQueueDao();
    lockDao = SingletonDao.getLockDao();
  }

  @Override
  public void doWork() {
    Session currentLockSession = lockDao.getLockSession();
    try {

      logger.info(String.format("acquiring lock at %s", Instant.now(Clock.systemUTC()).toString()));
      DistributedLock distributedLock =
          lockDao.obtainLock(currentLockSession, DM_LOCK_TIMEOUT, DM_LOCK_ID);
      if (distributedLock != null) {
        logger.info(
            String.format(
                "acquired lock %s at %s",
                distributedLock.getLockToken(), Instant.now(Clock.systemUTC()).toString()));

        // get sorted (by priority) list of all available queue for processing
        List<DmRequest> allAvailableRequetsForProcessing =
            dmQueueDao.getAllAvailableRequestsByPriority();

        // sort the list by bucket while keeping the same order
        Map<String, List<DmRequest>> dmRequestsMap =
            allAvailableRequetsForProcessing.stream()
                .collect(Collectors.groupingBy(DmRequest::getDataStorageRoot));

        List<String> failedDmProcessingBuckets = new ArrayList<>();

        for (String bucket : dmRequestsMap.keySet()) {
          if (!processDmRequestByBucket(bucket, dmRequestsMap.get(bucket))) {
            failedDmProcessingBuckets.add(bucket);
          }
        }

        if (failedDmProcessingBuckets.isEmpty()) {
          logger.info(
              String.format(
                  "Successfully processed DM requests for %d buckets.", dmRequestsMap.size()));
          workerResult.setStatus(WorkerResultStatus.SUCCESS);
        } else {
          logger.error(
              String.format(
                  "DM requests processing failed for %d out of %d buckets.",
                  failedDmProcessingBuckets.size(), dmRequestsMap.size()));
          workerResult.setStatus(WorkerResultStatus.FAILED);
        }

        lockDao.releaseLock(currentLockSession, distributedLock);
        logger.info(
            String.format(
                "released lock %s at %s",
                distributedLock.getLockToken(), Instant.now(Clock.systemUTC()).toString()));
      } else {
        logger.info("Can not acquire lock.");
        workerResult.setStatus(WorkerResultStatus.SUCCESS);
      }
    } catch (Exception e) {
      logger.error("Unknown error. ", e);
      workerResult.setStatus(WorkerResultStatus.FAILED);
    } finally {
      // clean up resource
      lockDao.closeLockSession(currentLockSession);
    }
  }

  private boolean processDmRequestByBucket(String bucket, List<DmRequest> dmRequests) {
    String destinationBucket = StsUtil.buildDestinationBucketName(bucket);

    ZonedDateTime zonedDateTimeNow = ZonedDateTime.now(Clock.systemUTC());
    String scheduleTimeOfDay = zonedDateTimeNow.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    String projectId = dmRequests.get(0).getProjectId();

    TransferJob transferJob = null;
    try {
      transferJob =
          StsRuleExecutor.getInstance()
              .findPooledJob(projectId, bucket, scheduleTimeOfDay, RetentionRuleType.USER);
    } catch (IOException e) {
      // Can't allocate the job from the pool. Fail immediately.
      return false;
    }

    TimeOfDay jobRunAtTimeOfDay = transferJob.getSchedule().getStartTimeOfDay();
    ZonedDateTime lastRunTime =
        ZonedDateTime.of(
            zonedDateTimeNow.getYear(),
            zonedDateTimeNow.getMonthValue(),
            zonedDateTimeNow.getDayOfMonth(),
            jobRunAtTimeOfDay.getHours(),
            jobRunAtTimeOfDay.getMinutes(),
            jobRunAtTimeOfDay.getSeconds(),
            jobRunAtTimeOfDay.getNanos() != null ? jobRunAtTimeOfDay.getNanos() : 0,
            ZoneId.of("UTC"));
    ZonedDateTime lastModifiedTime = ZonedDateTime.parse(transferJob.getLastModificationTime());
    List<String> existingIncludePrefixList = new ArrayList<>();
    if (transferJob.getStatus().equals(StsUtil.STS_ENABLED_STRING)) {
      ObjectConditions objectConditions = transferJob.getTransferSpec().getObjectConditions();
      if (objectConditions != null && objectConditions.getIncludePrefixes() != null) {
        existingIncludePrefixList = objectConditions.getIncludePrefixes();
      }
    }

    int initPrefxiNumber = 0;

    Set<String> newIncludePrefixSet = new HashSet<>();
    // Replace the existing prefix list if last modified time is older than the last job run time,
    // meaning the daily STS job has already run and the existing prefix list has been processed.
    if (lastModifiedTime.isBefore(lastRunTime.minusHours(24))) {
      initPrefxiNumber = StsUtil.MAX_PREFIX_COUNT;
    } else {
      initPrefxiNumber = StsUtil.MAX_PREFIX_COUNT - existingIncludePrefixList.size();
      newIncludePrefixSet.addAll(existingIncludePrefixList);
    }

    int maxCount = Math.min(initPrefxiNumber, dmRequests.size());
    int total = 0;
    int start = 0;
    List<String> newIncludePrefixList = null;

    while (total < StsUtil.MAX_PREFIX_COUNT && start < dmRequests.size()) {
      for (int i = 0; i < maxCount; i++) {
        String prefix =
            RetentionUtil.getDatasetPath(dmRequests.get(i + start).getDataStorageName());
        if (prefix != null && !prefix.isEmpty()) {
          if (!prefix.endsWith("/")) {
            prefix = prefix + "/";
          }
          newIncludePrefixSet.add(prefix);
        }
      }
      newIncludePrefixList =
          RetentionUtil.consolidateDmPrefixes(new ArrayList<>(newIncludePrefixSet));
      total = newIncludePrefixList.size();
      start = maxCount + start;
      maxCount = Math.min(StsUtil.MAX_PREFIX_COUNT - total, dmRequests.size() - start);
      newIncludePrefixSet = new HashSet<>(newIncludePrefixList);
    }
    // update STS job
    TransferJob jobToUpdate =
        new TransferJob()
            .setDescription(
                StsRuleExecutor.buildDescription(
                    RetentionRuleType.USER.toString(), null, scheduleTimeOfDay))
            .setTransferSpec(
                StsUtil.buildTransferSpec(
                    bucket, destinationBucket, newIncludePrefixList, false, null))
            .setStatus(StsUtil.STS_ENABLED_STRING);
    try {
      transferJob =
          StsUtil.updateExistingJob(client, jobToUpdate, transferJob.getName(), projectId);
    } catch (IOException e) {
      // Update STS job failed. Fail the process immediately.
      logger.error("Failed to update STS job.", e);
      return false;
    }

    RetentionRule retentionRule = new RetentionRule();
    retentionRule.setProjectId(projectId);
    retentionRule.setDataStorageName(ValidationConstants.STORAGE_PREFIX + bucket);
    retentionRule.setType(RetentionRuleType.USER);

    RetentionJob retentionJob =
        StsRuleExecutor.buildRetentionJobEntity(
            transferJob.getName(),
            retentionRule,
            StsUtil.convertPrefixToString(newIncludePrefixList),
            new Timestamp(Instant.parse(transferJob.getLastModificationTime()).toEpochMilli()));
    retentionJob.setBatchId(getUuid());

    // update retention_job and dm_queue tables
    List<DmRequest> processedDmRequests = dmRequests.subList(0, start);
    dmRequests.stream()
        .forEach(
            request -> {
              if (request.getStatus().equals(DatabaseConstants.DM_REQUEST_STATIUS_RETRY)) {
                request.setNumberOfRetry(request.getNumberOfRetry() + 1);
                request.setPriority(
                    RetentionUtil.generatePriority(
                        request.getNumberOfRetry(),
                        request.getCreatedAt().toInstant().toEpochMilli()));
              }
              request.setStatus(DatabaseConstants.DM_REQUEST_STATUS_SCHEDULED);
            });

    try {
      dmQueueDao.createRetentionJobUdpateDmStatus(retentionJob, processedDmRequests);
    } catch (IOException e) {
      // Database transaction failed. However the process is still considered success as STS job has
      // been updated successfully.
      logger.error("Failed to create retention job and update DM request status.");
    }

    return true;
  }
}
