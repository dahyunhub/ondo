-- 반 연령(spec-classroom-age-birthdate).
-- 만 나이(0~5). NULL = 미지정 또는 혼합연령반 — 필수가 아니다.
-- 아이 등록 시 생년월일 입력의 '기본 표시 연도'를 계산하는 데만 쓴다:
--   출생연도 = classroom.year - (age_class + 1)
-- 선택을 제약하지는 않는다(혼합연령반·조기입학·유예 아동이 흔하다).
-- 타입은 INT — 기존 year 와 맞추고, 엔티티의 Integer 와 Hibernate validate 가 일치해야 한다.
ALTER TABLE classroom ADD COLUMN age_class INT NULL AFTER year;
