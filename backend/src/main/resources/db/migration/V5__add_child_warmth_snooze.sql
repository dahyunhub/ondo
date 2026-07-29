-- 관찰 온도 '잠시 접어두기'(spec-child-warmth-snooze).
-- 장기 결석·입원한 아이가 볼 기회도 없었는데 계속 LOW 로 뜨는 문제를 막는다.
-- 출결 기능이 아니다 — 사유도 이력도 남기지 않고, 온도 판정에만 작용한다.
-- KST 달력일 기준. NULL = 접히지 않음. 값이 있어도 오늘 >= 이 날짜면 이미 만료(무기한 스누즈 없음).
ALTER TABLE child ADD COLUMN warmth_snoozed_until DATE NULL AFTER token_alias;
