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
@Table(name ="retention_job_validation")
public class RetentionJobValidation {
	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "id", updatable = false, nullable = false)
	private Integer id;
	
	@Column(name = "retention_job_id")
	private Integer retentionJobId;
	
	@Column(name = "job_operation_name")
	private String jobOperationName;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "status")
	private Enum<?> status;
	
	@Column(name = "created_at")
	private Timestamp createdAt;
	
	@Column(name = "updated_at")
	private Timestamp updatedAt;
	
	public RetentionJobValidation () {
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getRetentionJobId() {
		return retentionJobId;
	}

	public void setRetentionJobId(Integer retentionJobId) {
		this.retentionJobId = retentionJobId;
	}

	public String getJobOperationName() {
		return jobOperationName;
	}

	public void setJobOperationName(String jobOperationName) {
		this.jobOperationName = jobOperationName;
	}

	public Enum<?> getStatus() {
		return status;
	}

	public void setStatus(Enum<?> status) {
		this.status = status;
	}

	public Timestamp getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Timestamp createdAt) {
		this.createdAt = createdAt;
	}

	public Timestamp getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Timestamp updatedAt) {
		this.updatedAt = updatedAt;
	}

}
