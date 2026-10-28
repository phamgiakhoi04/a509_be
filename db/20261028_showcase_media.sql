-- REVIEW ONLY: run manually after mysqldump. Never invoked by the application.
-- Use the intended database explicitly before running this file.
-- Existing media flags and period fields may already exist; every ADD is guarded.
SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='images' AND COLUMN_NAME='display_mode')=0,
  'ALTER TABLE images ADD COLUMN display_mode VARCHAR(10) NOT NULL DEFAULT ''GRID''', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='articles' AND COLUMN_NAME='thumbnail_sensitive')=0,
  'ALTER TABLE articles ADD COLUMN thumbnail_sensitive BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='article_images' AND COLUMN_NAME='is_sensitive')=0,
  'ALTER TABLE article_images ADD COLUMN is_sensitive BOOLEAN NOT NULL DEFAULT FALSE', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
