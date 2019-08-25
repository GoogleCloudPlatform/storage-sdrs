CREATE TABLE `DistributedLockEntry` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT`,
  `lockIdVerificationToken` varchar(256) NOT NULL,
  `lockDuration` int(10) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
