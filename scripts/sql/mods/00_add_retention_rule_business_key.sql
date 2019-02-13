USE sdrs;

DROP PROCEDURE IF EXISTS modify_sdrs_add_rule_composite_key;

DELIMITER //

CREATE PROCEDURE modify_sdrs_add_rule_composite_key ( )
BEGIN
  DECLARE indexExists INT;
  SELECT COUNT(*) INTO indexExists
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE table_schema=DATABASE()
    AND table_name='retention_rule'
    AND index_name='unique_dataset_storage_project';

  IF indexExists = 0 THEN
    ALTER TABLE retention_rule
      ADD UNIQUE KEY `unique_dataset_storage_project` (`dataset_name`, `data_storage_name`, `project_id`);
  END IF;
END//

DELIMITER ;

CALL modify_sdrs_add_rule_composite_key;

DROP PROCEDURE modify_sdrs_add_rule_composite_key;
