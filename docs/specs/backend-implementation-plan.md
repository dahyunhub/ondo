---
title: 자람 — 백엔드 구현 계획 (Backend Implementation Plan)
status: active
created: 2026-06-15
sources:
  - _bmad-output/planning-artifacts/architecture.md
  - _bmad-output/planning-artifacts/epics.md
  - docs/specs/api-spec.md
  - docs/specs/data-model-spec.md
  - docs/specs/error-code-catalog.md
scope: 백엔드(Spring Boot)만. 프론트(Vue + Vite)는 Claude Design 으로 별도 구현 중 — 본 계획 범위 밖.
---

# 백엔드 구현 계획 — 자람

명세 3종(데이터 모델·API·에러코드)과 아키텍처/에픽을 코드 작업으로 매핑한 실행 계획.
**Flyway = 스키마 정본**, `ddl-auto=validate`, package-by-feature(`com.jaram.*`)를 정본으로 따른다.

## 결정 사항

- **테스트 DB 전략:** Testcontainers(MySQL 8) 통합테스트. H2 는 V1 DDL(InnoDB·utf8mb4_0900·MEDIUMTEXT·DATETIME(6))을 검증 못 하므로 제외. (Docker 필요 — 로컬 확인됨 29.x)
- **AI 벤더:** **OpenAI API**(사용자 확정 2026-06-16). `AiClient` 추상화 뒤 `OpenAiClient` 구현. 상세 = `docs/specs/ai-integration-spec.md`. 모델은 `AI_MODEL` env.
- 단위테스트(순수 로직: JWT·비식별화·검증)는 DB 없이, 통합테스트(repo·엔드포인트)는 Testcontainers.

## 현재 상태

- ✅ **Story 1.1** 프로젝트 골격: Spring Boot 4.0.7 + Java 25 + Gradle 9.5, package-by-feature, 빌드 통과.
- ✅ **Phase 0** 공통 기반: ErrorCode·BusinessException 계열·ApiError·GlobalExceptionHandler, BaseTimeEntity(UTC Auditing), JpaConfig, Flyway V1(7테이블), Testcontainers(MySQL 8.4) 통합테스트 베이스 — 완료(2026-06-15).
- ✅ **Story 1.2** 인증: Teacher 엔티티/repo, SecurityConfig(무상태), JwtProvider/JwtAuthFilter/RestAuthenticationEntryPoint, AuthController `POST /api/v1/auth/login`, dev 시드 교사. 테스트 8개 통과(JWT 단위 3 + 인증 통합 4 + contextLoads 1) — 완료(2026-06-15).
- ✅ **Story 1.3** 반 선택: Classroom 엔티티/repo, `GET /api/v1/classrooms`(아이 수 포함·학년도 내림차순), repo 레벨 teacher 소유권, @AuthenticationPrincipal 로 교사 식별. childCount 는 child 테이블 네이티브 카운트(soft delete 제외). 통합테스트 2개(소유권·정렬·childCount, 미인증 401) — 완료(2026-06-16). 총 10개 테스트 통과.
- ✅ **Story 1.4** 아이 등록·관리: Child 엔티티(`@SQLRestriction` soft delete, token_alias 자동부여 아이A/B…), CRUD 4종(`GET/POST /classrooms/{id}/children`, `PUT/DELETE /children/{id}`), 가나다순, 반/아이 소유권(타 교사 → 404). 통합테스트 9개 — 완료(2026-06-16). 총 19개(코드리뷰 후 21개) 테스트 통과.
- ✅ **Story 1.5** 워킹 스켈레톤 배포: `backend/Dockerfile`(멀티스테이지, Java 25), `docker-compose.yml`(app+mysql, 헬스체크, env 시크릿), `.dockerignore`, 루트 `.env.example`. `docker compose up -d` 로 검증 — Flyway V1 적용·`/actuator/health` UP·로그인 동작 확인. → **Epic 1 완료.** 다음 = Epic 2(메모) 또는 Epic 5(prod 강화 배포).
- ✅ **디자인 정합(성별)**: 클로드 디자인 목업 대조 → 유일 갭이던 **성별**을 백엔드에 반영. Flyway `V2__add_child_gender.sql`, `Gender`(MALE/FEMALE) enum, Child/DTO/검증·테스트. 정합 가이드 `docs/specs/design-api-alignment.md` 작성 — 완료(2026-06-16). 총 23개 테스트 통과.
- ✅ **프론트 Epic 1 연동**: Vue+Vite(로그인·반선택·아이 CRUD), api 클라이언트(JWT/401)·router 가드·tokens.css — 완료(2026-06-18, 커밋 92ec312). dev 시드 반·아이 추가.
- ✅ **Epic 2 메모·타임라인** (Story 2.1~2.3): `Memo` 엔티티(CurriculumArea enum, soft delete), `POST /memos`(MEMO_EMPTY 불변식·소유권), `DELETE /memos/{id}`, `GET /children/{id}/timeline`(area 필터·UNCLASSIFIED·최신순), `PATCH /memos/{id}/curriculum-area`. HttpMessageNotReadable→400 매핑 추가. 통합테스트 10개 — 완료(2026-06-18). 총 33개 테스트 통과. → **Epic 2 백엔드 완료. 다음 = 메모/타임라인 프론트 연동 또는 Epic 3(AI 일지).**
- 프론트 메모/타임라인 화면 연동은 다음 단계.

> 배포 노트: 호스트 포트는 `APP_PORT`(기본 8080)로 오버라이드 가능. 워킹 스켈레톤은 `SPRING_PROFILES_ACTIVE=dev`(시드 교사 → 로그인 데모). 운영 prod 프로파일·시크릿·deploy.sh 는 Epic 5(Story 5.1/5.2).

> 테스트 노트(soft delete): 단일 `@Transactional` 통합테스트에서 같은 아이를 두 번 삭제하면 1차 캐시 때문에 `@SQLRestriction`(SELECT 시점 필터)이 안 먹어 두 번째 findById 가 엔티티를 그대로 반환한다. 실제로는 요청마다 새 세션이라 문제없음 — 테스트에선 `em.flush(); em.clear()` 로 새 세션을 재현한다.

> 테스트 노트: 통합테스트 컨테이너는 **싱글턴 패턴**(static 블록 start)으로 띄운다. `@Testcontainers`/`@Container` 로 관리하면 첫 클래스 종료 시 컨테이너가 멈춰 캐시된 컨텍스트를 쓰는 이후 클래스가 "Could not open JPA EntityManager" 로 실패한다.

### Phase 0 + 1.2 구현 노트 (Spring Boot 4 특이사항)

- **Jackson 3**: Boot 4 는 `tools.jackson.databind.ObjectMapper`(Jackson 3)를 자동설정한다. `com.fasterxml.jackson`(Jackson 2)이 아님.
- **autoconfig 패키지 이동**: 시큐리티 `org.springframework.security.config.annotation.web.configuration.EnableWebSecurity`, MockMvc `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`.
- **Testcontainers 2.x**: BOM `org.testcontainers:testcontainers-bom:2.0.5`, 모듈 artifactId 가 `testcontainers-mysql`/`testcontainers-junit-jupiter`로 변경. Boot BOM 이 버전 관리 안 함.
- **starter 명**: web 은 `spring-boot-starter-webmvc`.
- dev 시드 교사는 V2 SQL 대신 `@Profile("dev")` CommandLineRunner(`DevDataInitializer`)로 — prod/test 미실행, BCrypt 해시 사전생성 불필요.

## Phase 0 — 공통 기반 (모든 기능 스토리 선행)

cross-cutting. 에픽상 Story 1.2 에 묶여 있으나 분리해 먼저 깐다.

- `common/exception`: `ErrorCode`(enum, 에러카탈로그 §4), `BusinessException`, `AiAnalysisException`, `DeidentificationException`
- `common/response`: `ApiError`(`{timestamp,status,code,message,path}`, 필드 오류 확장)
- `common/exception/GlobalExceptionHandler`(@RestControllerAdvice): 도메인/검증/폴백 매핑 단일 지점
- `common/entity`: `BaseTimeEntity`(JPA Auditing), `SoftDeletable`
- `config`: `JpaConfig`(@EnableJpaAuditing)
- **Flyway `V1__init.sql`**: 7개 테이블(data-model-spec §4 그대로)
- `application.yml`: DB 실연동(`ddl-auto=validate`, Flyway enabled) — 골격 단계 autoconfig 제외 해제
- 테스트 기반: `build.gradle` 에 Testcontainers(mysql, junit-jupiter) 추가, 공통 `IntegrationTest` 베이스(@SpringBootTest + MySQLContainer)

## Phase 1 — Epic 1: 인증·반·아이

- **1.2 인증(로그인·JWT)** ← *이번 묶음*
  - `Teacher` 엔티티/repo (email unique, password_hash, BaseTime)
  - `config/SecurityConfig`(SecurityFilterChain, BCryptPasswordEncoder, `/auth/login`·`/actuator/health` permitAll, 그 외 인증)
  - `auth/jwt`: `JwtProvider`(HS256, ~1h), `JwtAuthFilter`(Bearer 파싱→인증), 401 엔트리포인트→표준 에러
  - `auth`: `AuthController` `POST /auth/login`, `AuthService`, `LoginRequest`/`LoginResponse`
  - dev 시드 교사 `V2__seed_teacher_dev.sql`(BCrypt 해시, 운영 제외)
  - 테스트: JwtProvider 단위, AuthController 슬라이스(200/401/400), 보호경로 401 통합
- **1.3 반 선택**: `Classroom` 엔티티/repo/service/controller, `GET /classrooms`(childCount, year 구분), repo 레벨 소유권
- **1.4 아이 관리**: `Child`(soft delete `@SQLRestriction`, token_alias 자동부여), CRUD 4종, 가나다순

## Phase 2 — Epic 2: 메모·타임라인

- **2.1** `Memo` 엔티티, `POST /memos`(최소 1필드 `MEMO_EMPTY`), `DELETE`(soft)
- **2.2** `GET /children/{id}/timeline`(area 필터, `UNCLASSIFIED`=NULL, 빈 배열)
- **2.3** `PATCH /memos/{id}/curriculum-area`

## Phase 3 — Epic 3: AI 보육일지 (AI 인프라 선행 → 기능)

> ✅ 선행 블로커 해소: 일지/평가 `content` JSON 스키마 + AI 응답 파싱 타입 = `docs/specs/ai-integration-spec.md`(Messages API raw HTTP, structured outputs, 비식별화 순서, OutputValidator 규칙).

- **3.1** `AiClient` 인터페이스 + RestClient 구현(하드 타임아웃 ~20s, 연결·5xx 재시도), `AiClientConfig`, MockWebServer 테스트
- **3.2** `Deidentifier` + `RestorationContext`(요청 스코프 `[[CHILD_n]]`, 복원 실패 예외)
- **3.3** `PromptTemplateLoader` + `OutputValidator` + `resources/prompts/*`
- **3.4** `AnalysisGuard`(동시 1건) + TX 경계(`JournalService` no-TX → `JournalPersistService` REQUIRES_NEW)
- **3.5** `POST /journals/analyze`(daily_journal + journal_memo_link, FR-2 영역 분류)
- **3.6** `GET/PUT /journals`(수정·확정)
- **3.7** `POST /journals/{id}/analyze`(덮어쓰기, `reanalysisNeeded`)

## Phase 4 — Epic 4: 개인평가

- **4.1** `ChildReport`, `POST/GET reports`(기간 자동계산, MANUAL 누적, `REPORT_NO_MEMO`)
- **4.2** `MonthlyReportScheduler`(@Scheduled, 아이 단위 격리, UNIQUE 멱등 skip)

## Phase 5 — 배포 (Epic 1.5 + Epic 5)

- Dockerfile, docker-compose(app+mysql), `scripts/deploy.sh`, prod 프로파일·로깅(실명 금지)

## 진행 순서 요약

`Phase 0 → 1.2`(이번) → 1.3 → 1.4 → 2.x → [AI 연동 명세] → 3.x → 4.x → 5.x
