-- Repeatable local demo data for BookTalk.
-- Test accounts (all): password = BookTalk@123

USE book_talk;

INSERT IGNORE INTO `user` (id, username, password, email, status, role) VALUES
    (2, 'alice', '$2a$10$2HVhKJhJVbO5Jar1x0qy9.rH51DfoF3yCFb2Eu7wFIT2YdWHrbOQC', 'alice@booktalk.local', 1, 'user'),
    (3, 'bob', '$2a$10$2HVhKJhJVbO5Jar1x0qy9.rH51DfoF3yCFb2Eu7wFIT2YdWHrbOQC', 'bob@booktalk.local', 1, 'user'),
    (4, 'carol', '$2a$10$2HVhKJhJVbO5Jar1x0qy9.rH51DfoF3yCFb2Eu7wFIT2YdWHrbOQC', 'carol@booktalk.local', 1, 'user');

INSERT IGNORE INTO user_info (user_id, nickname, avatar_url, gender, region, signature) VALUES
    (1, 'BookTalk Admin', NULL, 'O', 'Local', 'Local administrator'),
    (2, 'Alice Reader', NULL, 'F', 'Shanghai', 'Science fiction and software books'),
    (3, 'Bob Books', NULL, 'M', 'Beijing', 'History and nonfiction reader'),
    (4, 'Carol Notes', NULL, 'F', 'Shenzhen', 'Keeps a practical reading list');

-- INSERT IGNORE does not repair a stale local admin profile from an older seed.
UPDATE user_info
SET nickname = 'BookTalk Admin', region = 'Local', signature = 'Local administrator'
WHERE user_id = 1;

INSERT IGNORE INTO category (id, name, description) VALUES
    (1, 'Literature', 'Fiction, poetry, and essays'),
    (2, 'Technology', 'Software and natural sciences'),
    (3, 'Humanities', 'History, philosophy, and society'),
    (4, 'Life', 'Psychology and personal growth'),
    (5, 'Children', 'Books for children and young readers');

INSERT IGNORE INTO book (id, isbn, title, description, author, publisher, publish_date, cover_url,
                         page_count, category_id, average_score, stars5_top, stars4_top, stars3_top,
                         stars2_top, stars1_top, score_count, favorite_count, hot_score, hot_score_update_time)
VALUES
    (10001, '9780000000001', 'Clean Code', 'A practical guide to writing maintainable software.', 'Robert C. Martin', 'Prentice Hall', '2008-08-01', NULL, 464, 2, 0, 70, 20, 8, 1, 1, 0, 2, 92.5000, NOW(3)),
    (10002, '9780000000002', 'Sapiens', 'A brief history of humankind.', 'Yuval Noah Harari', 'Harper', '2015-02-10', NULL, 464, 3, 0, 65, 25, 8, 1, 1, 0, 2, 87.2000, NOW(3)),
    (10003, '9780000000003', 'The Three-Body Problem', 'A science-fiction novel about first contact.', 'Cixin Liu', 'Tor Books', '2014-11-11', NULL, 400, 1, 0, 80, 15, 4, 1, 0, 0, 3, 96.8000, NOW(3)),
    (10004, '9780000000004', 'Deep Work', 'Rules for focused success in a distracted world.', 'Cal Newport', 'Grand Central', '2016-01-05', NULL, 304, 4, 0, 55, 30, 10, 3, 2, 0, 1, 79.4000, NOW(3)),
    (10005, '9780000000005', 'The Little Prince', 'A classic story about friendship and imagination.', 'Antoine de Saint-Exupery', 'Reynal and Hitchcock', '1943-04-06', NULL, 96, 5, 0, 75, 20, 4, 1, 0, 0, 1, 84.6000, NOW(3));

INSERT IGNORE INTO tag (id, creator_id, category_id, name, usage_count, description) VALUES
    (20001, 1, 2, 'software', 2, 'Software engineering'),
    (20002, 1, 3, 'history', 1, 'History and society'),
    (20003, 1, 1, 'science-fiction', 2, 'Science fiction'),
    (20004, 1, 4, 'productivity', 2, 'Focus and personal effectiveness'),
    (20005, 1, 5, 'classic', 1, 'Classic books');

INSERT IGNORE INTO book_tag_relation (book_id, tag_id) VALUES
    (10001, 20001), (10001, 20004), (10002, 20002), (10003, 20003), (10004, 20004), (10005, 20005);

INSERT IGNORE INTO book_shelf (id, user_id, book_id, status) VALUES
    (40001, 2, 10001, 'READ'), (40002, 2, 10003, 'READ'), (40003, 2, 10004, 'READING'),
    (40004, 3, 10002, 'READ'), (40005, 3, 10003, 'WANT_TO_READ'),
    (40006, 4, 10001, 'WANT_TO_READ'), (40007, 4, 10005, 'READ');

INSERT IGNORE INTO book_review (id, book_id, user_id, type, title, content, score, like_count, reply_count, hot_score, hot_score_update_time, create_time, update_time) VALUES
    (30001, 10001, 2, 1, 'Still useful for everyday code', 'The examples are direct and make refactoring decisions easier.', 9, 2, 2, 35.5000, NOW(3), DATE_SUB(NOW(3), INTERVAL 5 DAY), DATE_SUB(NOW(3), INTERVAL 2 DAY)),
    (30002, 10003, 3, 0, NULL, 'A strong first-contact story with memorable ideas.', 9, 1, 1, 42.1000, NOW(3), DATE_SUB(NOW(3), INTERVAL 3 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (30003, 10004, 4, 0, NULL, 'Helpful for rebuilding a focused work routine.', 8, 0, 0, 18.7000, NOW(3), DATE_SUB(NOW(3), INTERVAL 1 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY));

UPDATE book b
LEFT JOIN (
    SELECT book_id, ROUND(AVG(score), 2) AS average_score, COUNT(score) AS score_count
    FROM book_review
    WHERE score IS NOT NULL
    GROUP BY book_id
) review_stats ON review_stats.book_id = b.id
SET b.average_score = COALESCE(review_stats.average_score, 0),
    b.score_count = COALESCE(review_stats.score_count, 0);

INSERT IGNORE INTO post (id, user_id, title, content, summary, related_book_id, view_count, like_count, comment_count, hot_score, status, last_active_time, hot_score_update_time, create_time, update_time) VALUES
    (50001, 2, 'How do you keep reading notes?', 'I am comparing paper notes with a lightweight markdown workflow.', 'Comparing reading-note workflows.', 10001, 32, 2, 2, 31.2000, 1, DATE_SUB(NOW(3), INTERVAL 3 HOUR), NOW(3), DATE_SUB(NOW(3), INTERVAL 2 DAY), DATE_SUB(NOW(3), INTERVAL 3 HOUR)),
    (50002, 3, 'Favorite science-fiction translations', 'Which translation made a complex science-fiction book easier to read?', 'Discussion about science-fiction translations.', 10003, 18, 1, 1, 22.5000, 1, DATE_SUB(NOW(3), INTERVAL 1 DAY), NOW(3), DATE_SUB(NOW(3), INTERVAL 4 DAY), DATE_SUB(NOW(3), INTERVAL 1 DAY));

INSERT IGNORE INTO comment (id, root_id, target_type, parent_id, user_id, content, create_time) VALUES
    (60001, 30001, 'REVIEW', NULL, 3, 'The chapter on naming is a useful team discussion starter.', DATE_SUB(NOW(3), INTERVAL 2 DAY)),
    (60002, 30001, 'REVIEW', 60001, 2, 'Agreed. It gives us a shared vocabulary for review comments.', DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (60003, 30002, 'REVIEW', NULL, 4, 'The cultural context is what stayed with me.', DATE_SUB(NOW(3), INTERVAL 12 HOUR)),
    (60004, 50001, 'POST', NULL, 4, 'I use one markdown file per book and a single index page.', DATE_SUB(NOW(3), INTERVAL 5 HOUR)),
    (60005, 50001, 'POST', 60004, 2, 'That sounds easy to search later. Thanks.', DATE_SUB(NOW(3), INTERVAL 3 HOUR)),
    (60006, 50002, 'POST', NULL, 2, 'A translator note at the end would help readers compare editions.', DATE_SUB(NOW(3), INTERVAL 18 HOUR));

INSERT IGNORE INTO like_record (id, user_id, target_type, target_id, create_time) VALUES
    (70001, 3, 'REVIEW', 30001, DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (70002, 4, 'REVIEW', 30001, DATE_SUB(NOW(3), INTERVAL 20 HOUR)),
    (70003, 2, 'REVIEW', 30002, DATE_SUB(NOW(3), INTERVAL 8 HOUR)),
    (70004, 3, 'POST', 50001, DATE_SUB(NOW(3), INTERVAL 4 HOUR)),
    (70005, 4, 'POST', 50001, DATE_SUB(NOW(3), INTERVAL 3 HOUR)),
    (70006, 2, 'POST', 50002, DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (70007, 4, 'COMMENT', 60001, DATE_SUB(NOW(3), INTERVAL 20 HOUR));

INSERT IGNORE INTO content_tag_relation (id, content_id, content_type, tag_id, create_time) VALUES
    (80001, 30001, 'REVIEW', 20001, NOW(3)), (80002, 30001, 'REVIEW', 20004, NOW(3)),
    (80003, 30002, 'REVIEW', 20003, NOW(3)), (80004, 50001, 'POST', 20001, NOW(3)),
    (80005, 50001, 'POST', 20004, NOW(3)), (80006, 50002, 'POST', 20003, NOW(3));

INSERT IGNORE INTO notification (user_id, type, title, content, target_id, target_type, sender_id, sender_name, is_read, is_deleted, create_time) VALUES
    (2, 'LIKE', 'New like', 'Bob liked your review.', 30001, 'REVIEW', 3, 'Bob Books', FALSE, FALSE, DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (2, 'COMMENT', 'New reply', 'Bob commented on your review.', 60001, 'COMMENT', 3, 'Bob Books', FALSE, FALSE, DATE_SUB(NOW(3), INTERVAL 20 HOUR)),
    (3, 'LIKE', 'New like', 'Alice liked your post.', 50002, 'POST', 2, 'Alice Reader', TRUE, FALSE, DATE_SUB(NOW(3), INTERVAL 1 DAY));

INSERT IGNORE INTO user_behavior_log (id, user_id, target_id, target_type, behavior_type, behavior_score, extra_data, create_time) VALUES
    (90001, 2, 10001, 'BOOK', 'BOOK_VIEW', 1.0000, JSON_OBJECT('source', 'demo'), DATE_SUB(NOW(3), INTERVAL 3 DAY)),
    (90002, 2, 10003, 'BOOK', 'BOOK_REVIEW', 5.0000, JSON_OBJECT('score', 10), DATE_SUB(NOW(3), INTERVAL 2 DAY)),
    (90003, 2, 10004, 'BOOK', 'BOOK_SHELF', 3.0000, JSON_OBJECT('status', 'READING'), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (90004, 3, 10002, 'BOOK', 'BOOK_VIEW', 1.0000, JSON_OBJECT('source', 'demo'), DATE_SUB(NOW(3), INTERVAL 2 DAY)),
    (90005, 3, 10003, 'BOOK', 'BOOK_REVIEW', 4.5000, JSON_OBJECT('score', 9), DATE_SUB(NOW(3), INTERVAL 1 DAY)),
    (90006, 4, 10005, 'BOOK', 'BOOK_VIEW', 1.0000, JSON_OBJECT('source', 'demo'), DATE_SUB(NOW(3), INTERVAL 1 DAY));

INSERT IGNORE INTO user_interest_tag (id, user_id, interest_type, interest_key, interest_score, behavior_count, update_time, create_time) VALUES
    (91001, 2, 'TAG', 'software', 8.0000, 3, NOW(3), NOW(3)),
    (91002, 2, 'TAG', 'science-fiction', 7.0000, 2, NOW(3), NOW(3)),
    (91003, 2, 'CATEGORY', '2', 6.0000, 3, NOW(3), NOW(3)),
    (91004, 3, 'TAG', 'history', 7.5000, 2, NOW(3), NOW(3)),
    (91005, 3, 'CATEGORY', '3', 6.5000, 2, NOW(3), NOW(3)),
    (91006, 4, 'TAG', 'productivity', 6.0000, 2, NOW(3), NOW(3));
