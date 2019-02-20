USE sdrs;

DROP PROCEDURE IF EXISTS modify_sdrs_add_rule_composite_key;

DELIMITER //

CREATE PROCEDURE modify_sdrs_add_rule_composite_key ( )
BEGIN
  DECLARE oldIndexExists INT;
  DECLARE indexExists INT;

  -- Drop the old 3 value index if it still exists
  SELECT COUNT(*) INTO oldIndexExists
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE table_schema=DATABASE()
    AND table_name='retention_rule'
    AND index_name='unique_dataset_storage_project';

  IF oldIndexExists > 0 THEN
    DROP INDEX unique_dataset_storage_project on retention_rule;
  END IF;

  -- Create new 2 value index if it doesn't exist
  SELECT COUNT(*) INTO indexExists
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE table_schema=DATABASE()
    AND table_name='retention_rule'
    AND index_name='unique_storage_project';

  IF indexExists = 0 THEN
    ALTER TABLE retention_rule
    ADD UNIQUE KEY `unique_storage_project` (`data_storage_name`, `project_id`);
  END IF;
END//

DELIMITER ;

CALL modify_sdrs_add_rule_composite_key;

DROP PROCEDURE modify_sdrs_add_rule_composite_key;
