-- ============================================================================
-- 온도 리텐션·활성화 분석 쿼리 모음 (새 로그 인프라 없이 기존 스키마로 산출)
-- ----------------------------------------------------------------------------
-- 대상 DBMS : MySQL 8.x
-- 정본 스키마: backend/src/main/resources/db/migration/V1__init.sql
--
-- 공통 규칙
--  * created_at 은 UTC 저장(serverTimezone=UTC). 한국 교사 기준 "며칠"을 세기 위해
--    KST(+09:00)로 변환한다 — CONVERT_TZ 에 숫자 오프셋을 쓰므로 mysql.time_zone
--    테이블 적재가 없어도 동작한다.
--  * 데모 계정(demo@ondo.app)은 지표에서 제외한다. 없으면 필터는 무동작.
--  * "활동(active)" = 메모 작성 OR AI 일지 생성 (= 실제로 도구를 사용한 행위).
--    행위 자체를 세므로 이후 soft-delete(memo.deleted_at) 여부는 활동 판정에 넣지 않는다.
--
-- 실행법
--   로컬(dev):  mysql -h127.0.0.1 -P3307 -uondo -pondo ondo < analytics/retention.sql
--   운영(Cloud SQL): cloud-sql-proxy 로 로컬 포워딩 후 동일하게 실행하거나
--                    gcloud sql connect <INSTANCE> --user=<USER> --database=ondo 로 접속해 붙여넣기.
-- ============================================================================


-- ────────────────────────────────────────────────────────────────────────
-- Q1. 가입 → 활성화 퍼널 (누적 카운트)
--   가입한 교사가 어디까지 도달했는지. generated_journal(AI 일지 1회 이상)이
--   "핵심 가치 경험 = 진짜 활성화" 지점이다.
-- ────────────────────────────────────────────────────────────────────────
SELECT
  COUNT(*)                                                                        AS signups,
  SUM(EXISTS(SELECT 1 FROM classroom c WHERE c.teacher_id = t.id))                AS made_classroom,
  SUM(EXISTS(SELECT 1 FROM child ch JOIN classroom c ON ch.classroom_id = c.id
             WHERE c.teacher_id = t.id AND ch.deleted_at IS NULL))                AS added_child,
  SUM(EXISTS(SELECT 1 FROM memo m WHERE m.teacher_id = t.id))                     AS wrote_memo,
  SUM(EXISTS(SELECT 1 FROM daily_journal j WHERE j.teacher_id = t.id))            AS generated_journal
FROM teacher t
WHERE t.email <> 'demo@ondo.app';


-- ────────────────────────────────────────────────────────────────────────
-- Q2. 습관 형성 — "두 번째" 지표 (★ 이 앱의 진짜 성공 신호)
--   활성 교사 중 서로 다른 2일 이상 활동한 비율, 그리고 AI 일지 2건 이상 생성 비율.
--   "한 번 써보고 끝"이 아니라 "돌아와서 또 썼는가"의 프록시.
-- ────────────────────────────────────────────────────────────────────────
WITH activity AS (
  SELECT m.teacher_id, DATE(CONVERT_TZ(m.created_at, '+00:00', '+09:00')) AS d
    FROM memo m JOIN teacher t ON t.id = m.teacher_id WHERE t.email <> 'demo@ondo.app'
  UNION
  SELECT j.teacher_id, DATE(CONVERT_TZ(j.created_at, '+00:00', '+09:00')) AS d
    FROM daily_journal j JOIN teacher t ON t.id = j.teacher_id WHERE t.email <> 'demo@ondo.app'
),
per_teacher AS (
  SELECT a.teacher_id,
         COUNT(DISTINCT a.d) AS active_days,
         (SELECT COUNT(*) FROM daily_journal j WHERE j.teacher_id = a.teacher_id) AS journals
  FROM activity a
  GROUP BY a.teacher_id
)
SELECT
  COUNT(*)                                              AS active_teachers,
  SUM(active_days >= 2)                                 AS returned_2plus_days,
  ROUND(100 * AVG(active_days >= 2), 1)                 AS pct_returned_2plus_days,
  SUM(journals >= 2)                                    AS made_2plus_journals,
  ROUND(100 * AVG(journals >= 2), 1)                    AS pct_2plus_journals
FROM per_teacher;


-- ────────────────────────────────────────────────────────────────────────
-- Q3. 가입 주차별 코호트 리텐션 (주 = 월요일 시작, KST)
--   가입한 주(cohort_week) 기준으로 W0~W3 에 다시 활동한 교사 수.
--   w1_retained 가 진짜 리텐션 신호(가입 다음 주에도 돌아왔는가).
--   cohort_size 는 그 주 전체 가입자(활동 없어도 포함).
-- ────────────────────────────────────────────────────────────────────────
WITH t AS (
  SELECT id AS teacher_id, DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) AS signup_d
  FROM teacher WHERE email <> 'demo@ondo.app'
),
cohorts AS (
  SELECT teacher_id, signup_d,
         DATE_SUB(signup_d, INTERVAL WEEKDAY(signup_d) DAY) AS cohort_week
  FROM t
),
activity AS (
  SELECT teacher_id, DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) AS d FROM memo
  UNION
  SELECT teacher_id, DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) AS d FROM daily_journal
),
act AS (
  SELECT c.cohort_week, c.teacher_id, FLOOR(DATEDIFF(a.d, c.signup_d) / 7) AS week_no
  FROM cohorts c
  JOIN activity a ON a.teacher_id = c.teacher_id AND a.d >= c.signup_d
)
SELECT
  c.cohort_week,
  COUNT(DISTINCT c.teacher_id)                                       AS cohort_size,
  COUNT(DISTINCT CASE WHEN a.week_no = 0 THEN a.teacher_id END)      AS w0_active,
  COUNT(DISTINCT CASE WHEN a.week_no = 1 THEN a.teacher_id END)      AS w1_retained,
  COUNT(DISTINCT CASE WHEN a.week_no = 2 THEN a.teacher_id END)      AS w2_retained,
  COUNT(DISTINCT CASE WHEN a.week_no = 3 THEN a.teacher_id END)      AS w3_retained
FROM cohorts c
LEFT JOIN act a ON a.teacher_id = c.teacher_id
GROUP BY c.cohort_week
ORDER BY c.cohort_week;


-- ────────────────────────────────────────────────────────────────────────
-- Q4. 교사별 활동 요약 + 이탈 상태 (개별 사용자 기록)
--   active(≤7일 내 활동) / at_risk(8~30일) / churned(30일 초과) / never_active.
--   피드백 요청·리텐션 넛지 대상 고르는 실무용 목록.
-- ────────────────────────────────────────────────────────────────────────
WITH activity AS (
  SELECT teacher_id, DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) AS d FROM memo
  UNION
  SELECT teacher_id, DATE(CONVERT_TZ(created_at, '+00:00', '+09:00')) AS d FROM daily_journal
),
per AS (
  SELECT teacher_id, COUNT(DISTINCT d) AS active_days, MAX(d) AS last_active
  FROM activity GROUP BY teacher_id
)
SELECT
  t.id,
  t.email,
  t.name,
  DATE(CONVERT_TZ(t.created_at, '+00:00', '+09:00'))                AS signup,
  COALESCE(p.active_days, 0)                                        AS active_days,
  (SELECT COUNT(*) FROM memo m WHERE m.teacher_id = t.id)           AS memos,
  (SELECT COUNT(*) FROM daily_journal j WHERE j.teacher_id = t.id)  AS journals,
  p.last_active,
  DATEDIFF(CURDATE(), p.last_active)                                AS days_since_active,
  CASE
    WHEN p.last_active IS NULL                          THEN 'never_active'
    WHEN DATEDIFF(CURDATE(), p.last_active) <= 7        THEN 'active'
    WHEN DATEDIFF(CURDATE(), p.last_active) <= 30       THEN 'at_risk'
    ELSE 'churned'
  END                                                              AS state
FROM teacher t
LEFT JOIN per p ON p.teacher_id = t.id
WHERE t.email <> 'demo@ondo.app'
ORDER BY p.last_active DESC;


-- ────────────────────────────────────────────────────────────────────────
-- Q5. 주간 AI 일지 생성 추세 (참여 볼륨)
--   주별 생성 건수 + 그 주에 일지를 만든 교사 수(≈ WAU 프록시).
-- ────────────────────────────────────────────────────────────────────────
SELECT
  DATE_SUB(DATE(CONVERT_TZ(j.created_at, '+00:00', '+09:00')),
           INTERVAL WEEKDAY(DATE(CONVERT_TZ(j.created_at, '+00:00', '+09:00'))) DAY) AS week,
  COUNT(*)                        AS journals,
  COUNT(DISTINCT j.teacher_id)    AS active_teachers
FROM daily_journal j
JOIN teacher t ON t.id = j.teacher_id
WHERE t.email <> 'demo@ondo.app'
GROUP BY week
ORDER BY week;
