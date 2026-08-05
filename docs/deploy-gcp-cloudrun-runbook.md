# 온도(ondo) GCP Cloud Run 실배포 런북

> 실사용자 오픈 기준. 아동 개인정보 → **리전은 서울(`asia-northeast3`) 고정.**
> Cloud Run은 "VM 한 대"가 아니라 **무상태 컨테이너 서비스**라, compose를 그대로 올리는 게 아니라 **서비스별로 분리 배포**한다.
> AWS 방식(단일 VM + compose)은 [deploy-aws-runbook.md](deploy-aws-runbook.md) 참고.

## 목표 아키텍처

```
[사용자] → 커스텀 도메인 (관리형 SSL 자동 — HTTPS 블로커 없음)
   ├─ 프론트: Firebase Hosting (SPA 정적 · 무료 CDN+SSL)
   │       └ /api/** → rewrite → 백엔드 Cloud Run  (같은 origin → CORS 불필요)
   └─ 백엔드: Cloud Run 서비스 "ondo-api" (Spring, asia-northeast3)
           └ Cloud SQL 커넥터 → Cloud SQL for MySQL 8.4 (관리형)
```

**compose 대비 달라지는 점**
- `mysql` 컨테이너 → **Cloud SQL** (Cloud Run은 DB를 못 띄움)
- `web`(nginx) 컨테이너 → **Firebase Hosting** (정적 서빙·gzip·캐시·SSL을 대신 해줌 → nginx 불필요)
- `app` → Cloud Run 서비스 하나. DB는 Cloud SQL 커넥터로 연결
- 서비스 간 통신은 도커 이름(`app:8090`)이 아니라 https URL / Hosting rewrite로

**바뀌는 코드는 사실상 1개** — 백엔드에 Cloud SQL 소켓 팩토리 의존성 추가(Phase 4). 나머지는 전부 배포 설정·env 주입.

---

## Phase 0 — 사전 준비 (로컬)

- ☐ **도메인 확보** (예: `app.ondo.kr`). HTTPS·카카오·메일 링크가 이 도메인을 가리킨다.
- ☐ **gcloud CLI 설치 & 로그인**
  ```bash
  # macOS
  brew install --cask google-cloud-sdk
  gcloud auth login
  gcloud components install beta
  ```
- ☐ **Firebase CLI 설치** (프론트 배포용)
  ```bash
  npm install -g firebase-tools && firebase login
  ```
- ☐ 로컬 prod 프로파일 최종 점검 (`SKIP_PULL=1 ./scripts/deploy.sh` → `✅`)

---

## Phase 1 — GCP 프로젝트 · 결제 · 리전

- ☐ **프로젝트 생성**
  ```bash
  gcloud projects create ondo-prod --name="ondo"
  gcloud config set project ondo-prod
  gcloud config set run/region asia-northeast3   # 서울 고정
  ```
- ☐ **결제 계정 연결** — 신규 가입 시 **$300 크레딧(90일)**. 콘솔 Billing에서 프로젝트에 연결.
- ☐ **예산 알람** — Billing → Budgets → 월 예산(예: ₩20,000) 초과 시 이메일. (크레딧 소진 후 과금 대비)

> 💡 리전 주의: GCP "항상 무료" e2-micro VM은 **미국 리전만** → 한국 아동 데이터엔 부적합. Cloud Run·Cloud SQL 모두 `asia-northeast3`에서 생성한다.

---

## Phase 2 — 필요한 API 활성화

```bash
gcloud services enable \
  run.googleapis.com \
  sqladmin.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com
```

---

## Phase 3 — Cloud SQL for MySQL 생성

- ☐ 인스턴스 생성 (compose와 동일 버전 8.4)
  ```bash
  gcloud sql instances create ondo-db \
    --database-version=MYSQL_8_4 \
    --tier=db-f1-micro \
    --region=asia-northeast3 \
    --storage-size=10GB --storage-auto-increase \
    --backup --backup-start-time=18:00      # 자동 백업(실데이터 보호의 핵심)
  ```
- ☐ DB · 사용자 생성
  ```bash
  gcloud sql databases create ondo --instance=ondo-db
  gcloud sql users create ondo --instance=ondo-db --password='<강한-비번>'
  ```
- ☐ **연결 이름(INSTANCE_CONNECTION_NAME) 확인** (형식 `PROJECT:REGION:INSTANCE`)
  ```bash
  gcloud sql instances describe ondo-db --format='value(connectionName)'
  # 예: ondo-prod:asia-northeast3:ondo-db
  ```

> 스키마는 자동 — 앱 첫 기동 시 **Flyway가 빈 DB에 테이블 생성**(`ddl-auto: validate` + `db/migration`).
> ⚠️ **Cloud SQL은 무료 티어가 없다** (db-f1-micro 월 대략 ₩10,000~). 초기 90일은 $300 크레딧이 커버.

---

## Phase 4 — 백엔드 코드 변경 (유일한 필수 수정)

Cloud Run → Cloud SQL은 **Cloud SQL Java 커넥터(소켓 팩토리)**로 붙는다. `backend/build.gradle` 의존성 추가:

```gradle
dependencies {
    // ... 기존 의존성 ...
    // Cloud SQL for MySQL 커넥터 — jdbc:mysql:/// (호스트 없는) URL + socketFactory 로 연결
    implementation 'com.google.cloud.sql:mysql-socket-factory-connector-j-8:1.23.1'  // 최신 버전 확인
}
```

- ☐ 위 의존성 추가 후 로컬 빌드 확인: `cd backend && ./gradlew bootJar -x test`

> `application.yml`은 손댈 필요 없음 — `DB_URL`이 env로 주입되므로 Phase 6에서 Cloud SQL용 URL을 넘긴다.
> 포트도 코드 변경 불필요 — Cloud Run 배포 시 `--port=8090`으로 앱의 기존 리스닝 포트를 지정한다.

---

## Phase 5 — 시크릿 (Secret Manager)

`.env` 대신 Secret Manager에 저장하고 Cloud Run에 마운트한다.

```bash
# JWT (openssl rand -hex 32 로 생성한 값)
printf '%s' '<32바이트-랜덤>' | gcloud secrets create jwt-secret --data-file=-
# DB 비밀번호
printf '%s' '<ondo-DB-비번>'   | gcloud secrets create db-password --data-file=-
# 카카오 client_secret (사용함이면)
printf '%s' '<카카오-시크릿>'   | gcloud secrets create kakao-secret --data-file=-
# OpenAI 키 (선택)
printf '%s' '<OpenAI-키>'       | gcloud secrets create ai-key --data-file=-
```

- ☐ 위 시크릿 생성. (값 갱신은 `gcloud secrets versions add <이름> --data-file=-`)

---

## Phase 6 — 백엔드 Cloud Run 배포

`backend/`에 이미 Dockerfile이 있으므로 `--source`로 Cloud Build가 알아서 빌드한다. 아래에서 `<CONN>` = Phase 3의 연결 이름.

```bash
gcloud run deploy ondo-api \
  --source ./backend \
  --region asia-northeast3 \
  --port 8090 \
  --memory 1Gi --cpu 1 \
  --min-instances 0 \
  --add-cloudsql-instances '<CONN>' \
  --set-env-vars '^@^SPRING_PROFILES_ACTIVE=prod@AI_MODEL=gpt-5.4-mini@DB_USERNAME=ondo@DB_URL=jdbc:mysql:///ondo?cloudSqlInstance=<CONN>&socketFactory=com.google.cloud.sql.mysql.SocketFactory&serverTimezone=UTC&characterEncoding=UTF-8@APP_BASE_URL=https://<도메인>@KAKAO_CLIENT_ID=<카카오-REST키>' \
  --set-secrets 'JWT_SECRET=jwt-secret:latest,DB_PASSWORD=db-password:latest,KAKAO_CLIENT_SECRET=kakao-secret:latest,AI_API_KEY=ai-key:latest' \
  --allow-unauthenticated
```

포인트:
- `--set-env-vars '^@^...'` — DB_URL 안에 `&`가 있어 **구분자를 `@`로 바꿔** 넣는다(콤마 파싱 회피).
- `--add-cloudsql-instances` — Cloud Run이 Cloud SQL 접근 경로를 붙여준다. 서비스 계정에 **`roles/cloudsql.client`** 필요:
  ```bash
  PROJ_NUM=$(gcloud projects describe ondo-prod --format='value(projectNumber)')
  gcloud projects add-iam-policy-binding ondo-prod \
    --member="serviceAccount:${PROJ_NUM}-compute@developer.gserviceaccount.com" \
    --role="roles/cloudsql.client"
  ```
- `--min-instances 0` = scale-to-zero(무료↑, 대신 콜드스타트 수 초). 항상 빠릿하게 하려면 `1`(소액 과금).
- 배포 완료 시 출력되는 **서비스 URL**(`https://ondo-api-xxxx.a.run.app`) 기록.

- ☐ 헬스 확인: `curl https://ondo-api-xxxx.a.run.app/actuator/health` → `{"status":"UP"}`
- ☐ 로그에서 Flyway 마이그레이션 적용 확인:
  ```bash
  gcloud run services logs read ondo-api --region asia-northeast3 --limit 100
  ```

---

## Phase 7 — 프론트 배포 (Firebase Hosting)

브라우저는 프론트하고만 통신하고, `/api/**`는 Hosting이 백엔드 Cloud Run으로 rewrite → **같은 origin이라 CORS 불필요**(기존 상대경로 `/api` 그대로 동작).

저장소 루트에 `firebase.json`:
```json
{
  "hosting": {
    "public": "frontend/dist",
    "ignore": ["firebase.json", "**/.*", "**/node_modules/**"],
    "rewrites": [
      { "source": "/api/**", "run": { "serviceId": "ondo-api", "region": "asia-northeast3" } },
      { "source": "**", "destination": "/index.html" }
    ]
  }
}
```

- ☐ Firebase 프로젝트를 GCP 프로젝트에 연결
  ```bash
  firebase use --add    # ondo-prod 선택
  ```
- ☐ 프론트 빌드 & 배포
  ```bash
  cd frontend && npm ci && npm run build && cd ..
  firebase deploy --only hosting
  ```
- ☐ 발급된 Hosting URL(`https://ondo-prod.web.app`)로 접속 확인.

> ⚠️ **리전 캐비엇**: Firebase Hosting의 Cloud Run rewrite가 `asia-northeast3`를 지원하는지 배포 시 확인. 미지원이면 대안 ↓.
>
> **대안(프론트도 Cloud Run)**: `frontend/`에도 Dockerfile+nginx가 있으므로, `nginx.conf`의 `proxy_pass http://app:8090;`을 백엔드 서비스 URL(`https://ondo-api-xxxx.a.run.app`)로 바꿔 두 번째 Cloud Run 서비스로 배포. 이러면 리전 제약 없이 nginx 프록시 구조를 그대로 유지한다(단, 프록시 대상 주입을 위해 entrypoint에서 envsubst 처리 필요).

---

## Phase 8 — 카카오 · 메일 · 도메인

- ☐ **커스텀 도메인 연결** — Firebase Hosting → 도메인 추가(관리형 SSL 자동 발급). DNS는 안내되는 레코드로 설정.
- ☐ **카카오 개발자 콘솔**
  - 사이트 도메인: `https://<도메인>`
  - **Redirect URI에 운영 콜백 URL 등록** (프론트가 쓰는 redirect_uri와 일치)
  - `client_secret` "사용함"이면 `kakao-secret`(Phase 5) 값 필수 — 미전달 시 토큰 교환 401
- ☐ **메일(비밀번호 재설정)** — 실사용자는 실제 발송 필요. SMTP 제공자(SendGrid/Mailgun/AWS SES 등 무엇이든) 값을 Cloud Run env로 추가:
  ```bash
  gcloud run services update ondo-api --region asia-northeast3 \
    --update-env-vars 'MAIL_HOST=<smtp호스트>,MAIL_PORT=587,MAIL_USERNAME=<유저>,MAIL_FROM=no-reply@<도메인>' \
    --update-secrets 'MAIL_PASSWORD=mail-password:latest'
  ```
  > 미설정 시 앱은 뜨지만 재설정 메일이 실제로 안 감(로그만).
- ☐ `APP_BASE_URL`이 `https://<도메인>`인지 확인(Phase 6에 반영). 도메인 확정 후 바뀌면 `--update-env-vars`로 갱신.

---

## Phase 9 — 검증

- ☐ `https://<도메인>` 접속 → SPA 로딩, HTTPS 자물쇠 정상
- ☐ 회원가입 → 로그인(JWT 발급)
- ☐ 카카오 로그인 왕복
- ☐ 비밀번호 재설정 메일 수신
- ☐ AI 일지·평가 동작(AI_API_KEY 설정 시)
- ☐ `ondo-api` 헬스 `UP`, Flyway 마이그레이션 로그 확인

---

## Phase 10 — 운영

- ☐ **재배포(백엔드)** — 코드 갱신 후:
  ```bash
  gcloud run deploy ondo-api --source ./backend --region asia-northeast3
  ```
  (env·시크릿·Cloud SQL 설정은 유지됨. 롤백은 콘솔에서 이전 리비전으로 트래픽 전환 — 즉시.)
- ☐ **재배포(프론트)** — `cd frontend && npm run build && cd .. && firebase deploy --only hosting`
- ☐ **DB 백업** — Cloud SQL 자동 백업으로 커버. 중요 시점엔 `gcloud sql backups create --instance=ondo-db`.
- ☐ **롤백** — 백엔드는 리비전 전환(무중단). DB 스키마 변경분은 down 마이그레이션 없음 → 스냅샷 복구가 안전.
- ☐ **로그·모니터링** — Cloud Logging(자동), Cloud Monitoring에서 Cloud SQL 스토리지/CPU 알람.

---

## 비용 대략 (서울, 실사용 소규모)

| 항목 | 사양 | 월 예상 |
|------|------|---------|
| Cloud Run (백엔드) | scale-to-zero, 소규모 | ~₩0 (월 200만 요청 무료 티어) |
| Cloud SQL | db-f1-micro, 10GB | **~₩10,000~15,000** (무료 티어 없음 — 핵심 비용) |
| Firebase Hosting | 정적+CDN | ~₩0 (무료 티어) |
| 도메인 | .kr/.com | 연 ₩15,000~20,000 |
| **합계** | | **월 ~₩10,000~15,000** (첫 90일은 $300 크레딧으로 ₩0) |

> `--min-instances 1`(콜드스타트 제거) 선택 시 Cloud Run에 소액 추가.

---

## 오픈 전 필수 체크리스트 (요약)

- ☐ `JWT_SECRET` — dev 기본값 아닌 강한 랜덤(Secret Manager)
- ☐ Cloud SQL 자동 백업 켜짐
- ☐ 서비스 계정에 `roles/cloudsql.client` 부여
- ☐ 백엔드 `--add-cloudsql-instances` + DB_URL 소켓팩토리 형식
- ☐ HTTPS — Cloud Run/Hosting 자동(별도 작업 없음)
- ☐ 카카오 Redirect URI = 운영 도메인
- ☐ `APP_BASE_URL` = `https://<도메인>` (메일 링크)
- ☐ SMTP 설정 (비밀번호 재설정 메일)
- ☐ 결제 예산 알람
- ☐ 커스텀 도메인 SSL 발급 확인
