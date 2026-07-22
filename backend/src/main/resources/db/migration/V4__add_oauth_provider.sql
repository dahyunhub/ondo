-- V4__add_oauth_provider.sql : 카카오 등 소셜 로그인 지원.
-- 기존 이메일/비번 계정은 provider NULL 을 유지한다. 소셜 전용 계정은 password_hash NULL,
-- 카카오가 이메일을 주지 않으면 email 도 NULL 로 저장(로그인은 소셜로만).
-- (provider, provider_id) 로 소셜 계정을 식별 — provider 확장(naver/google) 대비.
-- MySQL 은 UNIQUE 인덱스에서 NULL 을 중복으로 보지 않으므로, provider/provider_id 가 NULL 인
-- 이메일 계정이 여럿이어도 충돌하지 않는다(email UNIQUE 도 동일).

ALTER TABLE teacher
    MODIFY COLUMN email         VARCHAR(255) NULL,
    MODIFY COLUMN password_hash VARCHAR(100) NULL,
    ADD COLUMN    provider      VARCHAR(20)  NULL,
    ADD COLUMN    provider_id   VARCHAR(64)  NULL,
    ADD CONSTRAINT uq_teacher_provider UNIQUE (provider, provider_id);
