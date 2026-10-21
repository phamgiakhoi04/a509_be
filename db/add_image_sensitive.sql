USE a509_db;

SET @has_sensitive = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'images'
      AND COLUMN_NAME = 'is_sensitive'
);
SET @add_sensitive = IF(@has_sensitive = 0,
    'ALTER TABLE images ADD COLUMN is_sensitive BOOLEAN NOT NULL DEFAULT FALSE',
    'SELECT 1');
PREPARE stmt FROM @add_sensitive;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
