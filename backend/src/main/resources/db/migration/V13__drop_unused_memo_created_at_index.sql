-- V13__drop_unused_memo_created_at_index.sql : 소비자 없는 idx_memo_created_at 제거
--
-- V11 에서 (child_id, created_at) 복합 인덱스를 만들 때, created_at 단일 인덱스는 "child_id 없이
-- 기간만 보는 조회의 여지" 로 남겨 뒀다. 그 여지는 실현되지 않았고, 지금 이 인덱스를 타는 쿼리는
-- 하나도 없다:
--   - 타임라인 · 일지 묶음 · 개인평가 묶음 · 관찰 온도 → 전부 child_id 로 시작하므로 복합을 탄다.
--   - analytics/retention.sql → teacher_id 기준이라 fk_memo_teacher 를 타거나 전체 스캔이다.
--     (created_at 은 SELECT 목록에만 있고 WHERE 범위 조건이 아니다.)
--
-- 확인 방법: MySQL 8 의 invisible index 로 "지운 셈 치고" 실행계획을 다시 떴다.
-- ALTER TABLE memo ALTER INDEX idx_memo_created_at INVISIBLE 상태에서 위 쿼리들의 plan 이
-- key · rows · Extra 까지 전부 동일했다(1학기치 데이터, 메모 3,162건). 즉 옵티마이저는 이미
-- 이 인덱스를 안 쓰고 있었고, 메모가 INSERT 될 때마다 유지 비용만 내고 있었다.
--
-- 되살릴 일이 생기면: CREATE INDEX idx_memo_created_at ON memo (created_at);

DROP INDEX idx_memo_created_at ON memo;
