-- V8__create_feedback.sql : 인앱 피드백 수집(실사용자 의견 → 기록)
-- 정본: backend/src/main/java/com/ondo/feedback/domain/Feedback.java
-- 피드백은 append-only 기록이다. created_at/updated_at 은 다른 테이블과 동일하게 BaseTimeEntity 로 채운다.

CREATE TABLE feedback (
    id         BIGINT        NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT        NULL,          -- 로그인 사용자면 채움(추후 익명 여지로 NULL 허용)
    category   VARCHAR(30)   NULL,          -- 버그/제안/기타 등(자유값)
    message    VARCHAR(2000) NOT NULL,      -- 의견 본문
    page       VARCHAR(200)  NULL,          -- 보낸 화면(라우트명) — 맥락 파악용
    user_agent VARCHAR(500)  NULL,          -- 브라우저·기기 대략(재현용)
    created_at DATETIME(6)   NOT NULL,
    updated_at DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_feedback_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    INDEX idx_feedback_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
