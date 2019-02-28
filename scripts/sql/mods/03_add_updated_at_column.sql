USE sdrs;

DROP PROCEDURE IF EXISTS modify_sdrs_job_updated_at;

DELIMITER //

CREATE PROCEDURE modify_sdrs_job_updated_at ( )
BEGIN
  DECLARE columnExists INT;

  -- Drop the old 3 value index if it still exists
  SELECT COUNT(*) INTO columnExists
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE table_schema='sdrs'
    AND table_name='retention_job'
    AND column_name ='updated_at';

  IF columnExists = 0 THEN
    ALTER TABLE retention_job
    ADD COLUMN `updated_at` timestamp NULL;
  END IF;

  ALTER TABLE retention_job
  MODIFY column updated_at timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP;
END//

DELIMITER ;

CALL modify_sdrs_job_updated_at;

DROP PROCEDURE modify_sdrs_job_updated_at;
