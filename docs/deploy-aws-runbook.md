# 온도(ondo) AWS 실배포 런북

> 실사용자 오픈 기준. 아동 개인정보를 다루므로 **리전은 서울(`ap-northeast-2`) 고정**, DB는 관리형(RDS)으로 분리한다.
> 이 문서는 위에서 아래로 한 번 훑으면 배포가 끝나도록 구성했다. 각 단계 `☐` 는 완료 시 체크.

## 목표 아키텍처

```
[사용자] → 도메인(Route 53 or 외부 등록기관)
         → EC2 (서울, t3.small)
              └ docker compose
                   ├ caddy   : 443 HTTPS 종료 + Let's Encrypt 자동 (신규)
                   ├ web     : nginx SPA + /api 프록시 (기존)
                   └ app     : Spring Boot :8090 (기존)
         → RDS for MySQL 8.4 (관리형, 자동 백업)   ← compose의 mysql 컨테이너를 대체
```

기존 `docker-compose.yml`(web+app+mysql)에서 **mysql을 RDS로 빼고, 앞단에 HTTPS(caddy)를 얹는 것**이 전부다. 앱 코드 변경은 없다(전부 env 주입).

---

## Phase 0 — 사전 준비 (로컬)

- ☐ **도메인 확보** — 예: `ondo.kr`, `app.ondo.io` 등. 가비아/후이즈/Route 53 어디든 무방. HTTPS·카카오·메일 링크가 전부 이 도메인을 가리킨다.
- ☐ **로컬에서 prod 프로파일로 한 번 떠보기** (배포 전 마지막 점검):
  ```bash
  cp .env.example .env   # 이미 있으면 생략
  # .env 에서 SPRING_PROFILES_ACTIVE=prod, 시크릿 실값으로 교체 후
  SKIP_PULL=1 ./scripts/deploy.sh
  ```
  `✅ 배포 완료` 가 뜨면 서버 이관 준비 끝.

---

## Phase 1 — AWS 계정 안전 세팅 (30분, 실사용 서비스라 생략 금지)

- ☐ 가입 후 **리전을 서울(`ap-northeast-2`)로 고정** — 콘솔 우상단 리전 선택. 이후 모든 리소스는 이 리전에서 생성.
- ☐ **루트 계정 MFA 활성화** 후 봉인 (IAM → 보안 자격 증명). 일상 작업에 루트 사용 금지.
- ☐ **일상용 IAM 사용자 생성** (AdministratorAccess) + 해당 계정에도 MFA.
- ☐ **결제 예산 알람** — Billing → Budgets → 월 예산(예: ₩15,000) 초과 시 이메일. AWS는 이거 없으면 요금이 조용히 샌다.

> 💡 프리티어(12개월): EC2 `t3.micro`, RDS `db.t3.micro` 상당 부분 무료. 다만 t3.micro는 실사용 트래픽엔 빠듯할 수 있어 아래는 t3.small 기준(월 몇 천 원 차이).

---

## Phase 2 — RDS for MySQL 생성

- ☐ RDS → 데이터베이스 생성 → **MySQL 8.4** (compose와 동일 버전)
- ☐ 템플릿: **프리티어** 또는 **개발/테스트**. 인스턴스 `db.t3.micro` (트래픽 늘면 승급)
- ☐ 스토리지: 20GB gp3, **스토리지 자동 확장 켜기**
- ☐ **자동 백업 보존 7일 이상** (실사용 데이터 보호의 핵심 이유)
- ☐ **퍼블릭 액세스 = 아니요** (인터넷 노출 금지, EC2에서만 접근)
- ☐ 자격 증명: 마스터 사용자/비밀번호 설정 → **어딘가 안전히 기록** (아래 `.env`에 들어감)
- ☐ 초기 데이터베이스 이름: `ondo`
- ☐ 생성 후 **엔드포인트 주소 복사** (예: `ondo-db.xxxx.ap-northeast-2.rds.amazonaws.com`)

> 스키마는 걱정 X — 앱 첫 기동 시 **Flyway가 빈 DB에 테이블을 자동 생성**한다(`ddl-auto: validate` + `db/migration`).

---

## Phase 3 — EC2 인스턴스 + 보안 그룹

- ☐ EC2 → 인스턴스 시작
  - AMI: **Ubuntu 22.04 LTS** (또는 Amazon Linux 2023)
  - 타입: **t3.small** (2GB RAM — Gradle 빌드가 도는 초기엔 이 정도 권장. 빌드를 CI로 빼면 t3.micro도 가능)
  - 스토리지: 20GB gp3
  - 키 페어: 새로 생성 → `.pem` 안전 보관 (SSH 접속용)
- ☐ **보안 그룹 (EC2)**:
  | 유형 | 포트 | 소스 |
  |------|------|------|
  | SSH | 22 | **내 IP만** |
  | HTTP | 80 | 0.0.0.0/0 (Let's Encrypt 인증 + HTTP→HTTPS 리다이렉트) |
  | HTTPS | 443 | 0.0.0.0/0 |
- ☐ **보안 그룹 (RDS)**: 인바운드 3306 → **소스를 EC2 보안 그룹으로 지정** (IP가 아니라 SG 참조). 이러면 EC2에서만 DB 접근 가능.
- ☐ **탄력적 IP(Elastic IP) 할당 후 인스턴스에 연결** — 재부팅해도 IP 고정. 도메인 A레코드가 이걸 가리킨다.
- ☐ DNS: 도메인 A레코드 → 탄력적 IP. (`app.ondo.kr → 3.3.3.3`)

---

## Phase 4 — 서버 초기 셋업

SSH 접속 후:

- ☐ Docker + compose 플러그인 설치:
  ```bash
  sudo apt-get update
  sudo apt-get install -y docker.io docker-compose-v2 git
  sudo usermod -aG docker $USER && newgrp docker   # sudo 없이 docker 사용
  ```
- ☐ 코드 클론:
  ```bash
  git clone <저장소 URL> ondo && cd ondo
  ```
- ☐ **`.env` 작성** (핵심). `.env.example` 복사 후 실값으로:
  ```bash
  cp .env.example .env && nano .env
  ```
  실사용 기준 필수 값:
  ```ini
  SPRING_PROFILES_ACTIVE=prod

  # DB — RDS 로 뺐으므로 DB_URL 을 RDS 엔드포인트로 override (아래 Phase 5의 compose 오버라이드에서 사용)
  DB_USERNAME=<RDS 마스터 사용자>
  DB_PASSWORD=<RDS 마스터 비번>

  # JWT — openssl rand -hex 32 로 생성한 강한 값 (dev 기본값 절대 금지)
  JWT_SECRET=<32바이트 이상 랜덤>

  # 카카오 (Phase 6에서 콘솔 설정과 짝 맞춤)
  KAKAO_CLIENT_ID=<REST API 키>
  KAKAO_CLIENT_SECRET=<사용함이면 필수>

  # AI (선택 — 없으면 AI 일지·평가만 비활성, 나머지 정상)
  AI_API_KEY=<OpenAI 키>

  WEB_PORT=80
  APP_PORT=8090
  ```
  > `JWT_SECRET` 생성: `openssl rand -hex 32`

---

## Phase 5 — RDS 연결 + HTTPS (⚠️ 실오픈의 실제 블로커)

기존 `docker-compose.yml`을 건드리지 않고 **override 파일**로 운영 구성을 덧씌운다. `docker-compose.prod.yml` 을 새로 만들어:

```yaml
# docker-compose.prod.yml — 운영 override (RDS 사용 + HTTPS)
#   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
services:
  # mysql 컨테이너 비활성화 — RDS 사용
  mysql:
    profiles: ["disabled"]   # 기본 up 대상에서 제외

  app:
    depends_on: []           # mysql 헬스 의존 제거
    environment:
      # RDS 엔드포인트로 override
      DB_URL: jdbc:mysql://<RDS-엔드포인트>:3306/ondo?serverTimezone=UTC&characterEncoding=UTF-8
      # 비밀번호 재설정 메일 링크가 가리킬 운영 도메인
      APP_BASE_URL: https://<도메인>
    ports: []                # app 직접 노출 차단(디버그 포트 닫기)

  web:
    ports: []                # web 직접 80 노출 차단 — 앞단 caddy만 외부 노출
    expose:
      - "80"

  # HTTPS 종료 + Let's Encrypt 자동 발급/갱신
  caddy:
    image: caddy:2
    restart: unless-stopped
    depends_on: [web]
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - caddy-data:/data
      - caddy-config:/config

volumes:
  caddy-data:
  caddy-config:
```

`Caddyfile` (저장소 루트):
```
<도메인> {
    reverse_proxy web:80
}
```

- ☐ `docker-compose.prod.yml` 과 `Caddyfile` 작성 (위 `<...>` 실값으로)
- ☐ 도메인 A레코드가 이미 EC2 탄력적 IP를 가리키는지 확인 (Caddy가 인증서 받을 때 필요)

> **왜 Caddy?** 단일 EC2 + compose 환경에서 HTTPS를 가장 적은 설정으로 얻는 방법. 도메인만 맞으면 Let's Encrypt 인증서를 자동 발급·자동 갱신한다. ALB(월 ~$16)보다 저렴하고, certbot 수동 갱신보다 손이 덜 간다.
>
> 이 Phase는 문서상 개요다. 실제 파일 작성·검증은 별도 작업(**태스크 B**)으로 같이 하면 안전하다.

---

## Phase 6 — 카카오 / 메일 / 도메인 마무리

- ☐ **카카오 개발자 콘솔**:
  - 플랫폼 → Web → 사이트 도메인에 `https://<도메인>` 등록
  - 카카오 로그인 → **Redirect URI에 운영 콜백 URL 등록** (프론트가 쓰는 경로와 일치해야 함 — 코드의 redirect_uri 확인)
  - `client_secret`을 "사용함"으로 두면 `.env`의 `KAKAO_CLIENT_SECRET` 값 전달 필수 (미전달 시 토큰 교환 401)
- ☐ **메일(비밀번호 재설정)** — 실사용자는 재설정 메일이 실제로 가야 함. `.env`에 SMTP 추가:
  ```ini
  MAIL_HOST=<예: email-smtp.ap-northeast-2.amazonaws.com (SES)>
  MAIL_PORT=587
  MAIL_USERNAME=<SMTP 사용자>
  MAIL_PASSWORD=<SMTP 비번>
  MAIL_FROM=no-reply@<도메인>
  ```
  > 미설정 시 앱은 뜨지만 재설정 메일이 실제 발송되지 않는다(로그만). SES는 초기 샌드박스(수신자 사전 인증 필요) → 프로덕션 액세스 요청 필요.
- ☐ `APP_BASE_URL` = `https://<도메인>` 확인 (Phase 5 override에 이미 반영)

---

## Phase 7 — 배포 실행 & 검증

- ☐ 배포 (override 포함):
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
  ```
  > `scripts/deploy.sh`는 기본 compose만 본다. override를 쓰므로 위 명령을 직접 쓰거나, deploy.sh에 override 인자를 추가하도록 손봐야 한다(태스크 B에서 함께 정리 가능).
- ☐ 상태 확인:
  ```bash
  docker compose -f docker-compose.yml -f docker-compose.prod.yml ps
  docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f app
  ```
- ☐ 검증 체크:
  - `https://<도메인>` 접속 → SPA 로딩, 자물쇠(HTTPS) 정상
  - 회원가입 → 로그인 (JWT 발급)
  - 카카오 로그인 왕복
  - 비밀번호 재설정 메일 수신
  - Flyway 로그에 마이그레이션 적용 확인, `app` 헬스 `UP`

---

## Phase 8 — 운영

- ☐ **DB 백업** — RDS 자동 백업(7일)로 커버. 중요 시점엔 수동 스냅샷.
- ☐ **재배포** (코드 갱신 시):
  ```bash
  cd ~/ondo && git pull
  docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
  ```
- ☐ **롤백** — 직전 커밋으로 `git checkout <sha>` 후 재빌드. DB 스키마 변경이 있었다면 Flyway 되돌림은 별도 고려(down 마이그레이션 없음 → 스냅샷 복구가 안전).
- ☐ **로그 로테이션** — docker json 로그 무한 증가 방지. `/etc/docker/daemon.json`에 `log-opts` 설정 권장.
- ☐ **모니터링(최소)** — CloudWatch에서 EC2 CPU/메모리, RDS 스토리지 알람.

---

## 비용 대략 (서울, 실사용 소규모 기준)

| 항목 | 사양 | 월 예상 |
|------|------|---------|
| EC2 | t3.small | ~₩20,000 (프리티어 t3.micro면 12개월 무료) |
| RDS | db.t3.micro, 20GB | ~₩20,000 (프리티어 커버 상당) |
| 탄력적 IP | 연결 시 무료 | ₩0 |
| 도메인 | .kr/.com | 연 ₩15,000~20,000 |
| **합계** | | **월 ₩0~40,000** (프리티어 활용 폭에 따라) |

> ALB/CloudFront를 쓰면 월 ₩20,000+ 추가. 초기엔 Caddy 단일 박스로 충분.

---

## 오픈 전 필수 체크리스트 (요약)

- ☐ `JWT_SECRET` — dev 기본값 아닌 강한 랜덤
- ☐ HTTPS 적용 (Caddy) — 로그인·개인정보 서비스라 필수
- ☐ RDS 자동 백업 켜짐 + 퍼블릭 액세스 꺼짐
- ☐ RDS 보안그룹 = EC2 SG에서만 3306
- ☐ 카카오 Redirect URI = 운영 도메인
- ☐ `APP_BASE_URL` = `https://<도메인>` (메일 링크)
- ☐ SMTP 설정 (비밀번호 재설정 메일)
- ☐ 결제 예산 알람
- ☐ SSH 22번 = 내 IP만
