package com.ondo.warmth;

import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.common.time.AppTime;
import com.ondo.memo.MemoRepository;
import com.ondo.warmth.domain.WarmthLevel;
import com.ondo.warmth.dto.ChildWarmth;
import com.ondo.warmth.dto.WarmthResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 아이별 관찰 온도(spec-child-warmth). 최근 14일(KST) 메모를 반감기 감쇠로 집계해 WARM/LOW 로 나눈다.
 * AI 호출·새 테이블 없이 {@code memo.created_at} + {@code child} 만으로 계산한다.
 *
 * <p>판정이 비대칭인 것이 이 기능의 핵심이다. 중앙값·상위 N% 같은 <b>상대 순위</b>로 가르면 언제나 절반이
 * LOW 가 되어 "내가 방치한 명단"이 만들어진다. 그래서 LOW 는 <b>반 평균의 30% 미만</b>인 소수에게만 붙고,
 * 고르게 기록한 반에서 LOW 가 0명인 것은 버그가 아니라 정상 동작이다.
 */
@Service
@Transactional(readOnly = true)
public class WarmthService {

    /** 판정 창(일). KST 달력일 기준, 오늘 포함. */
    static final int WINDOW_DAYS = 14;

    /** 점수 반감기(일). 5일 전 기록은 오늘 기록의 절반 무게 — 단순 건수로는 '요즘'이 표현되지 않는다. */
    private static final double HALF_LIFE_DAYS = 5.0;

    /** 반 평균 점수 대비 이 비율 미만이면 LOW 후보. */
    private static final double LOW_RATIO = 0.3;

    /**
     * LOW 표시 상한. 조건 없이 항상 적용된다 — 후보가 몇 명이든 가장 옅은 이 인원만 남는다(죄책감 폭탄 방지).
     *
     * <p>처음엔 "후보가 대상의 1/3을 넘을 때만" 상한을 걸었는데, 그러면 후보가 정확히 1/3 이하일 때
     * 상한이 통째로 빠져 15명 반에서 LOW 5명, 30명 반에서 10명이 나왔다. 게다가 규칙이 비단조적이었다 —
     * 방치된 아이가 5명일 땐 5명 다 뜨는데 6명이 되면 오히려 3명으로 줄었다. 무조건 상한으로 바꿔 둘 다 없앴다.
     */
    private static final int LOW_CAP = 3;

    /** 등록 후 이 기간(일, 오늘 포함)은 관찰 기회가 없었다고 보고 평균에서 제외하며 WARM 으로 취급한다. */
    private static final int NEW_CHILD_DAYS = 3;

    private final ClassroomRepository classroomRepository;
    private final ChildRepository childRepository;
    private final MemoRepository memoRepository;

    public WarmthService(ClassroomRepository classroomRepository, ChildRepository childRepository,
                         MemoRepository memoRepository) {
        this.classroomRepository = classroomRepository;
        this.childRepository = childRepository;
        this.memoRepository = memoRepository;
    }

    /** 반 전체 온도. 반 소유권 검증 후 계산한다. 판정 불가 상황은 예외가 아니라 {@code enabled=false} 로 응답. */
    public WarmthResponse getWarmth(Long teacherId, Long classroomId) {
        classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));

        LocalDate today = AppTime.today();
        List<Child> roster = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId);

        // 판정 대상 = 등록한 지 NEW_CHILD_DAYS 이상 된 아이. 오늘 등록한 아이가 무조건 LOW 로 뜨고
        // 반 평균까지 끌어내리는 것을 막는다.
        LocalDateTime newcomerFrom = AppTime.startOfDayUtc(today.minusDays(NEW_CHILD_DAYS - 1L));
        List<Child> target = roster.stream()
                .filter(c -> c.getCreatedAt().isBefore(newcomerFrom))
                .toList();
        if (target.isEmpty()) {
            return WarmthResponse.disabled(WINDOW_DAYS);
        }

        LocalDateTime since = AppTime.startOfDayUtc(today.minusDays(WINDOW_DAYS - 1L));
        LocalDateTime end = AppTime.startOfNextDayUtc(today); // 미래 시각 행이 최대 가중치로 새는 것 차단
        List<Object[]> rows = memoRepository.findRecentMemoTimes(classroomId, since, end);

        // 콜드 스타트 가드 — 반 전체 기록량이 아이 수에도 못 미치면 아직 판정할 근거가 없다.
        // 가입 직후 전원이 옅게 뜨는 상황을 원천 차단하는 마지막 안전장치.
        if (rows.size() < target.size()) {
            return WarmthResponse.disabled(WINDOW_DAYS);
        }

        Map<Long, Double> scores = scoreByChild(rows, today);
        Set<Long> lowIds = pickLow(target, scores);

        // 응답은 명단 순서(가나다순) 그대로 — 온도순 정렬은 그 자체로 "못 본 아이 목록"이 된다.
        List<ChildWarmth> items = roster.stream()
                .map(c -> new ChildWarmth(c.getId(),
                        lowIds.contains(c.getId()) ? WarmthLevel.LOW : WarmthLevel.WARM))
                .toList();
        return new WarmthResponse(true, WINDOW_DAYS, items);
    }

    /** 아이별 감쇠 점수 합. 메모 1건의 무게 = 0.5^(경과일/반감기). */
    private Map<Long, Double> scoreByChild(List<Object[]> rows, LocalDate today) {
        Map<Long, Double> scores = new HashMap<>();
        for (Object[] row : rows) {
            Long childId = ((Number) row[0]).longValue();
            LocalDate memoDate = AppTime.kstDateOf((LocalDateTime) row[1]);
            // 시계 오차로 미래 시각이 들어와도 무게가 1을 넘지 않도록 하한 0.
            long elapsed = Math.max(0L, ChronoUnit.DAYS.between(memoDate, today));
            scores.merge(childId, Math.pow(0.5, elapsed / HALF_LIFE_DAYS), Double::sum);
        }
        return scores;
    }

    /**
     * LOW 로 표시할 아이. 평균의 30% 미만인 후보 중 가장 옅은 {@value #LOW_CAP} 명까지만.
     *
     * <p>대상 아이의 메모가 전무해 평균이 0이면 임계도 0이 되어 후보가 나오지 않는다(전원 WARM).
     * 판정 근거가 없을 때 아무도 지목하지 않는 쪽으로 무너지는 것이 이 기능의 안전한 실패 방향이다.
     */
    private Set<Long> pickLow(List<Child> target, Map<Long, Double> scores) {
        double threshold = target.stream()
                .mapToDouble(c -> score(scores, c))
                .average().orElse(0.0) * LOW_RATIO;

        return target.stream()
                .filter(c -> score(scores, c) < threshold)
                .sorted(Comparator.comparingDouble((Child c) -> score(scores, c)).thenComparing(Child::getId))
                .limit(LOW_CAP)
                .map(Child::getId)
                .collect(Collectors.toSet());
    }

    private static double score(Map<Long, Double> scores, Child child) {
        return scores.getOrDefault(child.getId(), 0.0);
    }
}
