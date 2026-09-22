-- V12__split_profile_photo_data.sql : profile_photo 에서 LONGBLOB 을 별도 테이블로 분리
--
-- V3 는 "메인 테이블(child/teacher)에 BLOB 을 섞지 않도록" 별도 테이블을 만들었는데,
-- 한 걸음이 모자랐다. 메타데이터(content_type·updated_at)와 이미지 바이트가 같은 행에
-- 있으면 그 행을 읽는 모든 경로가 이미지를 끌고 온다. 실제로 그렇게 터졌다 —
-- 아이 명단 API 한 번이 응답 0.6KB 를 만들자고 MySQL 에서 5.4MB 를 읽었다(2026-09-09).
--
-- 그때는 호출부를 projection 으로 바꿔 막았지만, 그건 규율이지 보장이 아니다. 누가 엔티티를
-- 반환하는 조회를 하나 더 추가하면 같은 일이 그대로 재발한다. 바이트를 다른 테이블로 빼면
-- 애초에 불가능한 실수가 된다 — 갱신시각을 읽는 쿼리에 이미지가 딸려올 자리가 없다.
--
-- FK 는 ON DELETE CASCADE: 사진 삭제 경로가 둘로 나뉘어도 바이트만 남는 상태가 안 나온다.

CREATE TABLE profile_photo_data (
    owner_kind VARCHAR(10) NOT NULL,
    owner_id   BIGINT      NOT NULL,
    data       LONGBLOB    NOT NULL,
    PRIMARY KEY (owner_kind, owner_id),
    CONSTRAINT fk_photo_data_photo FOREIGN KEY (owner_kind, owner_id)
        REFERENCES profile_photo (owner_kind, owner_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO profile_photo_data (owner_kind, owner_id, data)
SELECT owner_kind, owner_id, data FROM profile_photo;

ALTER TABLE profile_photo DROP COLUMN data;
