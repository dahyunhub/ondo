-- 비밀번호 재설정 토큰(spec-password-reset).
-- 원문 토큰은 저장하지 않는다 — 메일에만 싣고 여기엔 SHA-256 해시(64자 hex)만 남긴다.
-- DB 가 유출돼도 토큰을 되돌릴 수 없어야 하기 때문이다.
CREATE TABLE password_reset_token (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    teacher_id  BIGINT       NOT NULL,
    token_hash  CHAR(64)     NOT NULL,
    expires_at  DATETIME(6)  NOT NULL,
    used_at     DATETIME(6)  NULL,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_token_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    INDEX idx_password_reset_token_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
