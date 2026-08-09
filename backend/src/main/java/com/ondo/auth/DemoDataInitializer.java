package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.child.domain.Gender;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.classroom.domain.Classroom;
import com.ondo.config.DemoProperties;
import com.ondo.journal.DailyJournalRepository;
import com.ondo.journal.JournalMemoLinkRepository;
import com.ondo.journal.domain.DailyJournal;
import com.ondo.journal.domain.JournalMemoLink;
import com.ondo.journal.domain.JournalStatus;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import com.ondo.report.ChildReportRepository;
import com.ondo.report.domain.ChildReport;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * 읽기 전용 데모 계정({@code ondo.demo.enabled=true})용 시연 데이터 시드.
 * <p>
 * dev 시드({@link DevDataInitializer})와 달리 profile 이 아니라 프로퍼티로 켜지며, 운영(prod) 라이브
 * 데모에도 주입된다. 방문자가 로그인 직후 바로 둘러볼 수 있도록 반·아이 23명뿐 아니라 <b>최근 2주 메모,
 * 확정된 AI 하루 일지 1건, 개인 관찰평가 1건</b>까지 채운다. 변경은 {@link DemoReadOnlyFilter} 가 막는다.
 * <p>
 * 시각 정합성: 아이·메모의 {@code created_at} 을 과거로 backdate 한다. 아이가 오늘 등록되면 관찰 온도의
 * 신규-아이 가드에 걸려 기능이 꺼지고, 메모가 전부 지금 시각이면 타임라인·온도가 밋밋해지기 때문이다.
 * 일부 아이는 최근 기록을 옅게 두어 관찰 온도의 LOW 표시(볼까요?)가 자연스럽게 나타나게 한다.
 * <p>멱등: 데모 교사에게 이미 반이 있으면 아무것도 하지 않는다.
 */
@Component
public class DemoDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final DemoProperties demoProperties;
    private final TeacherRepository teacherRepository;
    private final ClassroomRepository classroomRepository;
    private final ChildRepository childRepository;
    private final MemoRepository memoRepository;
    private final DailyJournalRepository journalRepository;
    private final JournalMemoLinkRepository journalMemoLinkRepository;
    private final ChildReportRepository reportRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager em;

    public DemoDataInitializer(DemoProperties demoProperties,
                               TeacherRepository teacherRepository,
                               ClassroomRepository classroomRepository,
                               ChildRepository childRepository,
                               MemoRepository memoRepository,
                               DailyJournalRepository journalRepository,
                               JournalMemoLinkRepository journalMemoLinkRepository,
                               ChildReportRepository reportRepository,
                               PasswordEncoder passwordEncoder,
                               TransactionTemplate transactionTemplate) {
        this.demoProperties = demoProperties;
        this.teacherRepository = teacherRepository;
        this.classroomRepository = classroomRepository;
        this.childRepository = childRepository;
        this.memoRepository = memoRepository;
        this.journalRepository = journalRepository;
        this.journalMemoLinkRepository = journalMemoLinkRepository;
        this.reportRepository = reportRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = transactionTemplate;
    }

    /** 아이 관찰 메모 풀. 누리과정 5영역을 고루 덮는 실제 관찰 톤의 문장. */
    private record MemoSeed(String content, String play, String interaction, String attitude, CurriculumArea area) {}

    private static final List<MemoSeed> MEMO_POOL = List.of(
            new MemoSeed("블록으로 높은 탑을 쌓다 무너지자 방법을 바꿔 다시 도전했어요.", "블록놀이", null, "끈기", CurriculumArea.NATURE),
            new MemoSeed("친구에게 크레파스를 먼저 건네며 같이 쓰자고 했어요.", null, "나눔·양보", null, CurriculumArea.SOCIAL),
            new MemoSeed("선생님 이야기에 끝까지 집중하고 궁금한 걸 질문했어요.", null, null, "경청", CurriculumArea.COMMUNICATION),
            new MemoSeed("바깥놀이에서 달리기를 하며 크게 웃었어요.", "대근육 활동", null, null, CurriculumArea.PHYSICAL_HEALTH),
            new MemoSeed("그림 그리기 시간에 무지개를 색색으로 표현했어요.", null, null, null, CurriculumArea.ART),
            new MemoSeed("물웅덩이에 뜬 나뭇잎을 한참 들여다봤어요.", null, null, "몰입", CurriculumArea.NATURE),
            new MemoSeed("속상해하는 친구를 토닥이며 괜찮냐고 물었어요.", null, "공감", null, CurriculumArea.SOCIAL),
            new MemoSeed("점심 후 스스로 자리를 정리하고 책상을 닦았어요.", null, null, "자조·정리", CurriculumArea.PHYSICAL_HEALTH),
            new MemoSeed("역할놀이에서 의사 역할을 맡아 친구를 진찰했어요.", "역할놀이", null, null, CurriculumArea.SOCIAL),
            new MemoSeed("새로 배운 노래를 율동과 함께 신나게 불렀어요.", null, null, null, CurriculumArea.ART),
            new MemoSeed("퍼즐이 어렵다고 하면서도 끝까지 맞췄어요.", "조작놀이", null, "끈기", CurriculumArea.NATURE),
            new MemoSeed("친구와 생각이 달라도 말로 차근차근 설명했어요.", null, "의사표현", null, CurriculumArea.COMMUNICATION),
            new MemoSeed("미끄럼틀을 오르내리며 순서를 잘 지켰어요.", "바깥놀이", null, null, CurriculumArea.PHYSICAL_HEALTH),
            new MemoSeed("동화책을 보고 뒷이야기를 상상해서 들려줬어요.", null, null, null, CurriculumArea.COMMUNICATION),
            new MemoSeed("화단의 달팽이를 발견하고 친구들에게 알려줬어요.", null, "나눔", null, CurriculumArea.NATURE),
            new MemoSeed("종이접기로 비행기를 접어 친구와 함께 날렸어요.", "조작놀이", null, null, CurriculumArea.ART));

    private record Kid(String name, LocalDate birth, Gender gender) {}

    private static final List<Kid> KIDS = List.of(
            new Kid("강하준", LocalDate.of(2021, 3, 15), Gender.MALE),
            new Kid("김서준", LocalDate.of(2021, 1, 8), Gender.MALE),
            new Kid("김지호", LocalDate.of(2021, 11, 22), Gender.MALE),
            new Kid("박서윤", LocalDate.of(2021, 2, 27), Gender.FEMALE),
            new Kid("이도윤", LocalDate.of(2021, 5, 19), Gender.MALE),
            new Kid("정시우", LocalDate.of(2021, 9, 30), Gender.MALE),
            new Kid("최아인", LocalDate.of(2021, 4, 21), Gender.FEMALE),
            new Kid("한예준", LocalDate.of(2021, 7, 3), Gender.MALE),
            new Kid("오지안", LocalDate.of(2021, 12, 11), Gender.MALE),
            new Kid("윤하율", LocalDate.of(2021, 6, 14), Gender.FEMALE),
            new Kid("임서아", LocalDate.of(2021, 8, 25), Gender.FEMALE),
            new Kid("장은우", LocalDate.of(2021, 1, 30), Gender.MALE),
            new Kid("조유나", LocalDate.of(2021, 10, 7), Gender.FEMALE),
            new Kid("신도아", LocalDate.of(2021, 3, 2), Gender.FEMALE),
            new Kid("유주원", LocalDate.of(2021, 5, 28), Gender.MALE),
            new Kid("배소율", LocalDate.of(2021, 9, 16), Gender.FEMALE),
            new Kid("문지우", LocalDate.of(2021, 2, 9), Gender.FEMALE),
            new Kid("양건우", LocalDate.of(2021, 11, 5), Gender.MALE),
            new Kid("손채원", LocalDate.of(2021, 4, 13), Gender.FEMALE),
            new Kid("백시윤", LocalDate.of(2021, 7, 27), Gender.MALE),
            new Kid("홍서연", LocalDate.of(2021, 6, 1), Gender.FEMALE),
            new Kid("고은서", LocalDate.of(2021, 10, 19), Gender.FEMALE),
            new Kid("남주하", LocalDate.of(2021, 8, 8), Gender.FEMALE));

    /** 최근 기록을 옅게 둘 아이 수(가장 뒤 N명) — 관찰 온도 LOW(볼까요?) 시연용. */
    private static final int QUIET_COUNT = 3;

    @Override
    public void run(String... args) {
        if (!demoProperties.isEnabled()) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> seed());
    }

    private void seed() {
        String email = demoProperties.getEmail();
        Teacher teacher = teacherRepository.findByEmail(email).orElse(null);
        if (teacher == null) {
            teacher = teacherRepository.save(Teacher.create(
                    email, passwordEncoder.encode(demoProperties.getPassword()), demoProperties.getName()));
            log.info("[demo] 시드 교사 생성: {} / {}", email, demoProperties.getPassword());
        }

        // 멱등: 이미 반이 있으면 재주입하지 않음.
        if (!classroomRepository.findClassroomSummaryRows(teacher.getId()).isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(KST);
        Long teacherId = teacher.getId();

        // 만 4세반(age_class=4) → 출생연도 = year - (age_class + 1).
        Long classroomId = classroomRepository.save(
                Classroom.create(teacherId, "만 4세반", today.getYear(), 4, LocalDate.of(today.getYear(), 3, 2))).getId();

        // 아이 등록 — token_alias 는 비식별 별칭(아이A, 아이B …).
        List<Long> childIds = new ArrayList<>();
        char alias = 'A';
        for (Kid k : KIDS) {
            childIds.add(childRepository.save(
                    Child.create(classroomId, k.name(), k.birth(), k.gender(), "아이" + alias)).getId());
            alias++;
        }
        // 아이는 학기 초(60일 전)에 등록된 것으로 backdate — 신규-아이 가드로 관찰 온도가 꺼지지 않게.
        em.flush();
        em.createNativeQuery("UPDATE child SET created_at = ?1 WHERE classroom_id = ?2")
                .setParameter(1, utc(today.minusDays(60).atTime(9, 0)))
                .setParameter(2, classroomId)
                .executeUpdate();

        // 메모 — 아이별로 최근 2주에 분산. 앞쪽 아이는 오늘 기록을 두어 일지 근거가 되고,
        // 마지막 QUIET_COUNT 명은 12~14일 전 1건만 남겨 관찰 온도 LOW 로 뜨게 한다.
        record Backdate(Long memoId, LocalDateTime at) {}
        List<Backdate> backdates = new ArrayList<>();
        List<Long> todayMemoIds = new ArrayList<>();
        int pool = 0;
        for (int i = 0; i < KIDS.size(); i++) {
            Long childId = childIds.get(i);
            boolean quiet = i >= KIDS.size() - QUIET_COUNT;
            int[] daysAgoList = quiet
                    ? new int[]{12 + (i % QUIET_COUNT)}                              // 12~14일 전 1건
                    : new int[]{i < 10 ? 0 : 1 + (i % 3),                            // 앞 10명은 오늘
                                3 + (i % 4), 8 + (i % 5),
                                (i % 2 == 0) ? 2 + (i % 3) : -1};                    // 짝수 아이는 최근 1건 추가

            for (int daysAgo : daysAgoList) {
                if (daysAgo < 0) {
                    continue;
                }
                MemoSeed s = MEMO_POOL.get(pool % MEMO_POOL.size());
                pool++;
                Memo memo = memoRepository.save(
                        Memo.create(childId, teacherId, s.content(), s.play(), s.interaction(), s.attitude()));
                memo.changeCurriculumArea(s.area());
                int hour = 9 + (pool % 8);                                           // 09~16시
                LocalDateTime at = utc(today.minusDays(daysAgo).atTime(hour, (pool * 7) % 60));
                backdates.add(new Backdate(memo.getId(), at));
                if (daysAgo == 0) {
                    todayMemoIds.add(memo.getId());
                }
            }
        }
        em.flush();
        for (Backdate b : backdates) {
            em.createNativeQuery("UPDATE memo SET created_at = ?1, updated_at = ?1 WHERE id = ?2")
                    .setParameter(1, b.at())
                    .setParameter(2, b.memoId())
                    .executeUpdate();
        }

        // 확정된 AI 하루 일지(오늘) — 오늘 메모를 근거로 링크한다.
        DailyJournal journal = DailyJournal.createDraft(teacherId, classroomId, today,
                DEMO_JOURNAL_CONTENT, utc(today.atTime(17, 30)));
        journal.update(DEMO_JOURNAL_CONTENT, JournalStatus.CONFIRMED);
        Long journalId = journalRepository.save(journal).getId();
        for (Long memoId : todayMemoIds) {
            journalMemoLinkRepository.save(JournalMemoLink.of(journalId, memoId));
        }

        // 개인 관찰평가 1건(첫 아이, 최근 2주) — 상담·발달평가 화면 시연용.
        reportRepository.save(ChildReport.createManual(
                childIds.get(0), today.minusDays(13), today, DEMO_REPORT_CONTENT));

        log.info("[demo] 시드 완료: 반 1 · 아이 {}명 · 메모 {}건 · 일지 1 · 평가 1",
                KIDS.size(), backdates.size());
    }

    /** KST 벽시계 시각을 저장용 UTC LocalDateTime 으로 변환(DB 는 UTC 저장). */
    private static LocalDateTime utc(LocalDateTime kstWallClock) {
        return kstWallClock.atZone(KST).toInstant().atZone(ZoneOffset.UTC).toLocalDateTime();
    }

    // 평탄화 5영역 JSON(ai-integration-spec §3). 문자열 값 안에는 개행이 없다(키 사이 개행은 JSON 공백).
    private static final String DEMO_JOURNAL_CONTENT = """
            {"summary":"오늘은 바깥놀이와 역할놀이에서 또래와 어울리는 모습이 활발했고, 자연물 관찰에 몰입한 아이들이 많았어요.",
            "PHYSICAL_HEALTH":"바깥놀이에서 달리기와 미끄럼틀을 즐기며 대근육을 활발히 썼고, 점심 후 스스로 자리를 정리하는 모습이 늘었어요.",
            "COMMUNICATION":"동화 뒷이야기를 상상해 들려주거나 친구와 의견을 말로 조율하는 등 자기 생각을 표현하는 시도가 많았어요.",
            "SOCIAL":"역할놀이에서 역할을 나눠 맡고, 속상해하는 친구를 먼저 다독이는 배려가 돋보였어요.",
            "ART":"무지개를 색색으로 표현하고 새 노래에 율동을 더하는 등 자유로운 표현을 즐겼어요.",
            "NATURE":"물에 뜬 나뭇잎과 화단 달팽이를 오래 관찰하며 궁금한 점을 친구들과 나눴어요."}""";

    // 개인 관찰평가 JSON(ai-integration-spec §4): {summary, areas:[{area,text}]}.
    private static final String DEMO_REPORT_CONTENT = """
            {"summary":"강하준은 최근 2주 관찰에서 또래와 협력하는 힘과 끝까지 해내는 끈기가 함께 자랐어요.",
            "areas":[
            {"area":"PHYSICAL_HEALTH","text":"바깥놀이에서 달리기와 오르기를 즐기며 대근육 조절이 안정되었고, 식사 후 정리를 스스로 해내요."},
            {"area":"COMMUNICATION","text":"자기 생각을 문장으로 또렷하게 전하고, 친구의 말을 끝까지 듣고 되물어요."},
            {"area":"SOCIAL","text":"역할놀이에서 역할을 나누고 속상한 친구를 먼저 챙기는 배려가 돋보여요."},
            {"area":"ART","text":"색과 재료를 다양하게 시도하며 자기만의 방식으로 표현하는 것을 즐겨요."},
            {"area":"NATURE","text":"곤충과 자연물을 오래 관찰하고 궁금한 점을 친구들과 나누며 탐구를 즐겨요."}]}""";
}
