# 온도 — 리뷰 · 트러블슈팅 로그

> 주요 결정에 대한 멀티관점 검토에서 나온 지적과, 그에 따라 적용한 수정을 누적 기록합니다.
> 나중에 트러블슈팅·회고용으로 이 문서만 따로 보면 됩니다. 최신 항목이 위로 오도록 추가합니다.

---

## 2026-09-10 · 비밀번호를 바꿔도 기존 JWT 가 살아 있던 문제

**맥락:** `deferred-work.md` 에 **Medium·보안**으로 2026-07-05부터 보류돼 있던 항목. 무상태 JWT 라 비밀번호 해시를 교체해도 이미 발급된 토큰이 만료(1시간)까지 그대로 유효했다. **계정을 도난당한 사람이 쓸 수 있는 대응이 비밀번호 변경인데, 그 대응이 최대 한 시간 동안 듣지 않았다**는 뜻이다. 보류 사유는 "JwtAuthFilter 구조 변경이 필요해 별도 스토리로".

### 선택한 방식

보류 당시 후보는 두 가지였다 — ①토큰 버전 claim ②`passwordChangedAt` 비교. **②를 택했다.** ①은 토큰에 버전을 싣고 계정에 카운터를 두는 방식이라 발급·검증 양쪽을 바꿔야 하는데, ②는 이미 토큰에 들어 있는 `iat` 를 그대로 쓰므로 발급 로직을 건드리지 않는다.

- `teacher.password_changed_at`(V9)에 변경 시각 기록.
- `JwtAuthFilter` 가 그보다 먼저 발급된 토큰을 **401 `AUTH_TOKEN_REVOKED`**(신설)로 거절.
- 비밀번호 변경(로그인 상태)과 재설정(메일 링크)이 **둘 다 `Teacher.changePassword()` 를 지난다.** 엔티티 메서드 한 곳에 시각 기록을 넣는 것만으로 두 경로가 함께 커버됐다 — 서비스 두 곳에 각각 넣었다면 한쪽을 빠뜨렸을 자리다.

### 판단이 갈린 지점

| 쟁점 | 선택 | 이유 |
|---|---|---|
| 변경 직후 **현재 기기**도 로그아웃할 것인가 | **유지** | 변경 응답을 204 → 200 으로 바꿔 새 토큰을 함께 내려주고 프론트가 교체. 다른 기기 세션만 끊긴다. 본인이 비밀번호를 바꾸자마자 로그인 화면으로 튕기는 것은 보안 이득 없이 혼란만 준다 |
| 에러코드 신설 vs `AUTH_TOKEN_EXPIRED` 재사용 | **신설** | 프론트는 401 을 일괄 처리해 변경이 필요 없고, 로그·Sentry 에서 "만료"와 "무효화"를 구분할 수 있다 |
| 인증 경로에 DB 조회 추가 | **감수** | 무상태였던 필터에 요청당 쿼리가 하나 생긴다. 2026-09-09 항목과 같은 방식으로 스칼라 projection 만 읽어 엔티티는 로드하지 않는다. 라이브 응답시간 0.42~0.46초로 체감 변화 없음 |

### 알려진 한계 — `iat` 초 단위

JWT 의 `iat` 는 초 단위라 변경 시각도 초로 잘라 비교한다. **같은 초에 발급된 토큰은 통과한다.** 이는 변경 직후 재발급한 토큰을 살리기 위한 선택이고, 그 대가로 최대 1초의 창이 남는다 — 공격자가 정확히 그 1초 안에 발급받은 토큰을 들고 있어야 하므로 감수했다. 없애려면 토큰 버전 claim(①)으로 가야 한다.

### 테스트에서 드러난 것

바로 이 초 단위 특성 때문에 **테스트가 실행 속도에 좌우된다.** `setUp` 에서 만든 토큰과 비밀번호 변경이 같은 초에 일어나면 무효화가 일어나지 않아 통과해 버린다. `sleep` 을 넣는 대신 변경 시각을 벌크 업데이트로 명시적으로 뒤로 밀어 결정적으로 만들었다. (Epic 3·4 회고의 "결정적 테스트" 패턴 — 래치 기반 경합, ±10초 시드와 같은 계열)

- 신규 4건: 변경 응답 200+새 토큰 / 재발급 토큰 즉시 사용 가능 / 변경보다 먼저 발급된 토큰 401 `AUTH_TOKEN_REVOKED` / 재설정도 변경 시각을 기록
- 전체 **212개 통과**

### 검증

- 배포는 **프론트 → 백엔드 순서.** 구버전 프론트는 새 토큰을 받지 못해 변경 직후 로그아웃되므로, 백엔드를 먼저 올리면 그 사이 사용자가 튕긴다.
- Cloud Run 리비전 `ondo-api-00018-l2f`. Flyway V9 적용 확인, 로그 에러 없음.
- 라이브에서 로그인 200 · 인증 GET 200 · 위조 토큰 401 확인.
- ⚠️ **무효화 자체는 라이브 미검증** — 데모 계정이 읽기 전용이라 비밀번호 변경이 403 으로 막힌다(의도된 동작). 실계정 확인 필요.

### 남은 것

- 같은 리뷰에서 나온 **비밀번호 확인 엔드포인트 브루트포스 가드**(Medium·보안)는 여전히 미해결. 로그인 엔드포인트와 함께 공통 레이트리밋으로 다뤄야 해 범위를 분리했다.

---

## 2026-09-09 · 사진(LONGBLOB) 과다 조회 2건 — 벤치 실측으로 발견·제거

**맥락:** `bench/` 하네스(2026-08-10 추가)가 남긴 지표를 다시 읽다가, 응답 크기와 DB→앱 전송량이 심하게 어긋나는 엔드포인트를 발견. 하네스를 만들 때 "LONGBLOB 과다 조회를 드러내는 것"이 목적이었는데 **측정만 하고 수정은 들어가지 않은 상태**였다.

### ① 아이 명단 API가 사진 원본을 통째로 읽음

**증상:** `GET /classrooms/{id}/children` 이 응답 0.6KB 를 만들면서 MySQL 에서 **5,425KB** 를 읽어옴(사진 보유 19명·5.3MB).

**원인:** `ProfilePhotoRepository.findByOwnerKindAndOwnerIdIn` 이 엔티티(`List<ProfilePhoto>`)를 반환. `@Lob byte[] data` 는 기본 속성이라 지연 로딩이 걸리지 않아 아바타 **갱신시각 하나** 얻자고 사진 원본 전량이 딸려왔다. 정작 그 메서드의 주석은 "데이터 BLOB 은 로드하지 않도록 updatedAt 만"이라고 적혀 있었다 — **의도와 구현이 어긋난 채로 통과한 케이스**다.

**수정:** 갱신시각 전용 projection(`OwnerUpdatedAt`) + JPQL 로 select 절을 `ownerId·updatedAt` 두 컬럼에 고정. 단건 경로(`updatedAtOrNull` — 아이 상세·교사 프로필·로그인 응답)도 같은 결함이라 함께 교체. 사진 바이트가 실제로 필요한 `find()`(이미지 응답·업서트)는 엔티티 조회 유지. 호출부 변경 없음.

### ② 아바타 ETag 재검증이 304 를 주면서도 사진을 읽음

**증상:** `If-None-Match` 재검증이 304(본문 0B)를 반환하는데도 요청당 DB 에서 **325KB** 를 읽음. 아바타가 아이 수만큼 붙는 명단 화면에서는 재방문 한 번이 사진 전량(19장·5.4MB)을 헛읽는 셈.

**원인:** 컨트롤러가 엔티티를 먼저 통째로 읽고, 그 ETag 로 Spring MVC 가 304 를 만들던 구조. **네트워크만 아끼고 DB 는 그대로**였다. bench 의 해당 측정 블록 주석이 이미 이 가능성을 적어두고 있었다("네트워크는 아껴도 DB 는 그대로일 수 있다").

**수정:** ETag 재료인 갱신시각만 먼저 읽고 `WebRequest.checkNotModified` 로 끊은 뒤, 실제로 바뀐 경우에만 바이트를 가져오도록 순서를 뒤집음.
- `If-None-Match` 파싱은 직접 하지 않고 Spring 에 위임 — 약한 검증자(`W/"…"`)·다중 값·`*` 처리에서 틀리기 쉽다.
- ETag 형식(갱신시각 epoch millis)은 **그대로 유지** — 형식을 바꾸면 이미 캐시를 가진 브라우저가 전부 재다운로드한다.
- `Cache-Control` 을 304 에도 직접 실음 — `checkNotModified` 는 상태코드와 ETag 만 세팅해서, 안 실으면 기존 200 대비 캐시 정책이 빠진다.
- 두 조회 사이에 사진이 삭제되는 경합을 대비해 두 번째 조회가 비면 404.

### 측정 (bench, 반 1 · 아이 23명 · 사진 19장 5.3MB, 30회 반복)

| 지표 | before | after |
|---|---:|---:|
| `api.children.db_kb_per_req` | 5,425.7KB | **4.2KB** (−99.9%) |
| `api.children.p50_ms` | 11.7ms | **6.4ms** (−45.3%) |
| `api.children.p95_ms` | 19.7ms | **8.4ms** (−57.4%) |
| `etag.revalidate_db_kb` | 325.0KB | **1.5KB** (−99.5%) |

**읽은 행 수(43)와 응답 크기(0.6KB)는 그대로**다. 쿼리 결과를 바꾼 게 아니라 불필요하게 딸려오던 사진 원본만 사라졌다는 직접 증거.

**대가:** ②로 인해 캐시 미스 경로는 조회가 1건 늘어난다(`api.photo` 3행 → 4행, p50 6.3 → 7.2ms). 재검증이 최초 다운로드보다 훨씬 잦으므로 남는 거래로 판단.

### 검증

- 테스트 **209개 통과**(0 실패). ETag 응답 계약(304 · ETag 유지 · Cache-Control · 미스 시 바이트 반환)을 고정하는 통합테스트 신설.
- Cloud Run 배포 후 라이브에서 아이 명단 API 정상(200 · 아이 23명 · 0.30초), 서버 로그 에러 없음. 리비전 `ondo-api-00017-c9m`.
- ⚠️ **②는 라이브 검증 못 함** — 데모 계정의 아이 23명 전원이 프로필 사진이 없어 아바타 요청 자체가 성립하지 않는다. 근거는 로컬 실측과 통합테스트뿐. 사진이 등록된 실제 반에서 아바타 요청이 304 로 뜨는지 확인 필요.

### 남은 것 / 교훈

- **주석이 계약이 아니다.** ①은 "BLOB 을 읽지 않는다"는 주석을 단 채 정확히 그 반대로 동작했고, 리뷰도 이를 잡지 못했다. 과다 조회는 기능 테스트로는 절대 드러나지 않는다(응답은 정상이다) — **측정 하네스가 없었으면 계속 몰랐을 결함**이다.
- 같은 이유로, 새 목록 API 를 추가할 때 `db_kb_per_req` 를 한 번 재보는 것을 기본 절차로 둘 만하다.
- `deferred-work.md` 의 `(child_id, created_at)` 복합 인덱스 부재(타임라인 `Using filesort`)는 이번 범위 밖 — 유지.

---

## 2026-06-19 · 서비스명 리브랜딩 (자람 → 온도 / jaram → ondo)

**결정:** 서비스명을 **자람(jaram)** → **온도(ONDO)** 로 전면 변경.
**사유:** 영유아 교육 업계에 동일·유사 명칭 브랜드가 다수 존재해 검색 식별성 확보가 어려움. 새 이름 '온도'는 "**온**전히 **도**와드리겠습니다"라는 약속 + 교사 곁의 따뜻한 온기를 담음. 슬로건: **온전히 도와드리겠습니다.**

### 변경 범위
- **Java**: 패키지 `com.jaram`→`com.ondo`, 메인 클래스 `JaramApplication`→`OndoApplication`, `@ConfigurationProperties` prefix `jaram.ai`/`jaram.jwt`→`ondo.*`
- **빌드/설정**: Gradle group/rootProject, `application.yml`(app name·DB 기본값·로깅·`ondo:` 블록), `.env.example`
- **인프라**: docker-compose(DB명/계정 `ondo`, 볼륨 `ondo-mysql-data`, DB_URL), Flyway V1 주석. DB는 dev 데이터라 `docker compose down -v`로 폐기 후 재생성.
- **프론트**: `index.html` title, stores localStorage 키(`ondo.*`), 로고 alt, 목업 전체 + 파일명(`자람.html`→`온도.html`, `jaram-ui.jsx`→`ondo-ui.jsx`)
- **문서/설정**: README(제목·슬로건), `docs/specs/*`, `_bmad/{core,bmm}/config.yaml`·`config.toml` project_name. `_bmad-output/planning-artifacts/` 과거 산출물은 폴더/파일명·본문 보존 + 상단 1줄 주석만.

### 검증
- `./gradlew clean build` 통과 — 전체 41개 테스트 GREEN.
- 프론트 `vite build` 통과.
- `docker compose up` 후 앱 healthy + `ondo` 스키마 Flyway 적용 + 시드 교사 생성 확인.
- 추적 대상 잔존 `jaram/자람` = 리브랜딩 프롬프트 지시문서뿐(의도).

---

## 2026-06-15 · 아키텍처 Step 6 (프로젝트 구조) — 파티 모드 검토

**맥락:** package-by-feature 프로젝트 구조안을 파티 모드로 검토. 참여: 🏗️ Winston, 💻 Amelia.

### 제기된 지적 → 적용한 수정

| # | 지적 (제기자) | 심각도 | 적용한 수정 |
|---|---------------|--------|-------------|
| 1 | AI 응답 파싱 타입 부재 — 없으면 journal이 String 직접 파싱(테스트 취약) (Amelia) | High | `ai/dto/`에 **AnalysisRequest·AnalysisResult** 추가. OutputValidator는 AnalysisResult 검증. |
| 2 | 도메인 예외 카탈로그 미정(복원실패·타임아웃·검증실패·동시분석거부 4종) (Amelia) | High | `common/exception/`에 **ErrorCode(enum) + GlobalExceptionHandler**로 4종→HTTP 상태 매핑 명시. |
| 3 | 비식별 토큰 매핑 수명·복원 지점 모호 (Amelia) | Med | `ai/deid/`에 **RestorationContext**(요청 스코프 매핑 보관, 복원 실패 시 예외 발생점). 오케스트레이션이 생성·소유. |
| 4 | AnalysisGuard는 AI가 아니라 오케스트레이션 자원 (Winston, Amelia) | Med | **`common/`으로 이동**, **단일 인스턴스 in-memory**(lock/ConcurrentHashMap) 구현 명시. |
| 5 | ClaudeAiClient 타임아웃 테스트 불가(20s 실대기) (Amelia) | Med | `AiClientConfig`에 **타임아웃 properties 바인딩 + 생성자 주입**(테스트 50ms). `test/resources/ai/` WireMock fixture 경로 선점. |
| 6 | 두 PersistService 공통 추상화 유혹 (Winston) | Med | **합치지 않음** — 규칙(no-TX 오케스트레이션/REQUIRES_NEW persist)만 공유, 코드는 복제. |
| 7 | 월말 멱등성 마커가 어느 테이블에 사는지 불명 (Winston) | Med | **CHILD_REPORT UNIQUE(child_id, report_month)** 로 멱등 보장(구조·DDL에 명시). |
| 8 | BaseTimeEntity에 soft delete 혼입 위험 (Winston) | Low | BaseTimeEntity는 **시간 필드만**. soft delete는 별도 인터페이스/베이스로 분리(CHILD·MEMO만). |
| 9 | Prompt Loader 치환 책임 불명 (Amelia) | Low | PromptTemplateLoader에 placeholder 치환 책임 명시, 누락 placeholder 탐지 테스트. |
| 10 | JournalMemoLink 정합성(memo soft delete 시) (Amelia) | Low | link write는 JournalPersistService. memo soft delete 시 link 처리 규칙 명시(끊김 허용). |

### 보류 / 근거
- **timeline/ 별도 패키지 분리** 보류 — TimelineController가 journal/report repo를 직접 끌어 쓰기 전엔 child/ 유지(YAGNI). 그 시점이 분리 신호.
- **ai/ 인프라·정책 폴더 분리** 보류 — 폴더는 두되 의존성 방향(정책은 ClaudeAiClient 모름)만 규율로 고정.

### 합의가 강했던 지점
- AnalysisGuard 위치/구현(#4), 예외·실패의 구조적 표현(#1·#2).

---

## 2026-06-15 · 아키텍처 Step 4 (핵심 결정) — 파티 모드 검토

**맥락:** 아키텍처 워크플로우 Step 4의 핵심 결정안(데이터/보안/API/AI연동/인프라)을 BMad 파티 모드로 검토. 참여: 🏗️ Winston(아키텍트), 💻 Amelia(시니어 엔지니어), 📋 John(PM).

### 제기된 지적 → 적용한 수정

| # | 지적 (제기자) | 심각도 | 적용한 수정 |
|---|---------------|--------|-------------|
| 1 | 동기 AI 호출(~15s)이 리버스 프록시/LB 타임아웃에 가로채여 `@RestControllerAdvice` 에러 처리가 무력화됨 (Winston) | High | **서버 측 하드 타임아웃(예 20s)을 클라이언트/프록시보다 짧게** 설정해 서버가 먼저 끊고 도메인 예외로 제어. 동기 유지(이미 구현된 프론트가 로딩·에러 3상태를 기대 — FR-4). |
| 2 | 타임아웃에까지 재시도하면 대기시간 2배 (Winston) | Med | 재시도는 **연결실패·5xx에만**, 타임아웃엔 재시도 안 함. |
| 3 | "배치/동기" 서술이 자기모순 — 실제 호출 경로 3종 (Amelia) | High | AI 경로를 **3종으로 명시·정책 분리**: ①동기 on-demand(FR-3 일지, FR-8 수동평가) ②야간 묶음(FR-2 영역분류는 일지 분석 패스에 포함) ③월말 @Scheduled(FR-9). 각자 timeout/retry 정책 별도. |
| 4 | TX 경계 분리는 메모 저장엔 무관 (Amelia) | Med | TX 분리 적용 대상을 **`JournalService`·`ReportService`로 한정**. `MemoService.save()`는 외부 의존성 0(동기 DB insert만). |
| 5 | Spring self-invocation으로 TX 분리가 안 먹음 (Winston, Amelia) | High | `REQUIRES_NEW` 결과 저장은 **별도 빈으로 분리**(또는 `TransactionTemplate`). 오케스트레이션 메서드엔 `@Transactional` 미부여. |
| 6 | 비식별화 복원 키 깨짐 — "아이A"는 조사 결합("아이A가")으로 단순 replace 실패 (Amelia) | High | alias를 **센티넬 토큰 `[[CHILD_1]]` 형식**으로(자연어 변형 안 붙음). 요청 스코프 매핑 테이블, **복원 실패 시 정책**(원문에 alias 잔존 시 도메인 예외) 명시. |
| 7 | 비식별화가 입력(메모 저장) 경로에 끼면 입력속도 가드(SM-1) 위협 (John) | High | 비식별화는 **분석 경로에서만** 수행. 메모 저장 = 외부 의존성 0을 명시적 제약으로 고정. |
| 8 | 메모 본문에 박힌 **다른 아이 이름**은 토큰화 안 됨 → 비식별화 누수 (Winston) | Med | MVP 한계로 **명시 문서화** + (선택) 반 명단 기반 단순 치환을 보강책으로 기록. |
| 9 | `DAILY_JOURNAL` 유니크(teacher,classroom,date) + soft delete 충돌 — 삭제 후 재생성 시 위반 (Amelia) | High | **soft delete는 자산(CHILD·MEMO)에만**. 일지는 재생성 산출물이라 **덮어쓰기=UPDATE in-place**(soft delete 안 함) → 유니크 충돌 원천 제거. |
| 10 | FR-6 덮어쓰기 vs CHILD_REPORT 누적 정책 상충 (Amelia) | Med | **일지=UPDATE 덮어쓰기**(재분석 시 JOURNAL_MEMO_LINK 갱신: 기존 링크 삭제 후 재삽입), **개인평가=신규 행 누적**(append)으로 정책 분리 명시. |
| 11 | 월말 @Scheduled 멱등성/부분실패 미정 (Winston, Amelia) | High | **유니크(child_id, report_month)** + 잡 실행 로그, **이미 생성분 skip**, **아이 1명 단위 트랜잭션·try-catch 격리**(부분 성공·재실행 가능). SM-4(누락 0%) 보장. |
| 12 | JWT 무상태라 즉시 무효화 불가 (Winston, Amelia) | Med | **짧은 만료(예 1h) + 리프레시 없음(재로그인)** MVP 정책으로 명시. 시크릿 env + `.gitignore` 확인. ownership 체크는 repository 레벨 강제. |
| 13 | 차별화의 핵심인 프롬프트 출력에 검증 장치 없음 (John) | High | 생성 결과에 **누리 5영역·또래상호작용·교사지원 포함 여부 경량 검증**, 누락 시 1회 재요청. |
| 14 | 영역 분류를 별도 야간 잡으로 쪼갬 = 유지보수 부채 (John) | Med | **분석 생성 패스에 영역 분류 통합**(별도 스케줄 잡 제거). |
| 15 | AI 비용 폭주 방어 부재 (Winston) | Low | **사용자당 동시 분석 1건** 가벼운 가드(진행 중이면 거절). |
| 16 | 손배포 실수 위험 (Winston) | Low | **한 줄 배포 스크립트**(pull→build→up -d→헬스체크) 권고. (post-MVP 가능) |

### 보류 / 근거
- **완전 비동기(202 + 폴링/SSE) 전환**은 보류 — 프론트가 동기 로딩·에러 UX(FR-4)로 이미 구현 완료라 동기+하드타임아웃이 충돌 없이 안전. 비동기는 v1.5 후보로 기록(트래픽/지연이 문제될 때 재검토).
- **NER 기반 본문 이름 스크러빙**은 MVP 범위 밖(#8). 반 명단 치환으로 갈음 가능.

### 합의가 강했던 지점
- (A) 동기 15초 호출 — 세 명 모두 위험 지적 → 하드 타임아웃 + 정책 분리로 수습.
- (B) 비식별화 위치·복원 — 세 명 모두 지적 → 분석 경로 한정 + 센티넬 토큰.

---

## 2026-06-15 · 구현 명세서 작성 (data-model / api / error-code)

**맥락:** 코드 구현에 바로 쓸 implementation-ready 명세 3종 생성(`docs/specs/`). 아키텍처·에픽·PRD를 정본으로 삼아 컬럼/엔드포인트/에러코드 레벨로 구체화하면서 드러난 명료화·결정 사항 기록.

### 명세화 중 드러난 갭 → 처리

| # | 사항 | 처리 |
|---|------|------|
| 1 | `child.token_alias`(FR-11 저장형) vs 비식별화 센티넬 `[[CHILD_n]]`(아키텍처/리뷰로그 #6, 요청 스코프) 상충 | 정합 해석 문서화: token_alias는 DB 저장 안정 가명(표시·보조), 실제 프롬프트 치환은 분석 요청마다 in-memory `[[CHILD_n]]` 매핑. 최종 확정은 AI 연동 명세로 이관. (data-model §6) |
| 2 | FR-8 평가 기간 기본 시작점 "반 학년도 시작일" 저장 위치 없음 | `classroom.start_date DATE NOT NULL` 신설. |
| 3 | `child_report` 수동 누적 vs 월말 멱등 상충 | `report_month` NULL 허용 + UNIQUE(child_id, report_month). MySQL UNIQUE의 NULL 다중허용으로 수동(NULL)은 무제한 누적, 자동은 (child,month) 1건 skip 멱등. |
| 4 | 일지 재분석 판정 기준 컬럼 | `daily_journal.analyzed_at` 신설(memo.created_at > analyzed_at = 재분석 필요). `status`(DRAFT/CONFIRMED)로 FR-5 확정 표현. |
| 5 | 메모 소유권 인가 경로 | `memo.teacher_id` FK 보유(repository 레벨 소유권 검증용). |
| 6 | 일지 `content` 구조(5영역 객체 vs 평문) | API는 5영역 객체 직렬화 가정, 내부 스키마 확정은 AI 연동 명세로 이관(api §9). |
| 7 | 메모 "최소 1필드" 제약 위치 | DB CHECK 대신 앱 레이어(`MEMO_EMPTY`) 강제 — 이식성. |
| 8 | 교사 가입 API 부재 | MVP 단일 담임 → 시드/마이그레이션 주입 전제(data-model §5). 다계정 시 `POST /auth/signup` 후속. |
| 9 | 타 교사 리소스 접근 응답 | 존재 비노출 위해 기본 `404`, 명백한 위반만 `403`. 보안 리뷰 후 일관 적용(미확정). |

### 정합성 확인
- FR-1~11 전부 엔드포인트로 커버(api §1 요약표), NFR(비식별화·동시1건·자산무손실·soft delete·BCrypt)가 스키마·에러코드에 반영됨.
- AI 계열 에러 메시지 전부 FR-4("메모는 그대로 보존") 고지 포함.
- 네이밍(snake_case DB · camelCase JSON · /api/v1 복수명사)·TX/soft delete 정책이 아키텍처 결정과 충돌 없음.

### 보류 / 후속
- AI 연동 명세(AiClient 시그니처·AnalysisRequest/Result·프롬프트 3종·OutputValidator)는 사용자 선택에서 제외 → 별도 작업으로 분리.
