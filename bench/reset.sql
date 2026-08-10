-- ============================================================================
--  bench 시드 되돌리기 (bench/reset.sql)
--
--  bench/seed.sql 이 넣은 행만 정확히 지운다 — 기준은 seed 가 남긴 bench_watermark.
--  워터마크가 없으면(= 시드한 적 없음) 아무것도 지우지 않는다.
--
--  실행: bench/seed.sh --reset
-- ============================================================================

SET @wm_memo    := (SELECT memo_max_id    FROM bench_watermark WHERE id = 1);
SET @wm_journal := (SELECT journal_max_id FROM bench_watermark WHERE id = 1);
SET @wm_report  := (SELECT report_max_id  FROM bench_watermark WHERE id = 1);

-- 워터마크가 없으면 삭제 조건을 거짓으로 만들어 통째로 no-op 이 되게 한다.
SET @wm_memo    := COALESCE(@wm_memo,    -1);
SET @wm_journal := COALESCE(@wm_journal, -1);
SET @wm_report  := COALESCE(@wm_report,  -1);

-- 메모를 지우기 전에 일지-메모 링크부터(FK). bench 메모를 참조하는 링크만.
DELETE FROM journal_memo_link WHERE @wm_memo >= 0 AND memo_id > @wm_memo;

DELETE FROM child_report  WHERE @wm_report  >= 0 AND id > @wm_report;
DELETE FROM daily_journal WHERE @wm_journal >= 0 AND id > @wm_journal;
DELETE FROM memo          WHERE @wm_memo    >= 0 AND id > @wm_memo;

-- 사진: seed 가 직접 넣은 소유자만(원래 사진이 있던 소유자는 bench_photo_owner 에 없다).
DELETE p FROM profile_photo p
    JOIN bench_photo_owner o
         ON o.owner_kind = p.owner_kind AND o.owner_id = p.owner_id;

DROP TABLE IF EXISTS bench_photo_owner;
DROP TABLE IF EXISTS bench_watermark;

SELECT
    (SELECT COUNT(*) FROM memo)          AS memo,
    (SELECT COUNT(*) FROM profile_photo) AS photo,
    (SELECT COUNT(*) FROM daily_journal) AS journal,
    (SELECT COUNT(*) FROM child_report)  AS report;
