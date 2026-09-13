-- V11__add_memo_child_created_index.sql : memo (child_id, created_at) 복합 인덱스
-- 정본: docs/specs/data-model-spec.md §memo
--
-- 메모를 읽는 쿼리는 전부 child_id 로 거른 뒤 created_at 으로 정렬(타임라인)하거나
-- 범위를 잡는다(관찰 온도·일지 묶음·개인평가 묶음). 지금까지는 child_id 와 created_at 에
-- 각각 단일 인덱스만 있어서:
--   - 타임라인은 child_id 인덱스로 행을 모은 뒤 created_at 으로 다시 정렬했다(Using filesort).
--   - 범위 쿼리는 옵티마이저가 created_at 쪽을 고르면 앱 전체의 그 기간 메모를 훑고 반으로 걸렀다.
-- 복합 인덱스 하나가 두 패턴을 다 덮는다: child_id 로 좁힌 안에서 created_at 순으로 이미 정렬돼 있다.
--
-- idx_memo_child 는 복합 인덱스의 왼쪽 접두이므로 완전히 중복된다 — 쓰기마다 유지 비용만 드는
-- 인덱스라 지운다. fk_memo_child 가 child_id 인덱스를 요구하지만 복합 인덱스가 그 역할을 하므로,
-- 반드시 복합 인덱스를 먼저 만들고 나서 지운다(순서를 바꾸면 FK 제약 때문에 DROP 이 거부된다).
-- idx_memo_created_at 은 그대로 둔다(child_id 없이 기간만 보는 조회의 여지).

CREATE INDEX idx_memo_child_created ON memo (child_id, created_at);
DROP INDEX idx_memo_child ON memo;
