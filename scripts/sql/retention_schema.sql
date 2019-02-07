-- Schema Script
CREATE DATABASE IF NOT EXISTS sdrs;
USE sdrs;

-- Drop Tables
DROP TABLE IF EXISTS retention_execution;
DROP TABLE IF EXISTS retention_job_validation;
DROP TABLE IF EXISTS retention_job;
DROP TABLE IF EXISTS retention_rule_history;
DROP TABLE IF EXISTS retention_rule;

-- Table Create Scripts
-- ----------------------------------------------------------
CREATE TABLE retention_rule (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dataset_name` varchar(256) NULL,
  `retention_period_in_days` int UNSIGNED NOT NULL,
  `data_storage_name` varchar(256) NULL,
  `project_id` varchar(256) NOT NULL,
  `type` enum('global', 'dataset') NOT NULL,
  `version` int UNSIGNED NOT NULL DEFAULT 0,
  `is_active` bit NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `user` varchar(256) NOT NULL,
  UNIQUE KEY `unique_dataset_storage_project` (`dataset_name`, `data_storage_name`, `project_id`),
  INDEX `retention_rule_dataset_name` (`dataset_name`),
  INDEX `retention_rule_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE retention_rule_history (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `retention_rule_id` int UNSIGNED NOT NULL,
  `dataset_name` varchar(256) NULL,
  `retention_period_in_days` int UNSIGNED NOT NULL,
  `data_storage_name` varchar(256) NULL,
  `project_id` varchar(256) NOT NULL,
  `type` enum('global', 'dataset') NOT NULL,
  `version` int UNSIGNED NOT NULL DEFAULT 0,
  `is_active` bit NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user` varchar(256) NOT NULL,

  FOREIGN KEY (retention_rule_id) REFERENCES retention_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE retention_job (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(256) NOT NULL,
  `retention_rule_id` int UNSIGNED NULL,
  `retention_rule_version` int UNSIGNED NULL,
  `retention_rule_type` enum('global', 'dataset', 'marker') NULL,
  `retention_rule_data_storage_name` varchar(256) NOT NULL,
  `retention_rule_project_id` varchar(256) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (retention_rule_id) REFERENCES retention_rule(id),
  INDEX `retention_job_name` (`name`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE retention_job_validation (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `retention_job_id` int UNSIGNED NOT NULL,
  `job_operation_name` varchar(256) NOT NULL,
  `status` enum('success','pending','error') NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (retention_job_id) REFERENCES retention_job(id),
  INDEX `retention_job_validation_status` (`status`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE retention_execution (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `executor_id` varchar(256) NOT NULL,
  `data_storage_type` enum('GCS','BQ') NOT NULL,
  `retention_job_id` int UNSIGNED NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (retention_job_id) REFERENCES retention_job(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- TRIGGER SCRIPTS
-- ------------------------------------------------------

DROP TRIGGER IF EXISTS update_retention_rule;

DELIMITER //

CREATE TRIGGER update_retention_rule
AFTER UPDATE ON retention_rule
FOR EACH ROW
BEGIN
    INSERT INTO retention_rule_history (
      retention_rule_id,
      dataset_name,
      retention_period_in_days,
      data_storage_name,
      project_id,
      `type`,
      version,
      is_active,
      `user`
    )
    VALUES (
      OLD.id,
      OLD.dataset_name,
      OLD.retention_period_in_days,
      OLD.data_storage_name,
      OLD.project_id,
      OLD.`type`,
      OLD.version,
      OLD.is_active,
      OLD.`user`
    );
END //

DELIMITER ;