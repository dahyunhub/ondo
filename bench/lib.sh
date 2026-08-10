#!/usr/bin/env bash
# 온도 성능 측정 공통 설정·헬퍼. seed.sh / bench.sh 가 source 한다.
# 값은 전부 환경변수로 덮어쓸 수 있다 (예: BASE_URL=http://localhost:8090 bench/bench.sh).

: "${MYSQL_CONTAINER:=ondo-mysql-1}"
: "${DB_NAME:=ondo}"
: "${DB_USER:=ondo}"
: "${DB_PASS:=ondo}"
# 기본은 nginx(80) — 프론트 정적 + /api 프록시가 같은 origin 이라 실사용 경로와 같다.
: "${BASE_URL:=http://localhost}"
: "${LOGIN_EMAIL:=teacher@ondo.dev}"
: "${LOGIN_PASSWORD:=password1234}"

BENCH_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(dirname "$BENCH_DIR")"

die() { printf '\033[31m✗ %s\033[0m\n' "$*" >&2; exit 1; }
info() { printf '\033[36m▸ %s\033[0m\n' "$*" >&2; }
ok() { printf '\033[32m✓ %s\033[0m\n' "$*" >&2; }

# MYSQL_PWD 로 넘겨 "Using a password on the command line" 경고를 없앤다.
mysql_run() {
  docker exec -i -e MYSQL_PWD="$DB_PASS" "$MYSQL_CONTAINER" \
    mysql -u"$DB_USER" --default-character-set=utf8mb4 "$DB_NAME" "$@"
}

# 한 줄짜리 스칼라 조회 (헤더 없이 값만).
mysql_scalar() { mysql_run -N -B -e "$1"; }

require_stack() {
  command -v docker >/dev/null || die "docker 가 필요합니다."
  command -v curl >/dev/null || die "curl 이 필요합니다."
  command -v jq >/dev/null || die "jq 가 필요합니다 (brew install jq)."
  docker ps --format '{{.Names}}' | grep -qx "$MYSQL_CONTAINER" \
    || die "MySQL 컨테이너($MYSQL_CONTAINER)가 떠 있지 않습니다. docker compose up -d 먼저."
}

# 로그인해서 JWT 를 얻는다. 실패하면 즉시 중단(측정이 전부 401 이 되는 걸 막는다).
login_token() {
  local res
  res=$(curl -sS -X POST "$BASE_URL/api/v1/auth/login" \
        -H 'Content-Type: application/json' \
        -d "{\"email\":\"$LOGIN_EMAIL\",\"password\":\"$LOGIN_PASSWORD\"}") \
    || die "로그인 요청 실패 — BASE_URL($BASE_URL) 확인."
  local token
  token=$(printf '%s' "$res" | jq -r '.accessToken // empty')
  [ -n "$token" ] || die "로그인 실패: $res"
  printf '%s' "$token"
}
