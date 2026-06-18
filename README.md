# 자람 (jaram)

유치원 교사 업무 도구 — 짧은 **메모**를 남기면 아이별로 기록이 쌓이고, **AI**가 누리과정 기반 보육일지·개인 관찰평가를 만들어 주는 서비스.

설계 산출물은 [`_bmad-output/planning-artifacts/`](_bmad-output/planning-artifacts/)
(PRD · 아키텍처 · 에픽/스토리)와 [`docs/specs/`](docs/specs/)(API·데이터모델·에러코드·AI 연동 명세)를 정본으로 따른다.

## 저장소 구조

```
jaram/
├── backend/                 # Spring Boot 4 + Java 25 (Gradle) REST API
│   └── src/main/java/com/jaram/
│       ├── auth/            # 로그인 · JWT
│       ├── classroom/       # 담당 반
│       ├── child/           # 아이 등록·관리(보존형 삭제)
│       ├── memo/            # 메모 기록 · 타임라인 · 누리과정 영역
│       ├── ai/              # AiClient 추상화 + OpenAiClient (LLM 연동)
│       ├── common/          # 공통 에러·예외·BaseEntity
│       └── config/          # Security · JPA · AI 설정
├── frontend/                # Vue 3 + Vite SPA (반응형: 데스크톱/모바일)
│   ├── src/                 # views · components · stores · lib · router
│   └── mockups/             # Claude Design 디자인 정본(목업)
├── docs/specs/              # 구현 명세 3종 + AI 연동 · 정합 가이드
└── _bmad-output/            # PRD · 아키텍처 · 에픽/스토리 (BMad 산출물)
```

## 기술 스택

| 영역    | 선택                                              |
|---------|---------------------------------------------------|
| 백엔드  | Java 25 LTS · Spring Boot 4.0.7 · Gradle 9.5      |
| DB      | MySQL 8.x · Spring Data JPA · Flyway (스키마 정본) |
| 인증    | Spring Security (stateless) · JWT (jjwt) · BCrypt  |
| AI      | **OpenAI** Chat Completions · RestClient(raw HTTP) · structured outputs |
| 프론트  | Vue 3 (Composition API, Pinia 미사용) · Vite 8 · 반응형(사이드바↔하단탭) |
| 테스트  | JUnit 5 · Testcontainers(MySQL) · MockWebServer    |
| 인프라  | Docker · docker-compose (app + mysql)              |

## 개발 실행

> 포트: 백엔드 **8090**, 프론트 dev **5273** (다른 프로젝트와 충돌을 피하려 8080/5173에서 변경).
> 필요 시 `SERVER_PORT`/`APP_PORT`(백엔드), `vite.config.js`(프론트)로 조정.

### 백엔드

```bash
cd backend
cp .env.example .env          # 환경변수 채우기 (.env 는 커밋 금지)
./gradlew build               # 빌드 + 테스트 (Docker 필요 — Testcontainers)
./gradlew bootRun             # 실행 (MySQL 필요) → http://localhost:8090
```

- 기동에는 MySQL 이 필요하다(`db/migration` 의 Flyway 마이그레이션 적용, `ddl-auto=validate`).
- 헬스체크: `GET /actuator/health`
- AI 호출은 `AI_API_KEY`(OpenAI) 가 있어야 동작한다. 미설정 시 일반 화면은 정상, AI 분석만 비활성.

### Docker 로 한 번에 (app + mysql)

```bash
docker compose up -d --build           # app + mysql 기동, Flyway 자동 적용
curl localhost:8090/actuator/health    # {"status":"UP"}
```

- 호스트 포트 충돌 시 `APP_PORT=8091 docker compose up -d` 로 변경 가능.
- dev 프로파일은 시드 교사(`teacher@jaram.dev` / `password1234`)와 햇살반·아이 6명을 생성해 바로 둘러볼 수 있다.

### 프론트엔드

```bash
cd frontend
npm install
npm run dev                   # http://localhost:5273 (→ /api 는 :8090 으로 프록시)
npm run build                 # 프로덕션 빌드
```

- **반응형**: 창 폭 **900px** 이상은 데스크톱(좌측 사이드바) 레이아웃, 미만은 모바일(하단 탭) 레이아웃으로 자동 전환된다.
- 디자인은 `frontend/mockups/`(Claude Design)를 정본으로 따른다.

## 구현된 화면 (프론트)

로그인 · 반 선택 · 홈 · 아이 목록 · 아이 등록/수정(보존형 삭제) · 빠른 메모 입력 · 아이별 타임라인(누리과정 영역 필터·인라인 영역 수정) · 마이(프로필·반 전환·로그아웃).
모든 화면이 데스크톱/모바일 두 레이아웃을 가진다. AI 일지·개인평가 화면은 백엔드(Epic 3·4) 구현 후 연결 예정으로 "준비 중" 상태.

## 실행 화면

**데스크톱 (사이드바 레이아웃)**

| 로그인 | 홈 | 아이 목록 | 아이 타임라인 |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/login-desktop.png" width="230"> | <img src="docs/screenshots/home-desktop.png" width="230"> | <img src="docs/screenshots/children-desktop.png" width="230"> | <img src="docs/screenshots/timeline-desktop.png" width="230"> |

**모바일 (하단 탭 레이아웃)**

| 홈 | 아이 목록 | 빠른 메모 | 아이 타임라인 |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home-mobile.png" width="150"> | <img src="docs/screenshots/children-mobile.png" width="150"> | <img src="docs/screenshots/memo-mobile.png" width="150"> | <img src="docs/screenshots/timeline-mobile.png" width="150"> |

> 캡처는 dev 시드 데이터(햇살반) 기준. 재생성: `cd frontend && node scripts/shoot.mjs` (dev 서버 + 백엔드 기동 상태에서 실행, 설치된 Chrome 사용).

## 데이터 모델 (ERD)

Flyway 마이그레이션(`backend/src/main/resources/db/migration`)이 스키마 정본. 7개 테이블 + 메모↔일지 N:M 링크. 자세한 정의는 [`docs/specs/data-model-spec.md`](docs/specs/data-model-spec.md).

```mermaid
erDiagram
    teacher ||--o{ classroom : "담당"
    teacher ||--o{ memo : "작성"
    teacher ||--o{ daily_journal : "생성"
    classroom ||--o{ child : "소속"
    classroom ||--o{ daily_journal : "대상"
    child ||--o{ memo : "관찰기록"
    child ||--o{ child_report : "개인평가"
    daily_journal ||--o{ journal_memo_link : "근거"
    memo ||--o{ journal_memo_link : "인용"

    teacher {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar name
    }
    classroom {
        bigint id PK
        bigint teacher_id FK
        varchar name
        int year
        date start_date
    }
    child {
        bigint id PK
        bigint classroom_id FK
        varchar name
        date birth_date
        varchar gender "MALE / FEMALE"
        varchar token_alias "비식별 별칭"
        datetime deleted_at "보존형 삭제"
    }
    memo {
        bigint id PK
        bigint child_id FK
        bigint teacher_id FK
        varchar content
        varchar play_activity "놀이"
        varchar interaction "의사소통·상호작용"
        varchar attitude "수업태도"
        varchar curriculum_area "누리과정 영역(nullable)"
        datetime deleted_at
    }
    daily_journal {
        bigint id PK
        bigint teacher_id FK
        bigint classroom_id FK
        date journal_date
        mediumtext content
        varchar status "DRAFT/…"
        int version
    }
    journal_memo_link {
        bigint id PK
        bigint daily_journal_id FK
        bigint memo_id FK
    }
    child_report {
        bigint id PK
        bigint child_id FK
        varchar report_type
        date period_start
        date period_end
        varchar report_month
        mediumtext content
    }
```

| 테이블 | 설명 | 핵심 제약 |
|--------|------|-----------|
| `teacher` | 교사 계정 | `email` 유니크 |
| `classroom` | 담당 반(학년도 단위) | `(teacher_id, name, year)` 유니크 |
| `child` | 원아 | `(classroom_id, token_alias)` 유니크 · 보존형 삭제(`deleted_at`) |
| `memo` | 관찰 메모(놀이·상호작용·태도) | 누리과정 영역은 밤에 자동 분류(nullable) · 소프트 삭제 |
| `daily_journal` | 하루 보육일지(AI 생성) | `(teacher_id, classroom_id, journal_date)` 유니크 · 재분석 `version` |
| `journal_memo_link` | 일지 ↔ 근거 메모 (N:M) | `(daily_journal_id, memo_id)` 유니크 |
| `child_report` | 개인 관찰평가(기간) | `(child_id, report_month)` 유니크 |

> `daily_journal`·`journal_memo_link`·`child_report`는 스키마만 준비(Epic 3·4에서 기능 연결).

## 현재 상태

| 단계 | 내용 | 상태 |
|------|------|------|
| Epic 1 | 골격 · 인증(JWT) · 반 선택 · 아이 등록·관리 · 워킹 스켈레톤 Docker | ✅ 완료 |
| 프론트 | Vue+Vite 전 화면 + **반응형(데스크톱/모바일)** 전환 | ✅ 완료 |
| Epic 2 | 메모 기록 · 타임라인 · 누리과정 영역 분류 | ✅ 완료 |
| Story 3.1 | `AiClient` 추상화 + `OpenAiClient`(structured outputs·타임아웃·재시도) | ✅ 완료 |
| Epic 3 | AI 보육일지 (비식별화 → 분석 → 검증) | 🔜 진행 예정 |
| Epic 4 | 개인 관찰평가 | ⬜ 예정 |
| Epic 5 | 운영 배포 | ⬜ 예정 |

전체 41개 테스트 통과. 상세 진행은 [`docs/specs/backend-implementation-plan.md`](docs/specs/backend-implementation-plan.md).
