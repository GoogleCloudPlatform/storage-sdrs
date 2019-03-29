CREATE TABLE `sts_job_pool` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` int(11) NOT NULL,
  `project` varchar(256) NOT NULL,
  `source_bucket` varchar(256) NOT NULL,
  `source_project` varchar(256) NOT NULL,
  `schedule` varchar(256) NOT NULL,
  `updated_at` timestamp(1) NULL DEFAULT NULL,
  `status` varchar(256) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique_sts_job_name` (`project`,`name`),
  KEY `query_project_bucket` (`source_bucket`,`source_project`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8