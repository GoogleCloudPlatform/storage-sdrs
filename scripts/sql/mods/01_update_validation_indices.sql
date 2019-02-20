USE sdrs;

DROP PROCEDURE IF EXISTS update_validation_indices;

DELIMITER //

CREATE PROCEDURE update_validation_indices ( )
BEGIN
  DECLARE oldIndexExists INT;
  DECLARE indexExists INT;

  SELECT COUNT(*) INTO oldIndexExists
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE table_schema=DATABASE()
    AND table_name='retention_job_validation'
    AND index_name='retention_job_validation_status';

  -- Delete old index if it exists
  IF oldIndexExists > 0 THEN
    DROP INDEX retention_job_validation_status on retention_job_validation;
  END IF;

  SELECT COUNT(*) INTO indexExists
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE table_schema=DATABASE()
    AND table_name='retention_job_validation'
    AND index_name='retention_job_validation_job_operation_name';

  -- Delete new index if it exists to make sure we create it correctly
  IF indexExists > 0 THEN
    DROP INDEX retention_job_validation_job_operation_name on retention_job_validation;
  END IF;

  -- Create new index
  CREATE INDEX retention_job_validation_job_operation_name on retention_job_validation (job_operation_name);
END//

DELIMITER ;

CALL update_validation_indices;

DROP PROCEDURE update_validation_indices;
