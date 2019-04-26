CREATE TABLE `sts_job_pool` (
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8