package com.google.gcs.sdrs.dao.model;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 
 * Note - coding to JPA specification, not Hibernate specific annotations 
 *
 */
@Entity
@Table(name ="retention_execution")
public class RetentionExecution {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Integer id;
	
	@Column(name = "executor_id")
	private String executorId;

	@Enumerated(EnumType.STRING)
	@Column(name = "data_storage_type")
	private Enum<?> dataStorageType;

	@Column(name = "retention_job_id")
	private Integer retentionJobId;

	@Column(name = "created_at")
	private Timestamp createdAt;
	
	public RetentionExecution () {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getExecutorId() {
		return executorId;
	}

	public void setExecutorId(String executorId) {
		this.executorId = executorId;
	}

	public Enum<?> getDataStorageType() {
		return dataStorageType;
	}

	public void setDataStorageType(Enum<?> dataStorageType) {
		this.dataStorageType = dataStorageType;
	}

	public Integer getRetentionJobId() {
		return retentionJobId;
	}

	public void setRetentionJobId(Integer retentionJobId) {
		this.retentionJobId = retentionJobId;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

}
