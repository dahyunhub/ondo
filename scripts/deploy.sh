#!/usr/bin/env bash
# 온도 한 줄 배포(Story 5.2): pull → build → up -d → 헬스체크.
# 헬스 실패 시 비정상 종료(배포 실패를 호출자에게 알림). prod 프로파일 권장.
#
#   ./scripts/deploy.sh            # 현재 브랜치로 배포
#   SKIP_PULL=1 ./scripts/deploy.sh   # git pull 건너뛰기(로컬 변경분 그대로 배포)
set -euo pipefail

cd "$(dirname "$0")/.."   # 저장소 루트

# compose 와 동일한 포트/시크릿을 보도록 .env 를 셸에도 로드(헬스체크 URL·프로파일 일치).
if [ -f .env ]; then set -a; . ./.env; set +a; fi

# docker compose v2(plugin) / v1(legacy) 모두 지원
if docker compose version >/dev/null 2>&1; then COMPOSE="docker compose"; else COMPOSE="docker-compose"; fi

# .env 확인 — prod 는 시크릿 외부주입 필수(공개 기본값 운영 방지). dev 는 경고만.
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"
if [ ! -f .env ]; then
  if [ "$PROFILE" = "prod" ]; then
    echo "❌ prod 배포에는 .env 가 필요합니다(JWT_SECRET·DB 비밀번호 등 시크릿 외부주입)."
    echo "   cp .env.example .env 후 강한 시크릿으로 채워 주세요. (dev 데모는 SPRING_PROFILES_ACTIVE=dev)"
    exit 1
  fi
  echo "⚠️  .env 가 없습니다(dev 기본값으로 진행). 운영은 .env 로 시크릿을 주입하세요."
fi

echo "▶ 1/4 최신 코드 pull"
if [ "${SKIP_PULL:-0}" = "1" ]; then
  echo "  (SKIP_PULL=1 — 건너뜀)"
else
  git pull --ff-only || {
    echo "❌ git pull 실패(로컬 변경 또는 분기 불일치). 현재 트리로 배포하려면 SKIP_PULL=1 로 실행하세요."
    exit 1
  }
fi

echo "▶ 2/4 이미지 build"
$COMPOSE build

echo "▶ 3/4 컨테이너 기동(up -d)"
$COMPOSE up -d

# 헬스체크: app 컨테이너의 health 가 healthy 가 될 때까지 폴링.
echo "▶ 4/4 헬스체크"
APP_URL="http://localhost:${APP_PORT:-8090}/actuator/health"
WEB_URL="http://localhost:${WEB_PORT:-80}/"
DEADLINE=$(( $(date +%s) + 180 ))   # 최대 3분(빌드 후 첫 기동 여유)

until curl -fsS "$APP_URL" >/dev/null 2>&1; do
  if [ "$(date +%s)" -ge "$DEADLINE" ]; then
    echo "❌ 백엔드 헬스체크 실패($APP_URL). 최근 로그:"
    $COMPOSE logs --tail=40 app || true
    exit 1
  fi
  echo "  …대기 중(app)"; sleep 3
done
echo "  ✓ app UP"

DEADLINE=$(( $(date +%s) + 180 ))   # web 용 예산 재설정(app 대기에 잠식되지 않도록)
until curl -fsS "$WEB_URL" >/dev/null 2>&1; do
  if [ "$(date +%s)" -ge "$DEADLINE" ]; then
    echo "❌ 프론트(web) 헬스체크 실패($WEB_URL). 최근 로그:"
    $COMPOSE logs --tail=40 web || true
    exit 1
  fi
  echo "  …대기 중(web)"; sleep 2
done
echo "  ✓ web UP"

echo "✅ 배포 완료 — 접속: $WEB_URL"
