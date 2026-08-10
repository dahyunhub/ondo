#!/usr/bin/env bash
# 두 측정 결과를 나란히 놓고 변화율을 낸다 — 이력서·포트폴리오에 붙일 표를 그대로 만든다.
#
#   bench/compare.sh before after
#   bench/compare.sh before after --md > docs/portfolio/성능개선.md
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

BEFORE="${1:-}"; AFTER="${2:-}"
[ -n "$BEFORE" ] && [ -n "$AFTER" ] || die "사용법: compare.sh <before-label> <after-label> [--md]"

A="$BENCH_DIR/results/$BEFORE.tsv"
B="$BENCH_DIR/results/$AFTER.tsv"
[ -f "$A" ] || die "없는 결과: $A"
[ -f "$B" ] || die "없는 결과: $B"

# 값이 낮을수록 좋은 지표(시간·바이트·행수)만 변화율을 계산한다.
# key / extra 같은 문자열 지표는 그대로 나란히 보여준다.
join -t $'\t' -a1 -a2 -e '-' -o '0,1.2,1.3,2.2' \
     <(sort -t $'\t' -k1,1 "$A") <(sort -t $'\t' -k1,1 "$B") \
| awk -F'\t' -v md="${3:-}" '
BEGIN {
  print "| 지표 | before | after | 변화 |"
  print "|---|---:|---:|---|"
}
{
  key=$1; b=$2; unit=$3; a=$4
  if (unit == "-") unit = ""          # join -e 가 채운 빈 단위
  if (b ~ /^-?[0-9.]+$/ && a ~ /^-?[0-9.]+$/ && b+0 != 0) {
    pct = (a - b) / b * 100
    if (pct <= -0.5)      delta = sprintf("**%.1f%%** ↓", -pct)
    else if (pct >= 0.5)  delta = sprintf("%.1f%% ↑", pct)
    else                  delta = "변화 없음"
  } else if (b == a) {
    delta = "변화 없음"
  } else {
    delta = "**변경됨**"
  }
  printf "| `%s` | %s%s | %s%s | %s |\n", key, b, unit, a, unit, delta
}'
