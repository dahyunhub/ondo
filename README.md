# 자람 (jaram)

유치원 교사 업무 도구 — 메모 기록, AI 보육일지/개인평가 생성 서비스.

설계 산출물은 [`_bmad-output/planning-artifacts/`](_bmad-output/planning-artifacts/)
(PRD · 아키텍처 · 에픽/스토리)와 [`docs/`](docs/)를 정본으로 따른다.

## 저장소 구조

```
jaram/
├── backend/        # Spring Boot 4.0.7 + Java 25 (Gradle) REST API
│   └── src/main/java/com/jaram/   # package-by-feature: auth, classroom, child,
│                                  #   memo, journal, report, ai, common, config
├── frontend/       # Vue 3 + Vite SPA
├── docs/           # 통합 기획서
└── _bmad-output/   # PRD · 아키텍처 · 에픽/스토리 (BMad 산출물)
```

## 기술 스택

| 영역      | 선택                                   |
|-----------|----------------------------------------|
| 백엔드    | Java 25 LTS · Spring Boot 4.0.7 · Gradle 9.5 |
| DB        | MySQL 8.x · Spring Data JPA · Flyway (스키마 정본) |
| 인증      | Spring Security · JWT · BCrypt          |
| AI        | 외부 LLM API (Claude/OpenAI) · RestClient |
| 프론트    | Vue 3 (Composition API) · Vite 8       |
| 인프라    | Docker + docker-compose (예정)          |

## 개발 실행

### 백엔드

```bash
cd backend
cp .env.example .env          # 환경변수 채우기
./gradlew build               # 빌드 + 테스트
./gradlew bootRun             # 실행 (MySQL 필요)
```

- 기동에는 MySQL 이 필요하다(`db/migration` 의 Flyway 마이그레이션 적용).
- 헬스체크: `GET /actuator/health`

### Docker 로 한 번에 (app + mysql)

```bash
cp .env.example .env          # 시크릿 채우기 (.env 는 커밋 금지)
docker compose up -d --build  # app + mysql 기동, Flyway 자동 적용
curl localhost:8090/actuator/health   # {"status":"UP"}
```

- 호스트 포트 충돌 시 `APP_PORT=8091 docker compose up -d` 로 변경 가능.
- dev 프로파일은 시드 교사(`teacher@jaram.dev` / `password1234`)를 생성해 로그인을 바로 시험할 수 있다.

### 프론트엔드

```bash
cd frontend
npm install
npm run dev                   # http://localhost:5273 (→ /api 는 :8090 으로 프록시)
npm run build                 # 프로덕션 빌드
```

## 현재 상태

**Epic 1 완료** — 프로젝트 골격 · 인증(JWT) · 반 선택 · 아이 등록·관리 · 워킹 스켈레톤 Docker 배포.
다음: Epic 2(메모 기록·타임라인) → Epic 3(AI 보육일지) → Epic 4(개인평가) → Epic 5(prod 배포). 상세는 `docs/specs/backend-implementation-plan.md`.
