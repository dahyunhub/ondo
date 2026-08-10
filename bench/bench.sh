#!/usr/bin/env bash
# ============================================================================
#  온도 성능 측정 하네스
#
#    bench/bench.sh before      개선 전 측정 → bench/results/before.{tsv,md}
#    bench/bench.sh after       개선 후 측정 → bench/results/after.{tsv,md}
#    bench/compare.sh before after
#
#  재는 것 (앞선 진단에서 나온 5개 지점에 각각 대응):
#    1. 정적 자산 전송 바이트 + Content-Encoding   → nginx gzip 미적용
#    2. 초기 JS 청크 개수·크기                     → 라우터 코드 스플리팅
#    3. MySQL→앱 전송 바이트 (요청당)              → 프로필 사진 LONGBLOB 과다 조회
#    4. EXPLAIN key / Extra                        → memo 복합 인덱스
#    5. 응답 바이트 · 304 응답 여부                 → 페이지네이션 / ETag 재검증
#
#  주의: 측정 전 bench/seed.sh 로 현실적인 데이터량을 넣어야 의미가 있습니다.
# ============================================================================
set -euo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

LABEL="${1:-$(date +%Y%m%d-%H%M%S)}"
ITER="${ITER:-30}"        # 엔드포인트당 반복 횟수
WARMUP="${WARMUP:-5}"     # JIT·버퍼풀 예열 (측정에서 제외)

RESULTS_DIR="$BENCH_DIR/results"
TSV="$RESULTS_DIR/$LABEL.tsv"
MD="$RESULTS_DIR/$LABEL.md"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT

mkdir -p "$RESULTS_DIR"
: > "$TSV"

# metric 기록: 키 / 값 / 단위 — compare.sh 가 이 TSV 를 조인한다.
emit() { printf '%s\t%s\t%s\n' "$1" "$2" "${3:-}" >> "$TSV"; }

require_stack
info "로그인 중…"
TOKEN="$(login_token)"
AUTH="Authorization: Bearer $TOKEN"
# 브라우저는 예외 없이 압축을 요청한다. 이 헤더가 없으면 gzip 적용 여부가 측정에 아예 안 잡힌다.
# curl 은 헤더를 직접 준 경우 응답을 풀지 않으므로 size_download = 실제 전송 바이트가 된다.
ACCEPT_ENC="Accept-Encoding: gzip, br"

# ---------------------------------------------------------------------------
# 대상 선정 — 시드가 만든 반, 그리고 메모가 가장 많은 아이(타임라인 최악 케이스)
# ---------------------------------------------------------------------------
CID="$(mysql_scalar "SELECT id FROM classroom ORDER BY id LIMIT 1")"
[ -n "$CID" ] || die "반이 없습니다. 앱을 dev 프로파일로 띄워 시드를 만든 뒤 bench/seed.sh 를 실행하세요."
CHILD="$(mysql_scalar "
  SELECT m.child_id FROM memo m
    JOIN child c ON c.id = m.child_id AND c.classroom_id = $CID AND c.deleted_at IS NULL
   WHERE m.deleted_at IS NULL
   GROUP BY m.child_id ORDER BY COUNT(*) DESC LIMIT 1")"
[ -n "$CHILD" ] || die "메모가 있는 아이가 없습니다. bench/seed.sh 를 먼저 실행하세요."

info "대상: classroom=$CID, child=$CHILD (메모 최다), 반복=$ITER회"

# ---------------------------------------------------------------------------
# 0. 데이터셋 규모 — 숫자의 전제. 이게 다르면 before/after 를 비교하면 안 된다.
# ---------------------------------------------------------------------------
read -r D_MEMO D_CHILD D_PHOTO D_PHOTOMB D_JOURNAL D_REPORT <<EOF
$(mysql_scalar "
  SELECT (SELECT COUNT(*) FROM memo WHERE deleted_at IS NULL),
         (SELECT COUNT(*) FROM child WHERE classroom_id=$CID AND deleted_at IS NULL),
         (SELECT COUNT(*) FROM profile_photo),
         (SELECT ROUND(COALESCE(SUM(LENGTH(data)),0)/1024/1024,1) FROM profile_photo),
         (SELECT COUNT(*) FROM daily_journal),
         (SELECT COUNT(*) FROM child_report)")
EOF
emit dataset.memo "$D_MEMO" 건
emit dataset.child "$D_CHILD" 명
emit dataset.photo "$D_PHOTO" 건
emit dataset.photo_mb "$D_PHOTOMB" MB
emit dataset.journal "$D_JOURNAL" 건
emit dataset.report "$D_REPORT" 건

# ---------------------------------------------------------------------------
# 1. EXPLAIN — 인덱스가 실제로 쓰이는지, 정렬을 인덱스로 해결하는지
#    JPA 가 만드는 SQL 모양 그대로 재현한다(@SQLRestriction 의 deleted_at 조건 포함).
# ---------------------------------------------------------------------------
# EXPLAIN 12개 컬럼: id select_type table partitions type possible_keys key key_len ref rows filtered Extra
#                     $1   $2          $3    $4         $5   $6           $7  $8      $9  $10  $11      $12
# 조인 쿼리는 여러 행이 나오므로 관심 대상인 memo 행만 고른다(child 행은 무시).
explain_row() { # $1=라벨  $2=SQL
  local out key extra rows
  out="$(mysql_run -N -B -e "EXPLAIN $2" | awk -F'\t' '$3=="memo" || $3=="m"' | head -1)"
  [ -n "$out" ] || { emit "plan.$1.key" "(memo 행 없음)"; return; }
  key="$(printf '%s' "$out" | awk -F'\t' '{print $7}')"
  extra="$(printf '%s' "$out" | awk -F'\t' '{print $12}')"
  rows="$(printf '%s' "$out" | awk -F'\t' '{print $10}')"
  [ -n "$key" ] && [ "$key" != "NULL" ] || key="(none)"
  emit "plan.$1.key" "$key"
  emit "plan.$1.extra" "${extra:-(none)}"
  emit "plan.$1.rows" "$rows" 행
}

WIN_START="$(mysql_scalar "SELECT DATE_SUB(UTC_TIMESTAMP(), INTERVAL 14 DAY)")"
explain_row timeline "SELECT * FROM memo WHERE child_id=$CHILD AND deleted_at IS NULL ORDER BY created_at DESC"
explain_row warmth "SELECT m.child_id, m.created_at FROM memo m
   WHERE m.child_id IN (SELECT c.id FROM child c WHERE c.classroom_id=$CID AND c.deleted_at IS NULL)
     AND m.created_at >= '$WIN_START' AND m.created_at < UTC_TIMESTAMP() AND m.deleted_at IS NULL"
explain_row bundle "SELECT * FROM memo m
   WHERE m.child_id IN (SELECT c.id FROM child c WHERE c.classroom_id=$CID AND c.deleted_at IS NULL)
     AND m.created_at >= '$WIN_START' AND m.created_at < UTC_TIMESTAMP() AND m.deleted_at IS NULL
   ORDER BY m.id ASC"

# ---------------------------------------------------------------------------
# 2. API 측정 — 응답시간 p50/p95/max, 응답 바이트, 그리고 요청당 MySQL→앱 전송 바이트
#
#    Bytes_sent 는 MySQL 이 클라이언트로 내보낸 총 바이트다. 앱이 필요 없는 컬럼까지
#    끌어오면(LONGBLOB 등) 응답 크기는 그대로인데 이 값만 폭증한다 — 과다 조회의 직접 증거.
#    (헬스체크 등 다른 커넥션의 잡음이 섞이지만 반복 횟수로 나누면 무시할 수준이다.)
# ---------------------------------------------------------------------------
# SHOW STATUS 의 행 순서에 기대지 않고 이름으로 골라낸다.
status_pair() { mysql_run -N -B -e \
  "SHOW GLOBAL STATUS WHERE Variable_name IN ('Bytes_sent','Innodb_rows_read')" \
  | awk '$1=="Bytes_sent"{b=$2} $1=="Innodb_rows_read"{r=$2} END{print b+0, r+0}'; }

pct() { sort -n "$1" | awk -v p="$2" '{v[c++]=$1} END{ if(c==0){print "0.0";exit}
        i=int((p/100)*(c-1)+0.5); printf "%.1f", v[i]*1000 }'; }

measure() { # $1=키  $2=경로
  local key="$1" path="$2" i code bytes t
  local times="$TMP/$key.times"; : > "$times"

  for ((i=0; i<WARMUP; i++)); do
    curl -sS -o /dev/null -H "$AUTH" -H "$ACCEPT_ENC" "$BASE_URL$path" >/dev/null || true
  done

  read -r b0 r0 <<< "$(status_pair)"
  bytes=0
  for ((i=0; i<ITER; i++)); do
    read -r t code bytes <<< "$(curl -sS -o /dev/null -H "$AUTH" -H "$ACCEPT_ENC" \
      -w '%{time_total} %{http_code} %{size_download}' "$BASE_URL$path")"
    printf '%s\n' "$t" >> "$times"
  done
  read -r b1 r1 <<< "$(status_pair)"

  if [ "$code" != "200" ]; then
    info "  $key → HTTP $code (측정에서 제외)"
    emit "api.$key.http" "$code"
    return
  fi

  emit "api.$key.p50_ms" "$(pct "$times" 50)" ms
  emit "api.$key.p95_ms" "$(pct "$times" 95)" ms
  emit "api.$key.max_ms" "$(pct "$times" 100)" ms
  emit "api.$key.resp_kb" "$(awk -v b="$bytes" 'BEGIN{printf "%.1f", b/1024}')" KB
  emit "api.$key.db_kb_per_req" \
       "$(awk -v a="$b0" -v b="$b1" -v n="$ITER" 'BEGIN{printf "%.1f", (b-a)/n/1024}')" KB
  emit "api.$key.db_rows_per_req" \
       "$(awk -v a="$r0" -v b="$r1" -v n="$ITER" 'BEGIN{printf "%.0f", (b-a)/n}')" 행

  printf '  %-10s p50=%sms p95=%sms  응답=%sKB  DB→앱=%sKB/req\n' \
    "$key" "$(pct "$times" 50)" "$(pct "$times" 95)" \
    "$(awk -v b="$bytes" 'BEGIN{printf "%.1f", b/1024}')" \
    "$(awk -v a="$b0" -v b="$b1" -v n="$ITER" 'BEGIN{printf "%.1f", (b-a)/n/1024}')" >&2
}

info "API 측정 중…"
measure children "/api/v1/classrooms/$CID/children"
measure warmth   "/api/v1/classrooms/$CID/warmth"
measure timeline "/api/v1/children/$CHILD/timeline"
measure journals "/api/v1/journals/list?classroomId=$CID"
measure reports  "/api/v1/children/$CHILD/reports"
measure photo    "/api/v1/children/$CHILD/photo"

# ---------------------------------------------------------------------------
# 3. 아바타 로딩 총비용 — 명단 화면 첫 진입 시 아바타는 아이마다 별도 요청이다.
#    (Avatar.vue 가 인증 fetch → objectURL 로 받으므로 N명 = N요청)
# ---------------------------------------------------------------------------
info "아바타 일괄 로딩 측정 중…"
PHOTO_IDS="$(mysql_scalar "SELECT owner_id FROM profile_photo WHERE owner_kind='CHILD'")"
if [ -n "$PHOTO_IDS" ]; then
  # 경과시간은 curl 이 준 time_total 을 합산한다 — BSD date 에는 %N(나노초)이 없다.
  n=0; total=0; secs=0
  for id in $PHOTO_IDS; do
    read -r t sz <<< "$(curl -sS -o /dev/null -H "$AUTH" -H "$ACCEPT_ENC" \
      -w '%{time_total} %{size_download}' "$BASE_URL/api/v1/children/$id/photo")"
    total=$((total + sz)); n=$((n + 1))
    secs=$(awk -v a="$secs" -v b="$t" 'BEGIN{printf "%.6f", a+b}')
  done
  emit avatar.requests "$n" 회
  emit avatar.total_kb "$(awk -v b="$total" 'BEGIN{printf "%.1f", b/1024}')" KB
  emit avatar.total_ms "$(awk -v s="$secs" 'BEGIN{printf "%.0f", s*1000}')" ms

  # ETag 재검증 — If-None-Match 를 보냈을 때 (a) 304 로 본문을 아끼는지,
  # (b) 그때 DB 는 여전히 BLOB 을 읽는지. 네트워크는 아껴도 DB 는 그대로일 수 있다.
  ETAG=$(curl -sS -o /dev/null -D - -H "$AUTH" "$BASE_URL/api/v1/children/$CHILD/photo" \
         | awk 'tolower($1)=="etag:"{print $2}' | tr -d '\r')
  if [ -n "$ETAG" ]; then
    read -r eb0 er0 <<< "$(status_pair)"
    for ((i=0; i<ITER; i++)); do
      read -r rc rb <<< "$(curl -sS -o /dev/null -H "$AUTH" -H "If-None-Match: $ETAG" \
        -w '%{http_code} %{size_download}' "$BASE_URL/api/v1/children/$CHILD/photo")"
    done
    read -r eb1 er1 <<< "$(status_pair)"
    emit etag.revalidate_status "$rc"
    emit etag.revalidate_resp_kb "$(awk -v b="$rb" 'BEGIN{printf "%.1f", b/1024}')" KB
    emit etag.revalidate_db_kb \
         "$(awk -v a="$eb0" -v b="$eb1" -v n="$ITER" 'BEGIN{printf "%.1f", (b-a)/n/1024}')" KB
  fi
else
  info "  사진이 없습니다 — bench/seed.sh 를 먼저 실행하세요."
fi

# ---------------------------------------------------------------------------
# 4. 프론트 정적 자산 — 실제로 나가는 바이트(gzip 적용 여부 포함)와 압축 여지
# ---------------------------------------------------------------------------
# index.html 이 직접 참조하는 것 = 첫 화면에서 반드시 받아야 하는 자산.
# 파일명 해시는 빌드마다 바뀌므로 지표 키는 파일별이 아니라 합계로만 남긴다(before/after 조인용).
info "정적 자산 측정 중…"
ASSETS="$(curl -sS "$BASE_URL/" | grep -oE '/assets/[A-Za-z0-9._-]+\.(js|css)' | sort -u)"
js_n=0; js_sent=0; css_sent=0; gz_total=0; enc="none"
: > "$TMP/assets.txt"
for a in $ASSETS; do
  hdr="$(curl -sS -o "$TMP/asset.bin" -D - -H 'Accept-Encoding: gzip, br' "$BASE_URL$a")"
  sent=$(wc -c < "$TMP/asset.bin" | tr -d ' ')
  e=$(printf '%s' "$hdr" | awk 'tolower($1)=="content-encoding:"{print $2}' | tr -d '\r')
  if [ -n "$e" ]; then
    enc="$e"
    gz=$sent          # 이미 압축돼 나온 바이트 — 그 자체가 최종 전송량
  else
    gz=$(gzip -9 -c "$TMP/asset.bin" | wc -c | tr -d ' ')   # 압축했다면 줄었을 크기
  fi
  gz_total=$((gz_total + gz))
  case "$a" in
    *.js)  js_n=$((js_n+1)); js_sent=$((js_sent+sent)) ;;
    *.css) css_sent=$((css_sent+sent)) ;;
  esac
  printf '%s\t%s\t%s\n' "$(basename "$a")" "$sent" "$gz" >> "$TMP/assets.txt"
done
emit asset.encoding "$enc"
emit asset.js_chunks "$js_n" 개
emit asset.js_sent_kb "$(awk -v b="$js_sent" 'BEGIN{printf "%.1f", b/1024}')" KB
emit asset.css_sent_kb "$(awk -v b="$css_sent" 'BEGIN{printf "%.1f", b/1024}')" KB
emit asset.initial_sent_kb \
     "$(awk -v j="$js_sent" -v c="$css_sent" 'BEGIN{printf "%.1f", (j+c)/1024}')" KB
emit asset.initial_gzipped_kb \
     "$(awk -v b="$gz_total" 'BEGIN{printf "%.1f", b/1024}')" KB

# ---------------------------------------------------------------------------
# 리포트
# ---------------------------------------------------------------------------
{
  echo "# 온도 성능 측정 — \`$LABEL\`"
  echo
  echo "- 측정 시각: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "- 대상: classroom=\`$CID\`, child=\`$CHILD\`, 반복 ${ITER}회(예열 ${WARMUP}회 제외)"
  echo "- 엔드포인트: \`$BASE_URL\`"
  echo
  echo '## 데이터셋'
  echo
  echo "| 메모 | 아이 | 사진 | 사진 용량 | 일지 | 평가 |"
  echo "|---:|---:|---:|---:|---:|---:|"
  echo "| $D_MEMO | $D_CHILD | $D_PHOTO | ${D_PHOTOMB}MB | $D_JOURNAL | $D_REPORT |"
  echo
  echo '## 정적 자산 (파일별)'
  echo
  echo "실제 전송 인코딩: \`$enc\`"
  echo
  echo '| 파일 | 전송 바이트 | 압축 시 |'
  echo '|---|---:|---:|'
  awk -F'\t' '{printf "| `%s` | %.1fKB | %.1fKB |\n", $1, $2/1024, $3/1024}' "$TMP/assets.txt"
  echo
  echo '## 전체 지표'
  echo
  echo '| 지표 | 값 | 단위 |'
  echo '|---|---:|---|'
  awk -F'\t' '{printf "| `%s` | %s | %s |\n", $1, $2, $3}' "$TSV"
} > "$MD"

ok "완료 → $TSV"
ok "        $MD"
echo >&2
info "개선 후 다시 재고 비교: bench/bench.sh <라벨> && bench/compare.sh $LABEL <라벨>"
