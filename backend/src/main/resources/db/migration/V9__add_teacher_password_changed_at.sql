-- V9__add_teacher_password_changed_at.sql : 비밀번호 변경 시각(기발급 JWT 무효화용)
-- 정본: backend/src/main/java/com/ondo/auth/domain/Teacher.java
--
-- 무상태 JWT 라 비밀번호를 바꿔도 이미 발급된 토큰이 만료(1h)까지 그대로 살아 있었다.
-- 계정을 도난당해 비밀번호를 바꾼 경우 공격자의 토큰이 최대 한 시간 유효했다는 뜻이다.
-- 이 시각보다 먼저 발급된 토큰을 JwtAuthFilter 가 거절한다.
-- 기존 행은 NULL = 무효화 기준 없음(지금 들고 있는 토큰은 그대로 유효).

ALTER TABLE teacher ADD COLUMN password_changed_at DATETIME(6) NULL AFTER password_hash;
