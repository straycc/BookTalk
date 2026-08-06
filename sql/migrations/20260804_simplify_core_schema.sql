-- Apply once to databases initialized before the core-model cleanup.
-- Keep this file for existing environments; sql/booktalk.sql is the clean install schema.

DROP TABLE IF EXISTS book_list_item;
DROP TABLE IF EXISTS book_list;
DROP TABLE IF EXISTS book_review_audit_log;

ALTER TABLE book_review
    DROP INDEX idx_review_book_status_time,
    DROP INDEX idx_review_hot,
    DROP CHECK chk_review_status,
    DROP COLUMN audit_remark,
    DROP COLUMN status,
    ADD KEY idx_review_book_time (book_id, create_time),
    ADD KEY idx_review_hot (hot_score, create_time);
