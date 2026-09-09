---
title: 온도 — 에러코드 카탈로그 (Error Code Catalog)
status: draft
created: 2026-06-15
sources:
  - _bmad-output/planning-artifacts/architecture.md (API & Communication Patterns, Process Patterns)
  - _bmad-output/planning-artifacts/prds/prd-ondo-2026-06-15/prd.md (FR-4 자산 무손실)
related:
  - docs/specs/api-spec.md
  - docs/specs/data-model-spec.md
---

# 에러코드 카탈로그 — 온도

전역 예외 처리(`@RestControllerAdvice`)에서 사용하는 표준 에러 응답·코드 정의다. 모든 도메인/AI 예외는 `ErrorCode`로 매핑되어 **단일 지점**에서 HTTP 응답으로 변환된다(architecture: 예외 경계).

## 1. 표준 에러 응답 포맷

모든 에러는 동일 구조로 반환한다(architecture Format Patterns).

```json
{
  "timestamp": "2026-06-15T11:02:00Z",
  "status": 502,
  "code": "AI_ANALYSIS_FAILED",
  "message": "일지 생성에 실패했어요. 작성하신 메모는 그대로 저장돼 있어요.",
  "path": "/api/v1/journals/analyze"
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `timestamp` | string(ISO-8601 UTC) | 응답 생성 시각 |
| `status` | number | HTTP 상태코드(`code`의 매핑값) |
| `code` | string | 안정적 머신리더블 코드(아래 enum) |
| `message` | string | 사용자 노출 한글 메시지(기본값, 컨텍스트로 보강 가능) |
| `path` | string | 요청 경로 |

**검증 실패(`VALIDATION_FAILED`)** 는 필드별 사유를 더한다(선택 확장):
```json
{
  "timestamp": "...", "status": 400, "code": "VALIDATION_FAILED",
  "message": "입력값을 확인해 주세요.", "path": "/api/v1/memos",
  "errors": [ { "field": "childId", "reason": "필수입니다" } ]
}
```

## 2. 설계 규약

- 도메인 예외는 `BusinessException`(보유: `ErrorCode`) 상속 → advice에서 `errorCode.status`/`code`/`message`로 변환.
- AI 관련 예외는 `AiAnalysisException`(BusinessException 계열).
- 비식별화 복원 실패는 `DeidentificationException`(BusinessException 계열).
- **로그 규약:** 에러 로그에 아이 실명·민감정보 금지(NFR-1). `ERROR`=장애, `WARN`=복구가능, `INFO`=주요 이벤트.
- **자산 무손실(FR-4):** 모든 AI 계열 코드의 사용자 메시지는 "원본 메모 보존" 사실을 포함한다.

## 3. ErrorCode 카탈로그

### 3.1 인증·인가 (AUTH)

| code | HTTP | 기본 message | 발생 지점 |
|------|------|-------------|-----------|
| `AUTH_INVALID_CREDENTIALS` | 401 | 이메일 또는 비밀번호가 올바르지 않아요. | 로그인 검증 실패 |
| `AUTH_UNAUTHENTICATED` | 401 | 로그인이 필요해요. | JWT 누락/무효 |
| `AUTH_TOKEN_EXPIRED` | 401 | 로그인이 만료됐어요. 다시 로그인해 주세요. | JWT 만료(리프레시 없음) |
| `AUTH_TOKEN_REVOKED` | 401 | 비밀번호가 변경되어 다시 로그인이 필요해요. | 비밀번호 변경·재설정 시각보다 먼저 발급된 JWT |
| `TOO_MANY_ATTEMPTS` | 429 | 시도가 너무 많아요. 잠시 후 다시 시도해 주세요. | 로그인·현재 비밀번호 확인의 시도 제한(브루트포스 가드). 재설정 메일 요청은 계정 열거 방지를 위해 이 코드를 쓰지 않고 발송만 조용히 멈춘다 |
| `AUTH_FORBIDDEN` | 403 | 접근 권한이 없어요. | 명백한 소유권 위반 |

### 3.2 검증 (VALIDATION)

| code | HTTP | 기본 message | 발생 지점 |
|------|------|-------------|-----------|
| `VALIDATION_FAILED` | 400 | 입력값을 확인해 주세요. | Bean Validation 위반(`@Valid`) |
| `MEMO_EMPTY` | 400 | 내용 또는 항목 중 하나 이상을 입력해 주세요. | FR-1: content/3항목 모두 비어 있음 |
| `INVALID_CURRICULUM_AREA` | 400 | 누리과정 영역 값이 올바르지 않아요. | 영역 enum 외 값 |
| `RESET_TOKEN_INVALID` | 400 | 링크가 만료되었거나 이미 사용된 링크예요… | 재설정 토큰 만료·재사용·위조 (**세 경우를 구분하지 않는다**) |

### 3.3 리소스 부재 (NOT_FOUND)

| code | HTTP | 기본 message | 발생 지점 |
|------|------|-------------|-----------|
| `TEACHER_NOT_FOUND` | 404 | 교사를 찾을 수 없어요. | 내부 조회 실패 |
| `CLASSROOM_NOT_FOUND` | 404 | 반을 찾을 수 없어요. | 미존재 또는 타 교사 소유(비노출) |
| `CHILD_NOT_FOUND` | 404 | 아이를 찾을 수 없어요. | 미존재/삭제/타 교사 소유 |
| `MEMO_NOT_FOUND` | 404 | 메모를 찾을 수 없어요. | 미존재/삭제/타 교사 소유 |
| `JOURNAL_NOT_FOUND` | 404 | 보육일지를 찾을 수 없어요. | 해당 날짜 일지 없음 |
| `REPORT_NOT_FOUND` | 404 | 평가를 찾을 수 없어요. | 미존재/타 교사 소유 |

### 3.4 충돌·상태 (CONFLICT / STATE)

| code | HTTP | 기본 message | 발생 지점 |
|------|------|-------------|-----------|
| `ANALYSIS_IN_PROGRESS` | 409 | 이미 분석이 진행 중이에요. 잠시 후 다시 시도해 주세요. | NFR-4: 사용자당 동시 분석 1건 가드 |
| `JOURNAL_ALREADY_EXISTS` | 409 | 그 날짜의 보육일지가 이미 있어요. 재분석을 이용해 주세요. | FR-3 유니크 위반(신규 생성 시도) |
| `DATA_CONFLICT` | 409 | 요청이 현재 상태와 충돌해요. 잠시 후 다시 시도해 주세요. | DB 제약 위반 폴백(예: token_alias 동시 등록 경쟁 → `DataIntegrityViolationException`) |
| `REPORT_NO_MEMO` | 422 | 해당 기간에 작성된 메모가 없어요. | FR-8 수동평가: 기간 내 메모 0건 |
| `JOURNAL_NO_MEMO` | 422 | 그 날짜에 작성된 메모가 없어요. | FR-3 일지 분석: 해당 날짜 메모 0건 |

### 3.5 AI 분석 (AI) — 모든 메시지에 "메모 보존" 포함 (FR-4)

| code | HTTP | 기본 message | 발생 지점 |
|------|------|-------------|-----------|
| `AI_ANALYSIS_FAILED` | 502 | 분석에 실패했어요. 작성하신 메모는 그대로 저장돼 있어요. 잠시 후 다시 시도해 주세요. | 외부 LLM 호출 실패(연결/5xx, 재시도 소진) |
| `AI_TIMEOUT` | 504 | 분석이 시간 내에 끝나지 않았어요. 작성하신 메모는 그대로 저장돼 있어요. | 서버 하드 타임아웃(~20s) 초과 |
| `AI_OUTPUT_INVALID` | 502 | 분석 결과를 확인하지 못했어요. 작성하신 메모는 그대로 저장돼 있어요. | OutputValidator 검증 실패(1회 재요청 후에도 누락) |
| `DEIDENTIFICATION_RESTORE_FAILED` | 500 | 결과 처리 중 문제가 생겼어요. 작성하신 메모는 그대로 저장돼 있어요. | 비식별화 복원 실패(원문에 `[[CHILD_n]]` 잔존) |

### 3.6 일반 (GENERIC)

| code | HTTP | 기본 message | 발생 지점 |
|------|------|-------------|-----------|
| `INTERNAL_ERROR` | 500 | 일시적인 문제가 발생했어요. 잠시 후 다시 시도해 주세요. | 매핑되지 않은 예외(최종 폴백) |
| `METHOD_NOT_ALLOWED` | 405 | 허용되지 않은 요청이에요. | 잘못된 HTTP 메서드 |

## 4. 참고 Java enum 스케치 (`com.ondo.common.exception.ErrorCode`)

> 구현 시작점. 실제 코드는 패키지 규약(architecture)에 맞춰 작성.

```java
public enum ErrorCode {
    // AUTH
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않아요."),
    AUTH_UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "로그인이 필요해요."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료됐어요. 다시 로그인해 주세요."),
    AUTH_TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "비밀번호가 변경되어 다시 로그인이 필요해요."),
    TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "시도가 너무 많아요. 잠시 후 다시 시도해 주세요."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없어요."),

    // VALIDATION
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 확인해 주세요."),
    MEMO_EMPTY(HttpStatus.BAD_REQUEST, "내용 또는 항목 중 하나 이상을 입력해 주세요."),
    INVALID_CURRICULUM_AREA(HttpStatus.BAD_REQUEST, "누리과정 영역 값이 올바르지 않아요."),
    RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "링크가 만료되었거나 이미 사용된 링크예요. 비밀번호 재설정을 다시 요청해 주세요."),

    // NOT_FOUND
    TEACHER_NOT_FOUND(HttpStatus.NOT_FOUND, "교사를 찾을 수 없어요."),
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "반을 찾을 수 없어요."),
    CHILD_NOT_FOUND(HttpStatus.NOT_FOUND, "아이를 찾을 수 없어요."),
    MEMO_NOT_FOUND(HttpStatus.NOT_FOUND, "메모를 찾을 수 없어요."),
    JOURNAL_NOT_FOUND(HttpStatus.NOT_FOUND, "보육일지를 찾을 수 없어요."),
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "평가를 찾을 수 없어요."),

    // CONFLICT / STATE
    ANALYSIS_IN_PROGRESS(HttpStatus.CONFLICT, "이미 분석이 진행 중이에요. 잠시 후 다시 시도해 주세요."),
    JOURNAL_ALREADY_EXISTS(HttpStatus.CONFLICT, "그 날짜의 보육일지가 이미 있어요. 재분석을 이용해 주세요."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "요청이 현재 상태와 충돌해요. 잠시 후 다시 시도해 주세요."),
    REPORT_NO_MEMO(HttpStatus.UNPROCESSABLE_ENTITY, "해당 기간에 작성된 메모가 없어요."),
    JOURNAL_NO_MEMO(HttpStatus.UNPROCESSABLE_ENTITY, "그 날짜에 작성된 메모가 없어요."),

    // AI (메모 보존 고지 포함)
    AI_ANALYSIS_FAILED(HttpStatus.BAD_GATEWAY, "분석에 실패했어요. 작성하신 메모는 그대로 저장돼 있어요. 잠시 후 다시 시도해 주세요."),
    AI_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "분석이 시간 내에 끝나지 않았어요. 작성하신 메모는 그대로 저장돼 있어요."),
    AI_OUTPUT_INVALID(HttpStatus.BAD_GATEWAY, "분석 결과를 확인하지 못했어요. 작성하신 메모는 그대로 저장돼 있어요."),
    DEIDENTIFICATION_RESTORE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "결과 처리 중 문제가 생겼어요. 작성하신 메모는 그대로 저장돼 있어요."),

    // GENERIC
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 요청이에요."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "일시적인 문제가 발생했어요. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }
    public HttpStatus getStatus() { return status; }
    public String getDefaultMessage() { return defaultMessage; }
}
```

## 5. 엔드포인트 → 에러코드 매핑 요약

| 엔드포인트 | 가능한 에러코드 |
|-----------|----------------|
| `POST /auth/login` | `VALIDATION_FAILED`, `AUTH_INVALID_CREDENTIALS` |
| `GET /classrooms` | `AUTH_UNAUTHENTICATED` |
| `GET/POST /classrooms/{id}/children` | `VALIDATION_FAILED`, `CLASSROOM_NOT_FOUND` |
| `PUT/DELETE /children/{id}` | `VALIDATION_FAILED`, `CHILD_NOT_FOUND` |
| `POST /memos` | `VALIDATION_FAILED`, `MEMO_EMPTY`, `CHILD_NOT_FOUND` |
| `GET /children/{id}/timeline` | `INVALID_CURRICULUM_AREA`, `CHILD_NOT_FOUND` |
| `PATCH /memos/{id}/curriculum-area` | `VALIDATION_FAILED`, `INVALID_CURRICULUM_AREA`, `MEMO_NOT_FOUND` |
| `DELETE /memos/{id}` | `MEMO_NOT_FOUND` |
| `POST /journals/analyze` | `VALIDATION_FAILED`, `CLASSROOM_NOT_FOUND`, `JOURNAL_NO_MEMO`, `JOURNAL_ALREADY_EXISTS`, `ANALYSIS_IN_PROGRESS`, `AI_*` |
| `GET /journals`, `GET /journals/{id}` | `JOURNAL_NOT_FOUND` |
| `PUT /journals/{id}` | `VALIDATION_FAILED`, `JOURNAL_NOT_FOUND` |
| `POST /journals/{id}/analyze` | `JOURNAL_NOT_FOUND`, `ANALYSIS_IN_PROGRESS`, `AI_*` |
| `POST /children/{id}/reports` | `CHILD_NOT_FOUND`, `ANALYSIS_IN_PROGRESS`, `REPORT_NO_MEMO`, `AI_*` |
| `GET /children/{id}/reports` | `CHILD_NOT_FOUND` |
| `GET /reports/{id}` | `REPORT_NOT_FOUND` |

> 모든 인증 필수 엔드포인트는 위에 더해 `AUTH_UNAUTHENTICATED`/`AUTH_TOKEN_EXPIRED`/`AUTH_TOKEN_REVOKED`/`AUTH_FORBIDDEN`이 공통 가능.
