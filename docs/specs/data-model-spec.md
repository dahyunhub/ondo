---
title: 온도 — 데이터 모델 명세 (Data Model Spec)
status: draft
created: 2026-06-15
sources:
  - _bmad-output/planning-artifacts/architecture.md (Data Architecture, Naming Patterns)
  - _bmad-output/planning-artifacts/epics.md (Story별 Flyway 마이그레이션 노트)
  - _bmad-output/planning-artifacts/prds/prd-ondo-2026-06-15/prd.md (FR-1~11)
  - _bmad-output/planning-artifacts/prds/prd-ondo-2026-06-15/addendum.md (§B ERD)
---

# 데이터 모델 명세 — 온도

이 문서는 코드 구현(Entity·Flyway 마이그레이션)에 바로 쓸 수 있는 테이블 단위 명세다. **Flyway가 스키마 정본**이고 Hibernate는 `ddl-auto=validate`로 검증만 한다(architecture). 아래 V1 DDL과 JPA 엔티티는 컬럼/타입/제약이 1:1로 일치해야 한다.

## 0. 공통 규약

- DBMS: **MySQL 8.x**, 엔진 InnoDB, 문자셋 `utf8mb4` / collation `utf8mb4_0900_ai_ci`.
- 네이밍: 테이블·컬럼 **snake_case 단수**. PK는 `id`(BIGINT, AUTO_INCREMENT). FK는 `<참조테이블>_id`.
- 인덱스 `idx_<table>_<col>`, 유니크 `uq_<table>_<cols>`, FK 제약 `fk_<table>_<ref>`.
- 시각 컬럼: `DATETIME(6)`, **저장값은 UTC**(앱 레이어에서 `Instant`/`LocalDateTime` UTC로 관리). 날짜 전용은 `DATE`.
- 공통 시간 필드 `created_at`/`updated_at`은 `BaseTimeEntity`(JPA Auditing)로 채운다. DB 기본값은 두지 않고 애플리케이션이 채운다(테스트 일관성).
- **soft delete는 자산(`child`, `memo`)에만** `deleted_at DATETIME(6) NULL` + Hibernate `@SQLRestriction("deleted_at is null")`. `daily_journal`은 덮어쓰기(UPDATE) 정책이라 soft delete 없음. `child_report`는 append 누적(삭제 없음).
- enum 성격 컬럼은 `VARCHAR`로 저장하고 JPA `@Enumerated(EnumType.STRING)` 매핑. 허용값은 각 표의 "허용값"에 고정.

## 1. ERD 개요

```
teacher 1──< classroom 1──< child 1──< memo
                              child 1──< child_report
classroom 1──< daily_journal
daily_journal >──< memo   (via journal_memo_link)
```

| 관계 | 카디널리티 | 비고 |
|------|-----------|------|
| teacher → classroom | 1:N | 한 교사가 여러 반(학년도로 구분) |
| classroom → child | 1:N | 반에 여러 아이 |
| child → memo | 1:N | 메모는 반드시 한 아이에 연결(서비스의 심장) |
| child → child_report | 1:N | 개인평가 시간순 누적 |
| classroom → daily_journal | 1:N | 반·날짜당 1건 |
| daily_journal ↔ memo | M:N | `journal_memo_link` 다리, 어떤 메모가 반영됐는지 추적 |

## 2. 누리과정 영역 enum (`curriculum_area`)

PRD §3 용어집의 5영역. `memo.curriculum_area`에 저장. **분류 전(자동분류 미수행)에는 `NULL` = "미분류"**.

| enum 값 | 한글 영역명 |
|---------|-----------|
| `PHYSICAL_HEALTH` | 신체운동·건강 |
| `COMMUNICATION` | 의사소통 |
| `SOCIAL` | 사회관계 |
| `ART` | 예술경험 |
| `NATURE` | 자연탐구 |

> API/JSON에서는 enum 값(`SOCIAL` 등)을 그대로 쓰고, 한글 표기는 프론트(Claude Design 구현물)가 매핑한다.

## 3. 테이블 명세

### 3.1 `teacher` (FR-10, NFR-6)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `email` | VARCHAR(255) | NOT NULL, UNIQUE(`uq_teacher_email`) | 로그인 ID |
| `password_hash` | VARCHAR(100) | NOT NULL | **BCrypt 해시**(약 60자). 평문 저장 금지 |
| `name` | VARCHAR(100) | NULL | 교사 표시 이름(선택) |
| `created_at` | DATETIME(6) | NOT NULL | |
| `updated_at` | DATETIME(6) | NOT NULL | |

- soft delete 없음(사용자 계정은 자산 정책 대상 아님).
- MVP는 단일 담임 전제. 교사 계정 생성은 가입 API 대신 **시드/마이그레이션**으로 1건 주입(아래 §5 참고). 가입 API가 필요해지면 `POST /api/v1/auth/signup`을 후속으로 추가.

### 3.2 `classroom` (FR-10)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `teacher_id` | BIGINT | NOT NULL, FK→`teacher.id`(`fk_classroom_teacher`) | 소유 교사 |
| `name` | VARCHAR(100) | NOT NULL | 반 이름(예: 햇살반) |
| `year` | INT | NOT NULL | 학년도. 같은 이름이라도 year로 구분 |
| `age_class` | INT | NULL | 만 나이(0~5). NULL=미지정·혼합연령반. **아이 생년월일 입력의 기본 표시 연도**를 정하는 데만 쓰고 선택을 제약하지 않는다. 출생연도 = `year − (age_class + 1)`. **V7 추가** — spec-classroom-age-birthdate |
| `start_date` | DATE | NOT NULL | **학년도 시작일**. FR-8 평가 기간 기본 시작점 |
| `created_at` | DATETIME(6) | NOT NULL | |
| `updated_at` | DATETIME(6) | NOT NULL | |

- UNIQUE `uq_classroom_teacher_name_year`(`teacher_id`, `name`, `year`) — 동일 교사·이름·학년도 중복 방지.
- INDEX `idx_classroom_teacher`(`teacher_id`) — 교사별 반 조회.

### 3.3 `child` (FR-11, NFR-2, NFR-1)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `classroom_id` | BIGINT | NOT NULL, FK→`classroom.id`(`fk_child_classroom`) | 소속 반 |
| `name` | VARCHAR(100) | NOT NULL | **실명**(DB에만 저장, 외부 AI 전송 금지) |
| `birth_date` | DATE | NOT NULL | 생년월일 |
| `gender` | VARCHAR(10) | NOT NULL | 성별 enum `MALE`/`FEMALE`(한글 남/여는 프론트 매핑). **V2 추가** — 디자인 명단/등록 화면 정합 |
| `token_alias` | VARCHAR(50) | NOT NULL | 저장형 가명(예: `아이A`). 등록 시 부여(FR-11). 비식별화 보조 식별자 — 상세는 §6 노트 |
| `warmth_snoozed_until` | DATE | NULL | 관찰 온도 '잠시 접어두기' 만료일(KST). NULL=접히지 않음. 오늘 ≥ 이 날짜면 **이미 만료**(무기한 없음). 온도 판정에만 작용하고 명단·메모·평가에는 영향 없음. **V5 추가** — spec-child-warmth-snooze |
| `deleted_at` | DATETIME(6) | NULL | soft delete 마커. NULL=활성 |
| `created_at` | DATETIME(6) | NOT NULL | |
| `updated_at` | DATETIME(6) | NOT NULL | |

- UNIQUE `uq_child_classroom_alias`(`classroom_id`, `token_alias`) — 반 내 가명 충돌 방지.
- INDEX `idx_child_classroom`(`classroom_id`).
- 조회 시 `@SQLRestriction("deleted_at is null")`로 삭제 아동 제외. 명단은 `name` 가나다순 정렬(FR-11, ORDER BY name).

### 3.3b `password_reset_token` (spec-password-reset, **V6 추가**)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `teacher_id` | BIGINT | NOT NULL, FK→`teacher.id` | 대상 계정 |
| `token_hash` | CHAR(64) | NOT NULL, UNIQUE | **원문 토큰의 SHA-256 hex.** 원문은 저장하지 않는다 |
| `expires_at` | DATETIME(6) | NOT NULL | 발급 + 30분 |
| `used_at` | DATETIME(6) | NULL | 사용·무효화 시각. NULL=아직 유효 |
| `created_at` / `updated_at` | DATETIME(6) | NOT NULL | |

- **원문 미저장이 이 테이블의 존재 이유다.** 난수는 메일에만 실리고 DB 에는 해시만 남아, DB 가 유출돼도 남의 비밀번호를 바꿀 수 없다.
- 유효 조건: `used_at IS NULL AND now < expires_at`. 재발급·비밀번호 변경 시 남은 토큰을 `used_at` 으로 일괄 무효화한다.
- soft delete 대상 아님(자산이 아니라 단기 크리덴셜).

### 3.4 `memo` (FR-1, FR-2, FR-7, NFR-5)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `child_id` | BIGINT | NOT NULL, FK→`child.id`(`fk_memo_child`) | 연결 아이(필수) |
| `teacher_id` | BIGINT | NOT NULL, FK→`teacher.id`(`fk_memo_teacher`) | 작성 교사(소유권 인가용) |
| `content` | VARCHAR(2000) | NULL | 자유 입력 |
| `play_activity` | VARCHAR(500) | NULL | 선택 항목 ①놀이 |
| `interaction` | VARCHAR(500) | NULL | 선택 항목 ②의사소통·상호작용 |
| `attitude` | VARCHAR(500) | NULL | 선택 항목 ③수업태도 |
| `curriculum_area` | VARCHAR(20) | NULL | 누리과정 영역(§2). NULL=미분류 |
| `created_at` | DATETIME(6) | NOT NULL | **재분석 판정 기준**(FR-6: memo.created_at > journal.analyzed_at) |
| `updated_at` | DATETIME(6) | NOT NULL | |
| `deleted_at` | DATETIME(6) | NULL | soft delete |

- INDEX `idx_memo_child`(`child_id`), `idx_memo_created_at`(`created_at`).
- **불변식(앱 레이어 강제):** `content`/`play_activity`/`interaction`/`attitude` 중 최소 1개는 non-blank(FR-1). DB CHECK 대신 Bean Validation + 서비스 재검증으로 강제(에러코드 `MEMO_EMPTY`).
- `curriculum_area`는 교사가 저장 시 입력하지 않음. Epic 3 일지 분석 패스(FR-2)에서 채워지거나 타임라인에서 수동 수정(FR-7, Story 2.3).

### 3.5 `daily_journal` (FR-3, FR-5, FR-6)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `teacher_id` | BIGINT | NOT NULL, FK→`teacher.id`(`fk_daily_journal_teacher`) | |
| `classroom_id` | BIGINT | NOT NULL, FK→`classroom.id`(`fk_daily_journal_classroom`) | |
| `journal_date` | DATE | NOT NULL | 일지 대상 날짜 |
| `content` | MEDIUMTEXT | NULL | 생성된 일지 서술(5영역 구조). 초안 생성 전엔 NULL 가능 |
| `status` | VARCHAR(20) | NOT NULL, DEFAULT `'DRAFT'` | `DRAFT`/`CONFIRMED`(FR-5) |
| `analyzed_at` | DATETIME(6) | NULL | 마지막 AI 분석 시각. **FR-6 재분석 판정 기준** |
| `version` | INT | NOT NULL, DEFAULT 1 | MVP는 항상 1(버전 관리는 v2) |
| `created_at` | DATETIME(6) | NOT NULL | |
| `updated_at` | DATETIME(6) | NOT NULL | |

- UNIQUE `uq_daily_journal_teacher_classroom_date`(`teacher_id`, `classroom_id`, `journal_date`) — 하루 1건(FR-3).
- **soft delete 없음.** 재분석은 같은 행을 UPDATE 덮어쓰기(유니크 충돌 원천 제거 — 리뷰로그 #9).
- 재분석 시 `analyzed_at` 갱신 → memo.created_at > analyzed_at인 메모가 없어지면 재분석 안내 자동 해소(FR-6).
- `status` enum 허용값: `DRAFT`(생성 직후 초안), `CONFIRMED`(교사 확정).

### 3.6 `journal_memo_link` (FR-3, FR-6)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `daily_journal_id` | BIGINT | NOT NULL, FK→`daily_journal.id`(`fk_jml_journal`) | |
| `memo_id` | BIGINT | NOT NULL, FK→`memo.id`(`fk_jml_memo`) | |
| `created_at` | DATETIME(6) | NOT NULL | |

- UNIQUE `uq_journal_memo_link`(`daily_journal_id`, `memo_id`) — 동일 일지에 같은 메모 중복 링크 방지.
- INDEX `idx_jml_journal`(`daily_journal_id`).
- **재분석 시:** 해당 일지의 기존 링크 전부 DELETE 후 재삽입(architecture). 어떤 메모가 반영됐는지 추적 가능(FR-3).

### 3.7 `child_report` (FR-8, FR-9, NFR-2)

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `id` | BIGINT | PK, AUTO_INCREMENT | |
| `child_id` | BIGINT | NOT NULL, FK→`child.id`(`fk_child_report_child`) | |
| `report_type` | VARCHAR(20) | NOT NULL | `MANUAL`/`MONTHLY` |
| `period_start` | DATE | NOT NULL | 분석 대상 기간 시작 |
| `period_end` | DATE | NOT NULL | 분석 대상 기간 끝 |
| `report_month` | VARCHAR(7) | NULL | `YYYY-MM`. **월말 자동 멱등 마커**. 수동 평가는 NULL |
| `content` | MEDIUMTEXT | NOT NULL | 생성된 개인평가 서술 |
| `created_at` | DATETIME(6) | NOT NULL | 시간순 누적 정렬 기준 |
| `updated_at` | DATETIME(6) | NOT NULL | |

- UNIQUE `uq_child_report_child_month`(`child_id`, `report_month`) — **월말 자동(FR-9) 멱등성** 보장. MySQL UNIQUE는 NULL 다중 허용이므로 **수동 평가(report_month NULL)는 무제한 누적**(FR-8: 6월·7월 별도 레코드). 자동 평가는 (child, month)당 1건으로 skip 멱등.
- INDEX `idx_child_report_child`(`child_id`), `idx_child_report_created_at`(`created_at`).
- `report_type` enum 허용값: `MANUAL`(수동, FR-8), `MONTHLY`(스케줄러, FR-9).
- soft delete 없음(append-only 자산 누적).

## 4. Flyway V1 마이그레이션 (`db/migration/V1__init.sql`)

> 위치: `backend/src/main/resources/db/migration/V1__init.sql`. 아래를 그대로 사용 가능. FK 순서를 위해 부모 테이블부터 생성한다.

```sql
-- V1__init.sql : 온도 초기 스키마 (7 tables)
-- DBMS: MySQL 8.x / InnoDB / utf8mb4

CREATE TABLE teacher (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name          VARCHAR(100) NULL,
    created_at    DATETIME(6)  NOT NULL,
    updated_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_teacher_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE classroom (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    teacher_id BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    year       INT          NOT NULL,
    start_date DATE         NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    updated_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_classroom_teacher_name_year UNIQUE (teacher_id, name, year),
    CONSTRAINT fk_classroom_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    INDEX idx_classroom_teacher (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE child (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    classroom_id BIGINT       NOT NULL,
    name         VARCHAR(100) NOT NULL,
    birth_date   DATE         NOT NULL,
    token_alias  VARCHAR(50)  NOT NULL,
    warmth_snoozed_until DATE NULL,
    deleted_at   DATETIME(6)  NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_child_classroom_alias UNIQUE (classroom_id, token_alias),
    CONSTRAINT fk_child_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id),
    INDEX idx_child_classroom (classroom_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE memo (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    child_id        BIGINT        NOT NULL,
    teacher_id      BIGINT        NOT NULL,
    content         VARCHAR(2000) NULL,
    play_activity   VARCHAR(500)  NULL,
    interaction     VARCHAR(500)  NULL,
    attitude        VARCHAR(500)  NULL,
    curriculum_area VARCHAR(20)   NULL,
    created_at      DATETIME(6)   NOT NULL,
    updated_at      DATETIME(6)   NOT NULL,
    deleted_at      DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_memo_child   FOREIGN KEY (child_id)   REFERENCES child (id),
    CONSTRAINT fk_memo_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id),
    INDEX idx_memo_child (child_id),
    INDEX idx_memo_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE daily_journal (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    teacher_id   BIGINT      NOT NULL,
    classroom_id BIGINT      NOT NULL,
    journal_date DATE        NOT NULL,
    content      MEDIUMTEXT  NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    analyzed_at  DATETIME(6) NULL,
    version      INT         NOT NULL DEFAULT 1,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_daily_journal_teacher_classroom_date UNIQUE (teacher_id, classroom_id, journal_date),
    CONSTRAINT fk_daily_journal_teacher   FOREIGN KEY (teacher_id)   REFERENCES teacher (id),
    CONSTRAINT fk_daily_journal_classroom FOREIGN KEY (classroom_id) REFERENCES classroom (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE journal_memo_link (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    daily_journal_id BIGINT      NOT NULL,
    memo_id          BIGINT      NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_journal_memo_link UNIQUE (daily_journal_id, memo_id),
    CONSTRAINT fk_jml_journal FOREIGN KEY (daily_journal_id) REFERENCES daily_journal (id),
    CONSTRAINT fk_jml_memo    FOREIGN KEY (memo_id)          REFERENCES memo (id),
    INDEX idx_jml_journal (daily_journal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE child_report (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    child_id     BIGINT      NOT NULL,
    report_type  VARCHAR(20) NOT NULL,
    period_start DATE        NOT NULL,
    period_end   DATE        NOT NULL,
    report_month VARCHAR(7)  NULL,
    content      MEDIUMTEXT  NOT NULL,
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_child_report_child_month UNIQUE (child_id, report_month),
    CONSTRAINT fk_child_report_child FOREIGN KEY (child_id) REFERENCES child (id),
    INDEX idx_child_report_child (child_id),
    INDEX idx_child_report_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 4.1 V2 — `V2__add_child_gender.sql` (디자인 정합)

```sql
ALTER TABLE child
    ADD COLUMN gender VARCHAR(10) NOT NULL AFTER birth_date;
```

> 클로드 디자인 목업의 아이 명단·등록 화면이 성별(남/여)을 다뤄 추가. enum `MALE`/`FEMALE`, JPA `@Enumerated(STRING)`. dev 시드 교사는 §5의 `DevDataInitializer`(코드)로 대체됨.

## 5. 시드 데이터 (선택, `V2__seed_teacher.sql` 또는 dev 전용)

로컬·데모용 교사 1건. **운영 배포에는 포함하지 말 것**(시크릿 노출). 비밀번호 해시는 BCrypt로 사전 생성해 주입.

```sql
-- 예시 (dev 전용). password_hash 는 실제 BCrypt 해시로 교체.
INSERT INTO teacher (email, password_hash, name, created_at, updated_at)
VALUES ('teacher@ondo.dev', '$2b$10$REPLACE_WITH_REAL_BCRYPT_HASH', '민지', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
```

## 6. 구현 노트 / 미해결 명료화

- **`token_alias` vs 요청 스코프 `[[CHILD_n]]`:** FR-11/PRD는 등록 시 `token_alias`(예: `아이A`)를 부여·저장하라고 명시한다. 반면 architecture/리뷰로그 #6은 비식별화 복원 안정성을 위해 **요청 스코프 센티넬 토큰 `[[CHILD_n]]`**(조사 결합에 안전)을 쓴다고 결정했다. 정합 해석: `child.token_alias`는 DB에 저장되는 안정적 가명(감사·표시·보조 매핑용)으로 유지하고, **실제 AI 프롬프트 치환은 분석 요청마다 in-memory로 `[[CHILD_n]]` 매핑**을 생성해 수행한다(`token_alias`를 프롬프트에 직접 박지 않음). 구현 시 Deidentifier가 둘의 관계를 캡슐화한다. → 별도 AI 연동 명세에서 확정 예정.
- **`memo` 최소 1개 필드 채움 제약**은 DB CHECK가 아니라 앱 레이어(`MEMO_EMPTY`)로 강제 — MySQL 버전·마이그레이션 이식성 고려.
- **점진 마이그레이션 옵션:** 에픽은 V1 전체 스키마 일괄 생성과 "스토리별 점진 마이그레이션"을 모두 허용한다. 본 문서는 일괄 V1을 기본으로 한다. 점진 방식 채택 시 테이블을 사용 스토리 직전 `V{n}`으로 분할하되 FK 부모 선행 순서를 유지한다.
- **타임존:** 컬럼은 tz 없는 `DATETIME(6)`. 앱·JDBC URL(`serverTimezone=UTC`)·컨테이너 TZ를 모두 UTC로 맞춰 일관성 확보(architecture).
