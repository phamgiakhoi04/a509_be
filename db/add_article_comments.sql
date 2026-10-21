USE a509_db;

-- Comments may belong to a uniform or an article.
-- Open this file in MySQL Workbench and execute it; do not use SOURCE in the SQL editor.
SET @uniform_nullable = (
    SELECT IF(EXISTS(
        SELECT 1
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'comments'
          AND COLUMN_NAME = 'uniform_id'
          AND IS_NULLABLE = 'NO'
    ), 'ALTER TABLE comments MODIFY COLUMN uniform_id BIGINT NULL', 'SELECT 1')
);
PREPARE stmt FROM @uniform_nullable;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_article_id = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'article_id'
);
SET @add_article_id = IF(
    @has_article_id = 0,
    'ALTER TABLE comments ADD COLUMN article_id BIGINT NULL',
    'SELECT 1'
);
PREPARE stmt FROM @add_article_id;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_article_fk = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'comments'
      AND COLUMN_NAME = 'article_id'
      AND REFERENCED_TABLE_NAME = 'articles'
);
SET @add_article_fk = IF(
    @has_article_fk = 0,
    'ALTER TABLE comments ADD CONSTRAINT fk_comments_article FOREIGN KEY (article_id) REFERENCES articles(article_id) ON DELETE CASCADE',
    'SELECT 1'
);
PREPARE stmt FROM @add_article_fk;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
