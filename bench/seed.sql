-- ============================================================================
--  온도 성능 측정용 시드 (bench/seed.sql)
--
--  목적: 성능 개선의 before/after 를 숫자로 남기려면 현실적인 데이터량이 필요하다.
--        dev 시드는 아이 23명 · 메모 8건이라 어떤 개선도 "0ms → 0ms" 로 측정된다.
--
--  넣는 것 (1학기치 ≈ 반 하나가 3월 개학 후 오늘까지 쌓았을 분량):
--    - memo          약 3,000건  (아이 23명 × 35~175건, 최근일수록 촘촘)
--    - profile_photo 20건 × ≈300KB (LONGBLOB 과다 조회 측정용)
--    - daily_journal 평일 1건씩  (무제한 목록 + content 파싱 비용 측정용)
--    - child_report  아이당 월별 (무제한 목록 + MEDIUMTEXT 과다 조회 측정용)
--
--  안전:
--    - INSERT 만 한다. 기존 행을 지우거나 바꾸지 않는다.
--    - 넣기 전 각 테이블의 MAX(id) 를 bench_watermark 에 기록한다.
--      → bench/reset.sql 이 "워터마크 위의 행" 만 정확히 되돌린다.
--    - 여러 번 실행해도 워터마크는 최초 1회만 기록되므로 reset 이 전부 회수한다.
--
--  실행: bench/seed.sh  (직접 실행 시 docker exec ... mysql < bench/seed.sql)
-- ============================================================================

SET SESSION cte_max_recursion_depth = 1000000;

-- ---------------------------------------------------------------------------
-- 대상: 가장 먼저 만들어진 반(= dev 시드의 '만 4세반'). 반이 없으면 아래 INSERT 들이 전부 0행이 된다.
-- created_at 은 앱이 UTC 로 쓰므로(AppTime) 여기서도 UTC 기준으로 만든다.
-- ---------------------------------------------------------------------------
SET @cid   := (SELECT id FROM classroom ORDER BY id LIMIT 1);
SET @tid   := (SELECT teacher_id FROM classroom WHERE id = @cid);
SET @start := (SELECT CAST(start_date AS DATETIME) FROM classroom WHERE id = @cid);
SET @now   := UTC_TIMESTAMP();
-- 개학일이 미래이거나 오늘이면 최소 한 학기(120일)는 확보해 뒤로 밀어 만든다.
SET @span  := GREATEST(TIMESTAMPDIFF(MINUTE, @start, @now), 120 * 24 * 60);
SET @start := DATE_SUB(@now, INTERVAL @span MINUTE);

-- ---------------------------------------------------------------------------
-- 워터마크 — reset 이 "bench 가 넣은 것만" 지울 수 있게 하는 기준선.
-- INSERT IGNORE 라 최초 1회만 기록된다(재시드해도 기준선은 그대로).
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bench_watermark (
    id             TINYINT     NOT NULL PRIMARY KEY,
    memo_max_id    BIGINT      NOT NULL,
    journal_max_id BIGINT      NOT NULL,
    report_max_id  BIGINT      NOT NULL,
    seeded_at      DATETIME(6) NOT NULL
) ENGINE=InnoDB;

-- 사진은 복합 PK(owner_kind, owner_id)라 id 워터마크가 없다 → 넣은 소유자를 따로 적어둔다.
CREATE TABLE IF NOT EXISTS bench_photo_owner (
    owner_kind VARCHAR(10) NOT NULL,
    owner_id   BIGINT      NOT NULL,
    PRIMARY KEY (owner_kind, owner_id)
) ENGINE=InnoDB;

INSERT IGNORE INTO bench_watermark (id, memo_max_id, journal_max_id, report_max_id, seeded_at)
SELECT 1,
       COALESCE((SELECT MAX(id) FROM memo), 0),
       COALESCE((SELECT MAX(id) FROM daily_journal), 0),
       COALESCE((SELECT MAX(id) FROM child_report), 0),
       @now;

-- ---------------------------------------------------------------------------
-- 1) memo — 약 3,000건
--
--  분포를 일부러 고르지 않게 만든다:
--    - rn <= 3 인 아이는 35건만  → 관찰 온도에서 LOW 로 판정되는 소수(기능이 실제로 동작하는 상태)
--    - 나머지는 130~174건        → WARM
--  시각은 (n * 9973 + rn * 7919) mod @span 분 으로 흩는다. 9973·7919 는 소수라
--  전 구간에 고르게 퍼지면서도 아이마다 다른 패턴이 나온다(결정적 = 재현 가능).
-- ---------------------------------------------------------------------------
INSERT INTO memo (child_id, teacher_id, content, play_activity, interaction, attitude,
                  curriculum_area, created_at, updated_at, deleted_at)
WITH RECURSIVE
    seq (n) AS (
        SELECT 1
        UNION ALL
        SELECT n + 1 FROM seq WHERE n < 175
    ),
    kids AS (
        SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
        FROM child
        WHERE classroom_id = @cid AND deleted_at IS NULL
    )
SELECT
    k.id,
    @tid,
    ELT(1 + (s.n * 7 + k.rn) % 8,
        '블록으로 높은 탑을 쌓다가 무너지자 다시 아래를 넓게 만들어 보겠다고 했어요.',
        '친구가 그린 그림을 한참 보더니 "나도 무지개 그리고 싶다"고 말했습니다.',
        '바깥놀이에서 달팽이를 발견하고 어디로 가는지 끝까지 따라가며 관찰했어요.',
        '점심시간에 아직 못 먹은 친구 옆에 앉아 기다려 주었습니다.',
        '노래에 맞춰 몸을 크게 흔들며 자기가 만든 동작을 친구들에게 알려 줬어요.',
        '가위질이 어렵다며 도움을 요청했고, 몇 번 해본 뒤 혼자 끝까지 오렸습니다.',
        '역할놀이에서 병원 놀이를 제안하고 친구들에게 역할을 나눠 주었어요.',
        '모래놀이 중 물길을 만들며 물이 어디로 흐르는지 계속 확인했습니다.'),
    ELT(1 + (s.n * 3 + k.rn) % 5, '쌓기놀이', '바깥놀이', '역할놀이', '미술영역', '음률활동'),
    ELT(1 + (s.n * 5 + k.rn) % 4, '친구와 협력', '교사에게 도움 요청', '또래에게 제안', '혼자 몰입'),
    ELT(1 + (s.n * 11 + k.rn) % 4, '적극적', '신중함', '즐거워함', '끈기 있음'),
    CASE (s.n + k.rn) % 7
        WHEN 1 THEN 'PHYSICAL_HEALTH'
        WHEN 2 THEN 'COMMUNICATION'
        WHEN 3 THEN 'SOCIAL'
        WHEN 4 THEN 'ART'
        WHEN 5 THEN 'NATURE'
        ELSE NULL              -- 미분류(일지 분석 전 상태) 도 섞는다
    END,
    DATE_ADD(@start, INTERVAL ((s.n * 9973 + k.rn * 7919) % @span) MINUTE),
    DATE_ADD(@start, INTERVAL ((s.n * 9973 + k.rn * 7919) % @span) MINUTE),
    -- 40건에 1건꼴 soft delete — @SQLRestriction 이 실제로 걸러내야 할 행
    CASE WHEN (s.n + k.rn) % 40 = 0
         THEN DATE_ADD(@start, INTERVAL ((s.n * 9973 + k.rn * 7919) % @span) + 60 MINUTE)
         ELSE NULL END
FROM kids k
         JOIN seq s
              ON s.n <= CASE WHEN k.rn <= 3 THEN 35 ELSE 130 + (k.rn * 7) % 45 END;

-- ---------------------------------------------------------------------------
-- 2) profile_photo — 아이 20명 × 250~340KB
--
--  프론트가 1:1 크롭+리사이즈해 올리는 썸네일의 현실적 크기대(서버 상한은 2MB).
--  UNHEX(REPEAT('AB', N)) → 정확히 N 바이트. 아이마다 조금씩 다르게 해 합계가 딱 떨어지지 않게 한다.
--  이미 사진이 있는 아이는 건드리지 않는다(INSERT IGNORE + bench_photo_owner 기록).
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO bench_photo_owner (owner_kind, owner_id)
SELECT 'CHILD', c.id
FROM (SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
      FROM child WHERE classroom_id = @cid AND deleted_at IS NULL) c
WHERE c.rn <= 20
  AND NOT EXISTS (SELECT 1 FROM profile_photo p
                  WHERE p.owner_kind = 'CHILD' AND p.owner_id = c.id);

INSERT IGNORE INTO profile_photo (owner_kind, owner_id, content_type, data, updated_at)
SELECT 'CHILD', o.owner_id, 'image/jpeg',
       UNHEX(REPEAT('AB', 250000 + (o.owner_id % 10) * 9000)),
       @now
FROM bench_photo_owner o
WHERE o.owner_kind = 'CHILD';

-- 교사 본인 사진(모든 화면의 헤더 아바타)
INSERT IGNORE INTO bench_photo_owner (owner_kind, owner_id)
SELECT 'TEACHER', @tid
WHERE NOT EXISTS (SELECT 1 FROM profile_photo WHERE owner_kind = 'TEACHER' AND owner_id = @tid);

INSERT IGNORE INTO profile_photo (owner_kind, owner_id, content_type, data, updated_at)
SELECT 'TEACHER', o.owner_id, 'image/jpeg', UNHEX(REPEAT('CD', 180000)), @now
FROM bench_photo_owner o
WHERE o.owner_kind = 'TEACHER';

-- ---------------------------------------------------------------------------
-- 3) daily_journal — 개학 후 평일마다 1건
--
--  content 는 앱이 저장하는 평탄화 JSON({summary, 5영역}) 과 같은 모양이어야 한다.
--  목록 API 가 이걸 전부 읽어 Jackson 으로 파싱한 뒤 summary 한 줄만 쓰기 때문에,
--  건수와 본문 크기가 둘 다 측정 대상이다. JSON_OBJECT 로 항상 유효한 JSON 을 만든다.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO daily_journal (teacher_id, classroom_id, journal_date, content, status,
                                  analyzed_at, version, created_at, updated_at)
WITH RECURSIVE days (d) AS (
    SELECT DATE(@start)
    UNION ALL
    SELECT DATE_ADD(d, INTERVAL 1 DAY) FROM days WHERE d < DATE(@now)
)
SELECT @tid, @cid, d,
       JSON_OBJECT(
           'summary', CONCAT('오늘은 ', DATE_FORMAT(d, '%c월 %e일'),
                             ' 바깥놀이와 쌓기놀이를 중심으로 하루가 흘렀습니다. ',
                             REPEAT('아이들은 스스로 놀이를 제안하고 서로의 방법을 살펴보며 놀이를 이어갔습니다. ', 3)),
           'PHYSICAL_HEALTH', REPEAT('바깥놀이에서 달리기와 오르기를 반복하며 몸의 움직임을 조절해 보았습니다. ', 5),
           'COMMUNICATION',   REPEAT('자기 생각을 말로 표현하고 친구의 이야기를 끝까지 듣는 모습이 나타났습니다. ', 5),
           'SOCIAL',          REPEAT('놀이 규칙을 함께 정하고 갈등이 생겼을 때 방법을 찾아보려 했습니다. ', 5),
           'ART',             REPEAT('노래와 그림으로 자기 느낌을 표현하며 즐거움을 나누었습니다. ', 5),
           'NATURE',          REPEAT('자연물을 관찰하고 왜 그런지 묻는 질문이 이어졌습니다. ', 5)
       ),
       IF(WEEKDAY(d) % 3 = 0, 'CONFIRMED', 'DRAFT'),
       DATE_ADD(CAST(d AS DATETIME), INTERVAL 9 HOUR), 1,
       DATE_ADD(CAST(d AS DATETIME), INTERVAL 9 HOUR),
       DATE_ADD(CAST(d AS DATETIME), INTERVAL 9 HOUR)
FROM days
WHERE WEEKDAY(d) < 5;  -- 평일만

-- ---------------------------------------------------------------------------
-- 4) child_report — 아이별 월말 평가
--
--  UNIQUE(child_id, report_month) 라 월별 1건. 목록 API 는 content 를 응답에서 빼지만
--  엔티티를 통째로 읽으므로 MEDIUMTEXT 를 전부 DB 에서 끌어온다 — 그 비용을 재려고 넣는다.
-- ---------------------------------------------------------------------------
INSERT IGNORE INTO child_report (child_id, report_type, period_start, period_end, report_month,
                                 content, created_at, updated_at)
WITH RECURSIVE months (m) AS (
    SELECT DATE_FORMAT(@start, '%Y-%m-01')
    UNION ALL
    SELECT DATE_ADD(m, INTERVAL 1 MONTH) FROM months WHERE m < DATE_FORMAT(@now, '%Y-%m-01')
)
SELECT c.id, 'MONTHLY',
       m, LAST_DAY(m), DATE_FORMAT(m, '%Y-%m'),
       CONCAT(REPEAT('이번 달 아이는 놀이 속에서 자기 생각을 표현하는 시도가 늘었습니다. ', 12),
              REPEAT('또래와의 상호작용에서 차례를 기다리고 제안을 주고받는 모습이 관찰되었습니다. ', 12)),
       DATE_ADD(CAST(LAST_DAY(m) AS DATETIME), INTERVAL 11 HOUR),
       DATE_ADD(CAST(LAST_DAY(m) AS DATETIME), INTERVAL 11 HOUR)
FROM months
         JOIN child c ON c.classroom_id = @cid AND c.deleted_at IS NULL
WHERE LAST_DAY(m) < DATE(@now);

-- ---------------------------------------------------------------------------
-- 결과 요약
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM memo)                                            AS memo,
    (SELECT COUNT(*) FROM memo WHERE deleted_at IS NOT NULL)               AS memo_deleted,
    (SELECT COUNT(*) FROM child WHERE classroom_id = @cid
                                 AND deleted_at IS NULL)                   AS child,
    (SELECT COUNT(*) FROM profile_photo)                                   AS photo,
    (SELECT ROUND(COALESCE(SUM(LENGTH(data)), 0) / 1024 / 1024, 1)
     FROM profile_photo)                                                   AS photo_mb,
    (SELECT COUNT(*) FROM daily_journal)                                   AS journal,
    (SELECT COUNT(*) FROM child_report)                                    AS report;
