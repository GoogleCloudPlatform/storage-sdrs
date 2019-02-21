USE sdrs;

DROP PROCEDURE IF EXISTS update_retention_type_enum;

DELIMITER //

CREATE PROCEDURE update_retention_type_enum ( )
BEGIN
  -- Create a new temporary column with the new enum values
  ALTER TABLE `retention_job` ADD `new_retention_rule_type` ENUM('global', 'dataset', 'user') NULL AFTER `retention_rule_version`;

  -- Copy over the existing column values, replacing marker with user
  UPDATE `retention_job` SET `new_retention_rule_type` = `retention_rule_type` WHERE `retention_rule_type` != 'marker';
  UPDATE `retention_job` SET `new_retention_rule_type` = 'user' WHERE `retention_rule_type` = 'marker';

  -- Delete the old column
  ALTER TABLE `retention_job` DROP `retention_rule_type`;

  -- Rename the new column to match the old one
  ALTER TABLE `retention_job` CHANGE `new_retention_rule_type` `retention_rule_type` ENUM('global', 'dataset', 'user') NULL AFTER `retention_rule_version`;
END//

DELIMITER ;

CALL update_retention_type_enum;

DROP PROCEDURE update_retention_type_enum;
