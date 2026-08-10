# 온도 성능 측정 (bench)

성능 개선을 **숫자로 남기기 위한** 시드 + 측정 도구입니다.
dev 시드는 메모 8건이라 어떤 개선도 "0ms → 0ms" 로 측정됩니다. 여기서 1학기치 데이터를 넣고,
개선 전후를 같은 조건으로 재서 비교표를 만듭니다.

## 쓰는 법

```bash
docker compose up -d                 # 앱·DB·nginx 기동 (dev 프로파일이어야 시드 반이 있음)
bench/seed.sh                        # 1학기치 데이터 주입 (INSERT 만, 기존 행은 안 건드림)
bench/bench.sh before                # 개선 전 측정
# ... 개선 적용 ...
bench/bench.sh after                 # 개선 후 측정
bench/compare.sh before after        # 비교표(마크다운) 출력
```

되돌리기: `bench/seed.sh --reset` — 시드가 넣은 행만 정확히 회수합니다.
현재 데이터량만 확인: `bench/seed.sh --status`

환경변수로 대상 변경 가능: `BASE_URL`, `MYSQL_CONTAINER`, `DB_USER`, `DB_PASS`,
`LOGIN_EMAIL`, `LOGIN_PASSWORD`, `ITER`(반복 횟수, 기본 30), `WARMUP`(예열, 기본 5).

## 파일

| 파일 | 역할 |
|---|---|
| `seed.sql` | 1학기치 데이터 생성 (재귀 CTE, 결정적 = 재현 가능) |
| `reset.sql` | `bench_watermark` 기준으로 시드분만 회수 |
| `seed.sh` | 위 둘의 래퍼 (`--reset`, `--status`) |
| `bench.sh` | 측정 하네스 → `results/<label>.{tsv,md}` |
| `compare.sh` | 두 결과 조인 → 변화율 표 |
| `lib.sh` | 공통 설정·헬퍼 |

`results/` 는 `.gitignore` 되어 있습니다. 포트폴리오에 넣을 표는 `compare.sh` 출력을 따로 저장하세요.

## 시드가 넣는 것

개학일(`classroom.start_date`)부터 오늘까지, 반 하나가 실제로 쌓았을 분량:

| | 양 | 왜 |
|---|---|---|
| `memo` | ≈3,100건 | 아이 23명 × 35~175건. **일부러 불균등** — 3명은 35건만 넣어 관찰 온도가 LOW 를 실제로 판정하게 만든다. 40건에 1건은 soft delete |
| `profile_photo` | 21건 / 5.7MB | 아이당 250~340KB. LONGBLOB 과다 조회를 드러내는 핵심 데이터 |
| `daily_journal` | 평일마다 1건 | content 는 앱과 같은 평탄화 JSON(≈3KB). 목록 API 가 이걸 전부 읽고 파싱한다 |
| `child_report` | 아이 × 월 | MEDIUMTEXT 과다 조회 측정용 |

**안전장치**: `seed.sql` 은 INSERT 만 합니다. 넣기 전 각 테이블 `MAX(id)` 를 `bench_watermark` 에
적어두고, `reset.sql` 이 그 위의 행만 지웁니다. 사진은 복합 PK라 `bench_photo_owner` 에 따로
기록하며, **원래 사진이 있던 소유자는 건드리지 않습니다.**

## 재는 것

| 지표 키 | 뜻 |
|---|---|
| `api.*.p50_ms` / `p95_ms` / `max_ms` | 응답시간 분위수 (예열 후 30회) |
| `api.*.resp_kb` | 클라이언트가 받는 응답 크기 |
| `api.*.db_kb_per_req` | **요청당 MySQL→앱 전송 바이트** |
| `api.*.db_rows_per_req` | 요청당 InnoDB 가 읽은 행 수 |
| `plan.*.key` / `.extra` / `.rows` | EXPLAIN 결과 (memo 행 기준) |
| `avatar.*` | 명단 화면 아바타 일괄 로딩 총비용 |
| `etag.revalidate_*` | `If-None-Match` 재검증 시 상태코드·응답·DB 비용 |
| `asset.*` | 정적 자산 전송 바이트·인코딩·청크 수 |

`db_kb_per_req` 가 이 하네스의 핵심입니다. **응답 크기는 그대로인데 이 값만 크면 과다 조회**
(필요 없는 컬럼까지 DB에서 끌어옴)라는 뜻입니다. `Bytes_sent` 전역 카운터의 델타를 반복 횟수로
나눈 값이라 헬스체크 등 다른 커넥션의 잡음이 조금 섞이지만, 30회로 나누면 무시할 수준입니다.

---

## 기준선 (`before`)

데이터셋: 메모 3,089건 · 아이 23명 · 사진 21건(5.7MB) · 일지 113건 · 평가 116건

| 엔드포인트 | 응답 | **DB→앱/req** | 증폭 |
|---|---:|---:|---:|
| `GET /classrooms/{id}/children` | 2.9KB | **5,678.7KB** | **×1,958** |
| `GET /journals/list` | 57.7KB | 330.7KB | ×5.7 |
| `GET /children/{id}/photo` | 323.2KB | 325.0KB | ×1.0 |
| `GET /classrooms/{id}/warmth` | 1.2KB | 12.7KB | (3,210행 읽음) |
| `GET /children/{id}/timeline` | 67.7KB | 38.1KB | — |
| `GET /children/{id}/reports` | 0.7KB | 14.5KB | ×20 |

**읽는 법**

- **명단 API 가 응답 2.9KB 를 만들려고 DB에서 5.6MB 를 끌어옵니다(약 2,000배).**
  `ProfilePhotoService.updatedAtByOwnerId()` 가 `updatedAt` 하나 쓰려고 `@Lob LONGBLOB` 이 달린
  엔티티를 통째로 로드하기 때문입니다. 프로젝션 쿼리로 바꾸면 사라집니다.
- **관찰 온도는 1.2KB 응답에 3,210행을 읽습니다.** `idx_memo_child` 로 아이별 전체 메모를 훑은 뒤
  `created_at` 을 사후 필터링하기 때문입니다. `(child_id, created_at)` 복합 인덱스면 커버링 인덱스가
  되어 14일 창의 행만 읽습니다.
- **일지 목록은 `summary` 한 줄을 뽑으려고 113건의 `content` MEDIUMTEXT 를 전부 읽고 JSON 파싱합니다.**
- 평가 목록은 응답에서 content 를 빼지만(0.7KB) DB에서는 여전히 읽어옵니다(14.5KB).

| 정적 자산 | 값 |
|---|---|
| `Content-Encoding` | **없음 (gzip 미적용)** |
| JS 청크 수 | **1개** (코드 스플리팅 없음) |
| 초기 전송량 | **282.9KB** |
| gzip 적용 시 | **79.3KB** (−72.0%) |

| EXPLAIN (memo) | key | Extra |
|---|---|---|
| 타임라인 | `idx_memo_child` | `Using where; `**`Using filesort`** |
| 관찰 온도 | `idx_memo_child` | `Using where` (커버링 아님) |
| 일지 묶음 | `idx_memo_child` | `Using where` |

### 측정으로 뒤집힌 가정

진단 단계에서 "사진 GET 이 `If-None-Match` 를 무시하고 항상 200 + 전체 본문을 보낼 것"이라고
봤지만, 재보니 **304 가 정상 반환**됩니다 — Spring MVC 의 `HttpEntityMethodProcessor` 가
`ResponseEntity` 에 붙은 ETag 로 조건부 요청을 자동 처리합니다. 코드에 `checkNotModified` 가
없어도 동작합니다.

다만 절반은 남았습니다: `etag.revalidate_db_kb = 325.0KB` — **본문은 안 나가지만 DB 는 매번
BLOB 전체를 읽습니다.** `PhotoController.toResponse()` 가 ETag 를 만들기 전에
`photoService.find()` 로 엔티티를 통째로 로드하기 때문입니다.

---

## 개선 1 — nginx gzip (`before` → `after`)

`frontend/nginx.conf` 에 `gzip on` + `gzip_proxied any` + `gzip_types`(JS·CSS·JSON) 추가.
nginx 공식 이미지는 gzip 이 꺼져 있어 지금까지 전부 원본으로 나가고 있었습니다.

**정적 자산 — 초기 로딩 282.9KB → 79.6KB (−71.9%)**

| | before | after | |
|---|---:|---:|---|
| JS | 210.8KB | 67.7KB | **−67.9%** |
| CSS | 72.2KB | 11.9KB | **−83.5%** |
| **합계** | **282.9KB** | **79.6KB** | **−71.9%** |
| `Content-Encoding` | 없음 | `gzip` | |

**API 응답 — `gzip_proxied any` 로 프록시된 JSON 까지 압축**

| 엔드포인트 | before | after | |
|---|---:|---:|---|
| `/journals/list` | 57.7KB | 2.4KB | **−95.8%** |
| `/children/{id}/timeline` | 67.7KB | 4.1KB | **−93.9%** |
| `/classrooms/{id}/warmth` | 1.2KB | 0.2KB | **−83.3%** |
| `/classrooms/{id}/children` | 2.9KB | 0.6KB | **−79.3%** |
| `/children/{id}/photo` | 323.2KB | 323.2KB | 변화 없음 (의도) |
| `/children/{id}/reports` | 0.7KB | 0.7KB | 변화 없음 (의도) |

의도한 "변화 없음" 둘이 설정이 정확히 먹었다는 증거입니다 — 사진은 `image/jpeg` 라
`gzip_types` 에서 뺐고(이미 압축된 데이터를 다시 압축하면 CPU 만 씁니다), 평가 목록은
`gzip_min_length 1024` 미만이라 압축 대상이 아닙니다.

**부작용 없음 확인**: `etag.revalidate_status` 는 여전히 `304`, DB 지표(`db_kb_per_req`,
`db_rows_per_req`)와 EXPLAIN 은 전부 그대로입니다. gzip 은 전송 계층만 건드리므로 당연한
결과이고, 이 "변화 없음"이 곧 회귀가 없다는 뜻입니다.

또 `asset.initial_gzipped_kb` 가 79.3KB(`gzip -9` 이론값) → 79.6KB(nginx `comp_level 6` 실측)로
사실상 같습니다. 압축 레벨을 9 까지 올려도 0.4% 밖에 못 줄이니 6 이 맞는 선택이었습니다.

### 응답시간(ms)은 이번 개선의 근거가 아닙니다

비교표에 `api.children.p95 63.1ms → 20.2ms` 같은 큰 폭이 찍히지만 **gzip 의 효과로 해석하면
안 됩니다.** 5.6MB BLOB 을 끌어오는 이 엔드포인트는 실행마다 편차가 커서(같은 조건 재측정에서
14.8ms ~ 63.1ms) 로컬 Docker 의 잡음입니다. 다른 엔드포인트는 ±2ms 안에서 위아래로 흔들립니다.

**이번 개선의 증거는 바이트입니다.** ms 가 의미를 갖는 건 3G·LTE 처럼 대역폭이 병목인
환경이고, 그건 로컬 루프백에서는 측정되지 않습니다.

---

## 주의

- **before/after 는 같은 데이터셋에서 재야 합니다.** 리포트 맨 위 데이터셋 표가 다르면 비교 무효입니다.
- 로컬 Docker 측정이라 절대 수치(ms)는 머신에 좌우됩니다. **비율과 바이트·행 수가 이야기의 본체**입니다.
- 예열 5회는 InnoDB 버퍼풀·JIT 를 데워 첫 요청 편향을 걷어냅니다.
- 하네스는 모든 요청에 `Accept-Encoding: gzip, br` 을 붙입니다(브라우저와 동일). curl 은 헤더를
  직접 준 경우 응답을 풀지 않으므로 `size_download` 가 곧 실제 전송 바이트입니다.
