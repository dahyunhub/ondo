---
title: 온도 — API 명세서 (REST API Specification)
status: draft
created: 2026-06-15
sources:
  - _bmad-output/planning-artifacts/architecture.md (API & Communication Patterns, Naming Patterns)
  - _bmad-output/planning-artifacts/epics.md (Epic 1~5 Stories)
  - _bmad-output/planning-artifacts/prds/prd-ondo-2026-06-15/prd.md (FR-1~11)
related:
  - docs/specs/data-model-spec.md
  - docs/specs/error-code-catalog.md
---

# API 명세서 — 온도

백엔드 REST API 전 엔드포인트의 구현 명세다. 프론트(Claude Design 구현물)가 이 계약을 소비한다. UI/화면은 범위 밖.

## 0. 공통 규약

- **Base path:** `/api/v1`. 모든 리소스 경로는 복수 명사(`/children`, `/memos`, `/journals`, `/reports`).
- **인증:** `Authorization: Bearer <JWT>`. `/api/v1/auth/login`과 `/actuator/health`를 제외한 모든 엔드포인트는 인증 필수. 미인증 → `401`(`AUTH_UNAUTHENTICATED`).
- **인가:** repository 레벨에서 `teacher_id = 현재 교사`를 강제. 타 교사 리소스 접근은 `404`(존재 비노출) 또는 `403`. 본 명세는 **존재 비노출 위해 `404`(`*_NOT_FOUND`) 기본** — 단, 명백한 소유권 위반이 드러나는 경우 `403`(`AUTH_FORBIDDEN`).
- **요청/응답 본문:** `application/json; charset=utf-8`. 필드 **camelCase**(Jackson 기본). 날짜 `YYYY-MM-DD`, 시각 ISO-8601 UTC(`2026-06-15T09:30:00Z`).
- **검증:** 요청 DTO에 Bean Validation. 위반 → `400`(`VALIDATION_FAILED`), 필드별 사유 포함.
- **성공 응답:** DTO 직접 반환(래퍼 없음) + 적절한 상태코드. 생성은 `201`, 조회/수정 `200`, 본문 없는 삭제 `204`.
- **에러 응답 표준:** `{ timestamp, status, code, message, path }` — 상세·코드 목록은 `error-code-catalog.md`.
- **enum 값**(`curriculumArea`, `reportType`, journal `status`)은 `data-model-spec.md` §2/§3 정의를 그대로 사용.

## 1. 엔드포인트 요약

| # | 메서드 | 경로 | 기능 | FR | Epic/Story |
|---|--------|------|------|-----|-----------|
| 1 | POST | `/api/v1/auth/login` | 로그인·JWT 발급 | FR-10 | 1.2 |
| 1b | POST | `/api/v1/auth/register` | 회원가입·자동 로그인(JWT 발급) | FR-12 | 1.6 |
| 2 | GET | `/api/v1/classrooms` | 담당 반 목록(아이 수 포함) | FR-10 | 1.3 |
| 3 | GET | `/api/v1/classrooms/{classroomId}/children` | 반 아이 명단(가나다순) | FR-11 | 1.4 |
| 4 | POST | `/api/v1/classrooms/{classroomId}/children` | 아이 등록 | FR-11 | 1.4 |
| 5 | PUT | `/api/v1/children/{childId}` | 아이 정보 수정 | FR-11 | 1.4 |
| 6 | DELETE | `/api/v1/children/{childId}` | 아이 soft delete | FR-11 | 1.4 |
| 7 | POST | `/api/v1/memos` | 메모 저장 | FR-1 | 2.1 |
| 8 | GET | `/api/v1/children/{childId}/timeline` | 아이 타임라인(영역 필터) | FR-7 | 2.2 |
| 9 | PATCH | `/api/v1/memos/{memoId}/curriculum-area` | 메모 영역 수정 | FR-7 | 2.3 |
| 10 | DELETE | `/api/v1/memos/{memoId}` | 메모 soft delete | FR-1 | 2.1 |
| 11 | POST | `/api/v1/journals/analyze` | 하루치 일지 초안 생성 | FR-2,3,4 | 3.5 |
| 12 | GET | `/api/v1/journals` | 일지 조회(재분석 안내 포함) | FR-6 | 3.7 |
| 13 | GET | `/api/v1/journals/{journalId}` | 일지 단건 조회 | FR-5 | 3.6 |
| 14 | PUT | `/api/v1/journals/{journalId}` | 일지 수정·확정 | FR-5 | 3.6 |
| 15 | POST | `/api/v1/journals/{journalId}/analyze` | 재분석·덮어쓰기 | FR-6 | 3.7 |
| 16 | POST | `/api/v1/children/{childId}/reports` | 수동 개인평가 생성 | FR-8 | 4.1 |
| 17 | GET | `/api/v1/children/{childId}/reports` | 개인평가 목록(시간순) | FR-8 | 4.1 |
| 18 | GET | `/api/v1/reports/{reportId}` | 개인평가 단건 | FR-8 | 4.1 |
| 19 | GET | `/actuator/health` | 헬스체크 | — | 1.1/1.5 |
| 20 | GET | `/api/v1/classrooms/{classroomId}/warmth` | 아이별 관찰 온도(최근 기록 밀도) | — | spec-child-warmth |
| 21 | POST | `/api/v1/children/{childId}/warmth-snooze` | 온도 판정에서 잠시 접어두기(14일) | — | spec-child-warmth-snooze |
| 22 | DELETE | `/api/v1/children/{childId}/warmth-snooze` | 접어두기 해제 | — | spec-child-warmth-snooze |
| 23 | POST | `/api/v1/auth/password-reset/request` | 비밀번호 재설정 링크 요청 | — | spec-password-reset |
| 24 | POST | `/api/v1/auth/password-reset/confirm` | 비밀번호 재설정 확정 | — | spec-password-reset |

> 월말 자동 평가(FR-9, Story 4.2)는 `@Scheduled` 내부 동작으로 엔드포인트 없음.

---

## 2. 인증 (Epic 1)

### [1] POST `/api/v1/auth/login` — 로그인

인증 불필요. 이메일·비밀번호 검증 후 JWT 발급(NFR-6: BCrypt, 짧은 만료 ~1h, 리프레시 없음).

**Request**
```json
{ "email": "teacher@ondo.dev", "password": "••••••••" }
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `email` | string | `@NotBlank`, `@Email` |
| `password` | string | `@NotBlank` |

**Response 200**
```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "teacher": { "id": 1, "email": "teacher@ondo.dev", "name": "민지" }
}
```

**에러:** `400 VALIDATION_FAILED` · `401 AUTH_INVALID_CREDENTIALS`(이메일/비번 불일치).

### [1b] POST `/api/v1/auth/register` — 회원가입 (FR-12)

인증 불필요. 이름·이메일·비밀번호로 교사 계정을 생성한다. 비밀번호는 BCrypt 해시로 저장하고, 성공 시 로그인과 **동일한 응답**(자동 로그인 토큰)을 반환한다. 소셜 로그인은 제공하지 않는다.

**Request**
```json
{ "name": "햇살쌤", "email": "teacher@ondo.dev", "password": "••••••••" }
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `name` | string | `@NotBlank` |
| `email` | string | `@NotBlank`, `@Email` |
| `password` | string | `@NotBlank`, `@Size(min=8)` |

**Response 201** — `[1]` 로그인과 동일한 바디(`accessToken`, `tokenType`, `expiresIn`, `teacher`).

**에러:** `400 VALIDATION_FAILED` · `409 EMAIL_ALREADY_EXISTS`(이미 가입된 이메일). 동시 가입 경쟁은 DB 유니크 제약으로 `409 DATA_CONFLICT` 처리.

### [23] POST `/api/v1/auth/password-reset/request` — 재설정 링크 요청

인증 불필요. 가입된 이메일이면 재설정 링크를 메일로 보낸다.

**Request** `{ "email": "teacher@ondo.dev" }` · **Response 204**

> ⚠️ **가입 여부와 무관하게 항상 204**, 본문도 동일하다. 응답이 갈리면 이 엔드포인트가 "이 이메일이 가입돼 있나?"를 알려주는 조회기가 된다. 메일 발송이 실패해도 204 를 유지한다(실패는 서버 로그로만).

- 링크 유효기간 **30분**, **1회용**.
- 재요청하면 **이전 링크는 즉시 무효**가 된다.
- **카카오 전용 계정**(비밀번호 없음)은 토큰을 만들지 않고 "카카오로 로그인하세요" 안내 메일만 보낸다. 응답은 동일.

**에러:** `400 VALIDATION_FAILED`(이메일 형식).

### [24] POST `/api/v1/auth/password-reset/confirm` — 재설정 확정

인증 불필요. 링크의 토큰으로 비밀번호를 교체한다. **자동 로그인하지 않는다.**

**Request** `{ "token": "...", "newPassword": "••••••••" }` · **Response 204**

| 필드 | 검증 |
|------|------|
| `token` | `@NotBlank` |
| `newPassword` | `@NotBlank`, `@Size(min=8)`, `@MaxBytes(72)` — 가입과 동일 |

- 성공 시 해당 계정의 **남은 재설정 링크도 모두 폐기**된다.
- 비밀번호 검증 실패(400)로 끝나면 **토큰은 그대로 살아 있다** — 메일을 다시 받지 않아도 재시도할 수 있다.

**에러:** `400 VALIDATION_FAILED` · `400 RESET_TOKEN_INVALID`(만료·재사용·위조를 **구분하지 않음**).

---

## 3. 반·아이 관리 (Epic 1)

### [2] GET `/api/v1/classrooms` — 담당 반 목록

현재 교사 소유 반만. 학년도(year)로 동명 반 구분.

**Response 200**
```json
[
  { "id": 10, "name": "햇살반", "year": 2026, "ageClass": 4, "startDate": "2026-03-02", "childCount": 18 },
  { "id": 7,  "name": "햇살반", "year": 2025, "ageClass": null, "startDate": "2025-03-02", "childCount": 20 }
]
```
- `childCount`: soft delete 안 된 아이 수.
- `ageClass`: 만 나이(0~5) 또는 `null`(미지정·혼합연령반). 프론트가 아이 생년월일 입력의 **기본 표시 연도**를 `year − (ageClass + 1)` 로 계산하는 데 쓴다. 값이 없으면 `year − 5`(만 4세)로 대체한다. **선택 범위를 제약하지는 않는다.**

**반 생성 요청**(`POST /api/v1/classrooms`)도 `ageClass` 를 선택적으로 받는다 — `@Min(0) @Max(5)`, 생략 가능.

### [3] GET `/api/v1/classrooms/{classroomId}/children` — 아이 명단

**Response 200** — `name` 가나다순 정렬, 삭제 아동 제외.
```json
[
  { "id": 101, "name": "김민준", "birthDate": "2021-04-10", "gender": "MALE",   "tokenAlias": "아이A" },
  { "id": 102, "name": "박서연", "birthDate": "2021-07-22", "gender": "FEMALE", "tokenAlias": "아이B" }
]
```
- `gender`: enum `MALE`/`FEMALE`(한글 남/여 표기는 프론트 매핑).

**에러:** `404 CLASSROOM_NOT_FOUND`(타 교사 반 포함).

### [4] POST `/api/v1/classrooms/{classroomId}/children` — 아이 등록

**Request**
```json
{ "name": "이도윤", "birthDate": "2021-02-15", "gender": "MALE" }
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `name` | string | `@NotBlank`, max 100 |
| `birthDate` | date | `@NotNull`, `@Past`, 하한(유치원 연령 범위) |
| `gender` | enum | `@NotNull`, `MALE`/`FEMALE` |

**동작:** 등록 시 `tokenAlias` 자동 부여(반 내 유일). **Response 201**
```json
{ "id": 103, "name": "이도윤", "birthDate": "2021-02-15", "gender": "MALE", "tokenAlias": "아이C" }
```
**에러:** `400 VALIDATION_FAILED` · `404 CLASSROOM_NOT_FOUND`.

### [5] PUT `/api/v1/children/{childId}` — 아이 수정

**Request:** [4]와 동일 스키마. **Response 200**: 수정된 child. **에러:** `400` · `404 CHILD_NOT_FOUND`.

### [6] DELETE `/api/v1/children/{childId}` — 아이 삭제(soft)

물리 삭제 아님. `deleted_at` 설정, 관찰 기록 보존(NFR-2). 이후 명단·타임라인에서 제외. **Response 204**. **에러:** `404 CHILD_NOT_FOUND`.

---

## 4. 메모 & 타임라인 (Epic 2)

### [7] POST `/api/v1/memos` — 메모 저장 (FR-1, NFR-5)

외부 AI 호출 없이 **동기·즉시 저장**. `curriculumArea`는 받지 않음(밤 분석에서 분류).

**Request**
```json
{
  "childId": 101,
  "content": "블록으로 높은 탑을 쌓고 친구에게 같이 하자고 함",
  "playActivity": "블록 쌓기",
  "interaction": "친구에게 먼저 제안",
  "attitude": "집중도 높음"
}
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `childId` | number | `@NotNull` (없으면 저장 불가) |
| `content` | string | nullable, max 2000 |
| `playActivity` | string | nullable, max 500 |
| `interaction` | string | nullable, max 500 |
| `attitude` | string | nullable, max 500 |

**불변식:** `content`/`playActivity`/`interaction`/`attitude` 중 **최소 1개 non-blank**. 모두 비면 `400 MEMO_EMPTY`.

**Response 201**
```json
{
  "id": 5001, "childId": 101,
  "content": "블록으로...", "playActivity": "블록 쌓기",
  "interaction": "친구에게 먼저 제안", "attitude": "집중도 높음",
  "curriculumArea": null,
  "createdAt": "2026-06-15T05:12:00Z"
}
```
**에러:** `400 VALIDATION_FAILED` · `400 MEMO_EMPTY` · `404 CHILD_NOT_FOUND`.

### [8] GET `/api/v1/children/{childId}/timeline` — 타임라인 (FR-7)

한 아이의 모든 메모를 **시간순(최신 우선 권장)** 으로. 영역 필터 지원.

**Query**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `area` | enum? | `PHYSICAL_HEALTH`/`COMMUNICATION`/`SOCIAL`/`ART`/`NATURE`/`UNCLASSIFIED`. 생략 시 전체 |

- `area=UNCLASSIFIED` → `curriculum_area IS NULL`(미분류) 필터.

**Response 200** — 메모 없으면 빈 배열 `[]`(빈 상태).
```json
[
  { "id": 5001, "date": "2026-06-15", "content": "블록으로...", "curriculumArea": null, "createdAt": "2026-06-15T05:12:00Z" },
  { "id": 4980, "date": "2026-06-14", "content": "역할놀이에서 의사 역할", "curriculumArea": "SOCIAL", "createdAt": "2026-06-14T03:40:00Z" }
]
```
**에러:** `400`(잘못된 area 값) · `404 CHILD_NOT_FOUND`.

### [9] PATCH `/api/v1/memos/{memoId}/curriculum-area` — 영역 수정 (FR-7, Story 2.3)

자동 분류 결과를 교사가 교정.

**Request**
```json
{ "curriculumArea": "SOCIAL" }
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `curriculumArea` | enum | `@NotNull`, 5영역 중 하나(미분류로 되돌림 비허용) |

**Response 200**: 수정된 memo. **에러:** `400 VALIDATION_FAILED` · `404 MEMO_NOT_FOUND`.

### [10] DELETE `/api/v1/memos/{memoId}` — 메모 삭제(soft)

`deleted_at` 설정. **Response 204**. **에러:** `404 MEMO_NOT_FOUND`.

---

## 5. 보육일지 (Epic 3)

> 분석 엔드포인트는 동기 AI 호출. **서버 하드 타임아웃 ~20s**, 사용자당 **동시 분석 1건**(진행 중 새 요청 → `409 ANALYSIS_IN_PROGRESS`). AI 실패 시에도 원본 메모 보존(FR-4) — 에러 메시지로 보장.

### [11] POST `/api/v1/journals/analyze` — 하루치 일지 초안 생성 (FR-2,3,4)

특정 날짜 반 전체 메모를 묶어 1회 분석. 해당 (teacher,classroom,date) 일지가 없으면 생성, 있으면 거절(`409`) — 재분석은 [15] 사용. 이 패스에서 각 메모 `curriculumArea` 자동 분류(FR-2).

**Request**
```json
{ "classroomId": 10, "date": "2026-06-15" }
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `classroomId` | number | `@NotNull` |
| `date` | date | `@NotNull` |

**Response 201** — `status: DRAFT`.
```json
{
  "id": 9001, "classroomId": 10, "journalDate": "2026-06-15",
  "status": "DRAFT",
  "content": {
    "PHYSICAL_HEALTH": "...", "COMMUNICATION": "...", "SOCIAL": "...",
    "ART": "...", "NATURE": "...",
    "summary": "놀이 흐름·또래 상호작용·교사 지원 포함 서술"
  },
  "analyzedAt": "2026-06-15T11:02:00Z",
  "linkedMemoIds": [5001, 5002, 5003]
}
```
> `content`는 5영역 구조 객체로 직렬화(저장은 MEDIUMTEXT에 JSON 문자열). 정확한 내부 스키마는 AI 연동 명세에서 확정.

**에러:** `400 VALIDATION_FAILED` · `404 CLASSROOM_NOT_FOUND` · `409 JOURNAL_ALREADY_EXISTS`(이미 존재 → 재분석 [15]) · `409 ANALYSIS_IN_PROGRESS` · `502 AI_ANALYSIS_FAILED` · `502 AI_OUTPUT_INVALID` · `504 AI_TIMEOUT`. (AI 계열 에러 메시지에 "메모는 그대로 보존" 고지 포함)

### [12] GET `/api/v1/journals` — 일지 조회 + 재분석 안내 (FR-6)

**Query:** `classroomId`(필수), `date`(필수).

**Response 200** — 일지 없으면 `404 JOURNAL_NOT_FOUND`. 존재 시 재분석 필요 여부 계산(메모.createdAt > journal.analyzedAt).
```json
{
  "id": 9001, "classroomId": 10, "journalDate": "2026-06-15",
  "status": "CONFIRMED", "content": { "...": "..." },
  "analyzedAt": "2026-06-15T11:02:00Z",
  "reanalysisNeeded": true,
  "newMemoIds": [5010, 5011]
}
```
- `reanalysisNeeded`: 분석 이후 추가된 메모 존재 시 `true`. `newMemoIds`로 식별(FR-6).

### [13] GET `/api/v1/journals/{journalId}` — 단건 조회

**Response 200**: [12]와 동일 형태. **에러:** `404 JOURNAL_NOT_FOUND`.

### [14] PUT `/api/v1/journals/{journalId}` — 수정·확정 (FR-5)

교사가 초안을 수정하고 확정. AI 호출 없음(단순 저장).

**Request**
```json
{ "content": { "PHYSICAL_HEALTH": "...", "...": "..." }, "status": "CONFIRMED" }
```
| 필드 | 타입 | 검증 |
|------|------|------|
| `content` | object/string | `@NotNull` |
| `status` | enum | `DRAFT`/`CONFIRMED` |

**Response 200**: 수정된 일지. **에러:** `400 VALIDATION_FAILED` · `404 JOURNAL_NOT_FOUND`.

### [15] POST `/api/v1/journals/{journalId}/analyze` — 재분석·덮어쓰기 (FR-6)

기존 일지를 같은 행에 UPDATE 덮어쓰기. `journal_memo_link` 재구성(기존 삭제 후 재삽입), `analyzedAt` 갱신 → 재분석 안내 자동 해소.

**Request:** 본문 없음(일지 ID로 대상·날짜 결정). 확인 절차는 프론트에서.

**Response 200**: [11]과 동일 형태(갱신된 content·analyzedAt·linkedMemoIds).
**에러:** `404 JOURNAL_NOT_FOUND` · `409 ANALYSIS_IN_PROGRESS` · `502 AI_ANALYSIS_FAILED` · `502 AI_OUTPUT_INVALID` · `504 AI_TIMEOUT`.

---

## 6. 개인 관찰평가 (Epic 4)

### [16] POST `/api/v1/children/{childId}/reports` — 수동 평가 생성 (FR-8)

동기 AI 호출. 분석 기간 자동 계산: 직전 평가(`MANUAL`/`MONTHLY`)의 `period_end` 다음 날 ~ 당일, 이전 평가 없으면 반 `start_date` ~ 당일. `type=MANUAL`로 누적(덮어쓰지 않음, `report_month=null`).

**Request:** 본문 없음(기간 자동). (선택 확장: `{ "periodStart", "periodEnd" }`로 명시 override 허용 가능 — MVP 기본은 자동.)

**Response 201**
```json
{
  "id": 7001, "childId": 101, "reportType": "MANUAL",
  "periodStart": "2026-06-01", "periodEnd": "2026-06-15",
  "reportMonth": null,
  "content": "개인 관찰 평가 서술...",
  "createdAt": "2026-06-15T12:00:00Z"
}
```
**에러:** `404 CHILD_NOT_FOUND` · `409 ANALYSIS_IN_PROGRESS` · `422 REPORT_NO_MEMO`(기간 내 메모 없음) · `502 AI_ANALYSIS_FAILED` · `502 AI_OUTPUT_INVALID` · `504 AI_TIMEOUT`.

### [17] GET `/api/v1/children/{childId}/reports` — 평가 목록 (FR-8)

**Response 200** — `createdAt` 시간순. 수동·자동 모두 포함. 없으면 `[]`.
```json
[
  { "id": 7001, "reportType": "MANUAL",  "periodStart": "2026-06-01", "periodEnd": "2026-06-15", "reportMonth": null,      "createdAt": "2026-06-15T12:00:00Z" },
  { "id": 6800, "reportType": "MONTHLY", "periodStart": "2026-05-01", "periodEnd": "2026-05-31", "reportMonth": "2026-05", "createdAt": "2026-05-31T15:00:00Z" }
]
```
> 목록은 `content` 생략(요약 리스트), 본문은 [18]에서. **에러:** `404 CHILD_NOT_FOUND`.

### [18] GET `/api/v1/reports/{reportId}` — 평가 단건

**Response 200**: content 포함 전체. **에러:** `404 REPORT_NOT_FOUND`.

---

## 6-1. 관찰 온도 (spec-child-warmth)

### [20] GET `/api/v1/classrooms/{classroomId}/warmth` — 아이별 관찰 온도

최근 14일(KST) 메모를 아이별로 반감기 5일 감쇠 집계해 `WARM`/`LOW` 2단계로 돌려준다. AI 호출·별도 테이블 없이 `memo.created_at` + `child` 만으로 계산한다. 담당 교사 본인 전용.

**Response 200**

```json
{
  "enabled": true,
  "windowDays": 14,
  "items": [
    { "childId": 3, "level": "WARM" },
    { "childId": 7, "level": "LOW" }
  ]
}
```

| 필드 | 설명 |
|------|------|
| `enabled` | 판정 가능 여부. `false`면 아직 근거가 모자란 상태(콜드 스타트·대상 아이 없음)이며 `items`는 빈 배열. **프론트는 온도 관련 UI를 통째로 숨긴다** — 안내 문구도 띄우지 않는다 |
| `windowDays` | 판정 창(고정 14) |
| `items[].level` | `WARM` \| `LOW` \| `SNOOZED`. 명단과 같은 가나다순. 온도순 정렬 아님 |
| `items[].snoozedUntil` | `SNOOZED`일 때만 채워지는 접어두기 만료일(KST). 그 외 `null` |

**판정 규칙**

- 점수 = `Σ 0.5^(경과일/5)` — 단순 건수가 아니라 최근성 가중.
- `LOW` = **반 평균 점수의 30% 미만**. 중앙값·상위 N% 같은 상대 순위로 가르지 않는다(그러면 항상 절반이 `LOW`가 된다). **고르게 기록한 반은 `LOW`가 0명인 것이 정상.**
- `LOW` 는 **조건 없이 항상 최대 3명**. 후보가 더 많으면 점수가 가장 낮은 3명만 남긴다(동점은 `childId` 오름차순).
- 등록 3일 미만 아이는 평균 계산에서 제외하고 `WARM` 취급(관찰 기회가 없었으므로).
- 창 내 반 전체 메모 수 < 대상 아이 수 → `enabled:false`.

> **점수·메모 건수는 응답에 싣지 않는다.** 숫자가 나가면 화면에서 등수가 되고, 인지 도구가 성적표로 바뀐다.

**에러:** `404 CLASSROOM_NOT_FOUND`(타 교사 반 포함 — 존재 비노출), `401`(미인증).

### [21] POST `/api/v1/children/{childId}/warmth-snooze` — 잠시 접어두기

결석·입원 등으로 볼 기회가 없던 아이를 온도 판정에서 **14일간** 제외한다. 접힌 아이는 `[20]` 응답에서 `SNOOZED`로 나오고, 평균 계산·`LOW` 후보·콜드 스타트 행 수 어디에도 들어가지 않는다.

**Request**: 본문 없음. **Response 204**.

- 기간은 **고정 14일**(`오늘 + 14`, KST). 사용자 지정·무기한 없음.
- 이미 접힌 아이를 다시 접으면 **오늘 기준으로 갱신**된다(누적 아님).
- 만료일 **당일에는 이미 만료** — 자동으로 판정 대상에 돌아온다.

> 자동 만료가 이 엔드포인트의 안전 속성이다. 무기한 스누즈를 두면 접어두고 잊은 아이가 안전망에서 영구히 빠지고, 월말 개인평가 재료가 없다는 걸 그때 알게 된다 — 온도 기능이 막으려던 사고 그 자체다.

**에러:** `404 CHILD_NOT_FOUND`(타 교사 아이·숨긴 아이 포함), `401`.

### [22] DELETE `/api/v1/children/{childId}/warmth-snooze` — 접어두기 해제

`warmth_snoozed_until`을 비운다. 즉시 판정 대상으로 복귀. **멱등** — 접혀 있지 않아도 `204`.

**에러:** `404 CHILD_NOT_FOUND`, `401`.

---

## 7. 운영 (Epic 1/5)

### [19] GET `/actuator/health` — 헬스체크

Spring Actuator. 인증 불필요(또는 `show-details: when-authorized`). 배포 검증·`scripts/deploy.sh`에서 사용.
**Response 200** `{ "status": "UP" }`.

---

## 8. 인증·인가 적용 매트릭스

| 엔드포인트 | 인증 | 소유권 검증 |
|-----------|------|------------|
| `POST /auth/login` | 불필요 | — |
| `POST /auth/register` | 불필요 | — |
| `GET /actuator/health` | 불필요 | — |
| 그 외 전부 | 필수(Bearer JWT) | repository 레벨 `teacher_id` 강제 |

- JWT 누락/만료 → `401`(`AUTH_UNAUTHENTICATED`/`AUTH_TOKEN_EXPIRED`).
- 타 교사 리소스 → 기본 `404`(존재 비노출).

## 9. 미확정·후속

- ~~**일지 `content` 내부 스키마**와 AI 응답 파싱 타입~~ → ✅ **확정**: `docs/specs/ai-integration-spec.md` §3/§4. 저장형 = `{ summary, PHYSICAL_HEALTH, COMMUNICATION, SOCIAL, ART, NATURE }`(평탄화 5영역 객체), 평가는 `{ summary, areas:[{area,text}] }`.
- **수동 평가 기간 override** 허용 여부(기본 자동) — 제품 결정 후 [16] Request 확정.
- ~~**교사 가입 API** 부재~~ → ✅ **추가됨(Story 1.6, FR-12)**: `POST /api/v1/auth/register`([1b]). dev 시드(`DevDataInitializer`)는 데모용으로 유지.
- `404` vs `403` 정책 — 본 명세는 존재 비노출 우선 `404`. 보안 리뷰 후 일관 적용.
