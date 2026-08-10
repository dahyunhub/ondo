#!/usr/bin/env bash
# 성능 측정용 시드 주입/회수.
#
#   bench/seed.sh            1학기치 데이터 주입 (INSERT 만 — 기존 행은 건드리지 않음)
#   bench/seed.sh --reset    주입한 것만 정확히 회수 (bench_watermark 기준)
#   bench/seed.sh --status   현재 데이터량만 출력
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_stack

case "${1:-}" in
  --reset)
    info "bench 시드 회수 중…"
    mysql_run < "$BENCH_DIR/reset.sql"
    ok "회수 완료 — 위 숫자가 시드 전 상태입니다."
    ;;
  --status)
    mysql_run -e "
      SELECT (SELECT COUNT(*) FROM memo) memo,
             (SELECT COUNT(*) FROM child WHERE deleted_at IS NULL) child,
             (SELECT COUNT(*) FROM profile_photo) photo,
             (SELECT ROUND(COALESCE(SUM(LENGTH(data)),0)/1024/1024,1) FROM profile_photo) photo_mb,
             (SELECT COUNT(*) FROM daily_journal) journal,
             (SELECT COUNT(*) FROM child_report) report;"
    ;;
  "")
    info "1학기치 데이터 주입 중… (수천 건이라 10~30초 걸립니다)"
    mysql_run < "$BENCH_DIR/seed.sql"
    ok "시드 완료. 이제 bench/bench.sh 로 측정하세요."
    ;;
  *)
    die "알 수 없는 옵션: $1  (사용법: seed.sh [--reset|--status])"
    ;;
esac
