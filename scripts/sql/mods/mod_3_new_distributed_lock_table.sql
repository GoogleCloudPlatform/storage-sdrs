CREATE TABLE `distributed_lock` (
  `id` varchar(128) NOT NULL,
  `lock_token` varchar(256) NOT NULL,
  `lock_duration` int(10) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
