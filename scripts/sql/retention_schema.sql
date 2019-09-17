-- Schema Script
CREATE DATABASE IF NOT EXISTS sdrs;
USE sdrs;

-- Drop Tables
DROP TABLE IF EXISTS retention_execution;
DROP TABLE IF EXISTS retention_job_validation;
DROP TABLE IF EXISTS retention_job;
DROP TABLE IF EXISTS retention_rule_history;
DROP TABLE IF EXISTS retention_rule;
DROP TABLE IF EXISTS pooled_sts_job;
DROP TABLE IF EXISTS dm_queue;
DROP TABLE IF EXISTS distributed_lock;



-- Table Create Scripts
-- ----------------------------------------------------------
CREATE TABLE retention_rule (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dataset_name` varchar(256) NULL,
  `retention_value` text NOT NULL,
  `data_storage_name` varchar(256) NULL,
  `data_storage_root` varchar(256) NOT NULL,
  `data_storage_type` varchar(128) NOT NULL,
  `project_id` varchar(256) NOT NULL,
  `type` enum('global', 'dataset', 'default') NOT NULL,
  `version` int UNSIGNED NOT NULL DEFAULT 0,
  `is_active` bit NOT NULL DEFAULT 1,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `user` varchar(256) NOT NULL,
  `metadata` text NULL,
  UNIQUE KEY `unique_storage_project_type` (`data_storage_name`, `project_id`, `type`),
  INDEX `retention_rule_dataset_name` (`dataset_name`),
  INDEX `retention_rule_is_active` (`is_active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE retention_rule_history (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `retention_rule_id` int UNSIGNED NOT NULL,
  `dataset_name` varchar(256) NULL,
  `retention_value` text NOT NULL,
  `data_storage_name` varchar(256) NULL,
  `data_storage_root` varchar(256) NOT NULL,
  `data_storage_type` varchar(256) NOT NULL,
  `project_id` varchar(256) NOT NULL,
  `type` enum('global', 'dataset', 'default') NOT NULL,
  `version` int UNSIGNED NOT NULL DEFAULT 0,
  `is_active` bit NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `user` varchar(256) NOT NULL,
  `metadata` text NULL,
  FOREIGN KEY (retention_rule_id) REFERENCES retention_rule(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE retention_job (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `name` varchar(256) NOT NULL,
  `retention_rule_id` int UNSIGNED NULL,
  `retention_rule_version` int UNSIGNED NULL,
  `retention_rule_type` enum('global', 'dataset', 'user', 'default') NULL,
  `retention_rule_data_storage_name` varchar(256) NOT NULL,
  `retention_rule_project_id` varchar(256) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `type` varchar(128) DEFAULT NULL,
  `batch_id` varchar(256) DEFAULT NULL,
  `metadata` text,
  `data_storage_root` varchar(256) DEFAULT NULL,
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
  `batch_id` varchar(256) DEFAULT NULL,
  `start_time` timestamp NULL DEFAULT NULL,
  `end_time` timestamp NULL DEFAULT NULL,
  `metadata` text,
  FOREIGN KEY (retention_job_id) REFERENCES retention_job(id),
  INDEX `retention_job_validation_job_operation_name` (`job_operation_name`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `pooled_sts_job` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(256) NOT NULL,
  `project_id` varchar(256) NOT NULL,
  `type` varchar(256) NOT NULL,
  `schedule` varchar(256) NOT NULL,
  `source_bucket` varchar(256) NOT NULL,
  `source_project` varchar(256) NOT NULL,
  `target_bucket` varchar(256) DEFAULT NULL,
  `target_project` varchar(256) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `status` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_sts_job_name` (`project_id`,`name`),
  KEY `query_project_bucket` (`source_bucket`,`source_project`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `dm_queue` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `data_storage_name` varchar(256) NOT NULL,
  `status` varchar(256) NOT NULL,
  `priority` int(10) unsigned NOT NULL DEFAULT 0,
  `data_storage_root` varchar(256) NOT NULL,
  `retention_job_id` int(10) unsigned,
  `number_of_retry` int(10) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `distributed_lock` (
  `id` varchar(128) NOT NULL,
  `lock_token` varchar(256) NOT NULL,
  `lock_duration` int(10) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


-- TRIGGER SCRIPTS
-- ------------------------------------------------------


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
      retention_value,
      data_storage_name,
      data_storage_root,
      data_storage_type,
      project_id,
      `type`,
      version,
      is_active,
      `user`,
      metadata
    )
    VALUES (
      OLD.id,
      OLD.dataset_name,
      OLD.retention_value,
      OLD.data_storage_name,
      OLD.data_storage_root,
      OLD.data_storage_type,
      OLD.project_id,
      OLD.`type`,
      OLD.version,
      OLD.is_active,
      OLD.`user`,
      OLD.metadata
    );
END //

DELIMITER//


DELIMITER ;
