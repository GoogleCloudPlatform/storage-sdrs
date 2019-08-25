CREATE TABLE `DMQueueTable` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `data_storage_name` varchar(256) NOT NULL,
  `status` varchar(256) DEFAULT NULL,
  `priority` int(10) NOT NULL DEFAULT 0,
  `data_storage_root varchar(256)` NOT NULL,
  `retention_job_id` , int(10),
  `number_of_retry` int(10) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  FOREIGN KEY (retention_job_id) REFERENCES retention_job(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
