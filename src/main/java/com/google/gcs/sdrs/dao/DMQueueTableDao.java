package com.google.gcs.sdrs.dao;

import com.google.gcs.sdrs.dao.model.DMQueueTableEntry;
import java.util.List;

public interface DMQueueTableDao extends Dao<DMQueueTableEntry, Integer> {

  List<DMQueueTableEntry> getAllAvailableQueueForProcessingSTSJobs();


  List<DMQueueTableEntry> getQueueEntryForSTSLock();

}
