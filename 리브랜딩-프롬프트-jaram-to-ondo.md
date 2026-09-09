# 리브랜딩 작업 프롬프트 — `jaram(자람)` → `온도(ONDO)`

> **✅ 완료된 작업입니다.** 2026-06-19에 커밋 `ae34bca` 로 수행됐고, 결과는
> [트러블슈팅 로그](docs/troubleshooting-log.md)의 같은 날짜 항목에 남아 있습니다.
> 아래는 당시 지시문 원본으로, 어떻게 진행했는지 기록으로 보관합니다.

---

## 작업 지시

이 프로젝트의 **서비스명을 `자람(jaram)`에서 `온도(ONDO)`로 전면 변경**한다.
단순 문자열 치환이 아니라 항목별로 성격이 다르므로, 아래 규칙을 정확히 지켜라.

### 0. 네이밍 기준 (이 표를 단일 기준으로 사용)

| 용도 | 기존 | 변경 후 |
|------|------|---------|
| 화면 표시 한글명 | 자람 | 온도 |
| 영문/라틴 표기 | jaram / Jaram / JARAM | ondo / Ondo / ONDO |
| Java 패키지 | `com.jaram` | `com.ondo` |
| 메인 클래스 | `JaramApplication` | `OndoApplication` |
| Gradle group | `com.jaram` | `com.ondo` |
| Gradle rootProject | `jaram` | `ondo` |
| DB 스키마/계정명 | `jaram` | `ondo` |
| 슬로건(신규 추가) | — | **온전히 도와드리겠습니다** (서브: 선생님 곁의 따뜻한 온도) |

**대소문자 매핑 규칙:** `jaram→ondo`, `Jaram→Ondo`, `JARAM→ONDO`. 한글 `자람→온도`.

### 1. 먼저 전체 현황을 파악하라
작업 시작 전에 아래로 잔존 위치를 모두 수집하고, 변경 계획을 보고한 뒤 진행하라.
```
grep -rinI 'jaram' . --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=dist
grep -rlI '자람' . --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=dist
```

### 2. Java 백엔드 (패키지 이동 — 가장 주의)
- 디렉토리 이동: `backend/src/main/java/com/jaram` → `.../com/ondo`,
  `backend/src/test/java/com/jaram` → `.../com/ondo` (`git mv` 사용).
- 모든 `.java` 파일의 `package com.jaram...` 선언과 `import com.jaram...` 구문을 `com.ondo...`로 변경.
- 클래스 리네임: `JaramApplication` → `OndoApplication`, `JaramApplicationTests` → `OndoApplicationTests`
  (파일명·클래스명·참조 모두).
- `@ConfigurationProperties(prefix = "jaram...")` 등 커스텀 프로퍼티 prefix가 있으면 `application.yml`의 `jaram:` 키와 함께 `ondo:`로 일관되게 변경.

### 3. 빌드/설정 파일
- `backend/settings.gradle`: `rootProject.name = 'ondo'`
- `backend/build.gradle`: `group = 'com.ondo'`
- `backend/src/main/resources/application.yml`:
  - `spring.application.name: ondo`
  - DB url/username/password 기본값의 `jaram` → `ondo`
  - 커스텀 `jaram:` 설정 블록 → `ondo:`
  - 로깅 `com.jaram` → `com.ondo`
- 테스트 리소스 `backend/src/test/resources/application.yml`도 동일하게.

### 4. 인프라 / 환경
- `docker-compose.yml`: `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD` 기본값, `DB_URL`의 스키마명, 볼륨명 `jaram-mysql-data` → `ondo-mysql-data`.
- `.env.example`: `DB_USERNAME` 등.
- ⚠️ **DB명/볼륨명을 바꾸면 기존 로컬 MySQL 볼륨의 데이터에 접근하지 못한다.** 개발 데이터뿐이라 폐기해도 되면 기존 볼륨을 삭제(`docker compose down -v`)하고 새로 띄워라. 보존이 필요하면 사용자에게 먼저 확인하라.
- `backend/src/main/resources/db/migration/V1__init.sql` 등 마이그레이션의 주석/스키마 참조에 `자람`·`jaram`이 있으면 변경. **단, 이미 적용된 Flyway 마이그레이션의 체크섬이 바뀌지 않도록** 운영 환경 여부를 확인하고, 개발 단계면 스키마 재생성으로 처리.

### 5. 프론트엔드
- `frontend/index.html`의 `<title>` 등 표시 문구 `자람` → `온도`.
- `frontend/src/components/Logo.vue`, `frontend/src/lib/api.js`, `frontend/src/stores/*`, `frontend/src/styles/tokens.css`의 브랜드 문자열·식별자.
- `frontend/mockups/` 일체: `jaram-ui.jsx`, 각 `screens-*.jsx`, `data.jsx`, `tokens.css` 내 문자열 및 **파일명 `자람.html` → `온도.html`**(`git mv`).
- 빌드 산출물 `frontend/dist/`는 직접 수정하지 말고 **재빌드로 갱신**하라.

### 6. 문서
- `README.md`: 제목 `# 자람 (jaram)` → `# 온도 (ONDO)`, 본문 브랜드 표기 및 슬로건 반영.
- `docs/specs/*.md`: 본문 내 서비스명 표기 변경.
- `BMAD-COWORK-사용법.md` 등 루트 가이드 문서.

### 7. BMad 산출물 처리 (판단 필요 — 함부로 바꾸지 말 것)
- `_bmad-output/planning-artifacts/` 하위 PRD 등은 **날짜가 박힌 과거 산출물**이다.
  - **폴더/파일명**(`prd-jaram-2026-06-15` 등)은 링크 깨짐 방지를 위해 **그대로 유지**한다.
  - 본문은 강제로 일괄 치환하지 말고, "리브랜딩됨(구 자람 → 온도)" 한 줄 주석을 상단에 추가하는 선에서 처리할지 사용자에게 확인하라.
- `_bmad/core/config.yaml`, `_bmad/bmm/config.yaml`의 프로젝트명 설정값은 `ondo`로 변경(빌드/워크플로우가 참조하므로).

### 8. 검증 (필수)
1. `cd backend && ./gradlew clean build` 통과 확인.
2. 전체 테스트 통과 확인.
3. 잔존 검색이 의도된 항목(과거 BMad 산출물 등)만 남는지 확인:
   ```
   grep -rinI 'jaram' . --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=dist
   grep -rlI '자람' . --exclude-dir=.git --exclude-dir=build --exclude-dir=node_modules --exclude-dir=dist
   ```
4. `docker compose up` 후 앱 부팅 및 DB 연결 정상 확인.
5. 프론트 재빌드 후 화면 타이틀/로고에 "온도" 표시 확인.

### 9. 마무리
- 변경 요약(파일 수, 패키지 이동, DB명 변경, 검증 결과)을 보고하라.
- 의미 있는 단위로 커밋하라 (예: `refactor: rename service jaram → ondo (package/db/docs)`).
- **이 리브랜딩 결정과 변경 내역을 `docs/troubleshooting-log.md`에 한 항목으로 추가**하라 (날짜 2026-06-19, 사유: 영유아 교육 업계 동명 브랜드 회피 및 검색 차별성 확보).

---

### 참고: 변경 이유 (공지문에 활용)
> 서비스명을 **자람**에서 **온도(ONDO)**로 변경합니다.
> 영유아 교육 업계에 이미 동일·유사 명칭 브랜드가 다수 존재해 검색 식별성을 확보하기 어려웠습니다.
> 새 이름 **온도**는 "**온**전히 **도**와드리겠습니다"라는 약속과, 교사 곁의 따뜻한 온기를 함께 담았습니다.
> 슬로건: **온전히 도와드리겠습니다.**
