-- Apply once to databases that still use book_rating as a separate score source.
-- A rating is copied only when the same user already has an unscored review.

UPDATE book_review review
JOIN book_rating rating
  ON rating.book_id = review.book_id
 AND rating.user_id = review.user_id
SET review.score = rating.score
WHERE review.score IS NULL;

DROP TABLE IF EXISTS book_rating;

ALTER TABLE book_review
    ADD KEY idx_review_book_score (book_id, score);

UPDATE book b
LEFT JOIN (
    SELECT book_id, ROUND(AVG(score), 2) AS average_score, COUNT(score) AS score_count
    FROM book_review
    WHERE score IS NOT NULL
    GROUP BY book_id
) review_stats ON review_stats.book_id = b.id
SET b.average_score = COALESCE(review_stats.average_score, 0),
    b.score_count = COALESCE(review_stats.score_count, 0),
    b.update_time = CURRENT_TIMESTAMP(3);
