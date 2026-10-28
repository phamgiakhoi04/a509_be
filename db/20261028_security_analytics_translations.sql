-- Additive migration for the local a509_db. Back up before executing.
-- Existing content, passwords and image/flag records are not rewritten.
SET @add_token_version = IF(
  (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='users' AND column_name='token_version')=0,
  'ALTER TABLE users ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0',
  'SELECT 1'
);
PREPARE token_version_statement FROM @add_token_version;
EXECUTE token_version_statement;
DEALLOCATE PREPARE token_version_statement;

CREATE TABLE IF NOT EXISTS page_views (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 event_id VARCHAR(36) NOT NULL,
 viewed_at DATETIME(6) NOT NULL,
 visitor_hash VARCHAR(64) NOT NULL,
 session_hash VARCHAR(64) NOT NULL,
 path VARCHAR(500) NOT NULL,
 referrer VARCHAR(253) NOT NULL,
 device VARCHAR(12) NOT NULL,
 country VARCHAR(2) NOT NULL,
 region VARCHAR(100) NOT NULL,
 UNIQUE KEY idx_views_event (event_id),
 KEY idx_views_time (viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS content_translations (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 resource_type VARCHAR(30) NOT NULL,
 resource_id BIGINT NOT NULL,
 field_name VARCHAR(40) NOT NULL,
 language VARCHAR(5) NOT NULL,
 translated_text LONGTEXT NOT NULL,
 source_hash VARCHAR(64) NOT NULL,
 updated_at DATETIME(6) NOT NULL,
 UNIQUE KEY uq_translation_field (resource_type,resource_id,field_name,language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
