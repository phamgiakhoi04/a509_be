USE a509_db;

-- Optional link from an archive/article image to a uniform/object page.
-- The statement is safe to run more than once.
SET @column_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'article_images'
      AND column_name = 'reference_uniform_id'
);
SET @sql := IF(
    @column_exists = 0,
    'ALTER TABLE article_images ADD COLUMN reference_uniform_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
