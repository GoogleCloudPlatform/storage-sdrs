package com.google.gcs.sdrs.dao;

import java.util.List;

import com.google.gcs.sdrs.dao.model.DMQueueTableEntry;

public interface DMQueueTableDao extends Dao<DMQueueTableEntry, Integer> {

  List<DMQueueTableEntry> getAllAvailableQueueForProcessingSTSJobs();


  List<DMQueueTableEntry> getQueueEntryForSTSLock();



}
