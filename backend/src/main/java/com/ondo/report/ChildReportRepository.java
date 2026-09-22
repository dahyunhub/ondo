package com.ondo.report;

import com.ondo.report.domain.ChildReport;
import com.ondo.report.domain.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChildReportRepository extends JpaRepository<ChildReport, Long> {

    /**
     * 평가 목록([17]): 생성 시간순. 수동·자동 모두 포함.
     *
     * 엔티티가 아니라 projection 을 반환한다. 목록 응답에는 content 가 없는데(ReportSummaryResponse)
     * 엔티티로 받으면 MEDIUMTEXT 를 읽어서 그대로 버린다 — 아이 1명당 평가 6건에 ≈13KB 였다.
     * 사진(LONGBLOB)에서 똑같은 결함을 겪었다(2026-09-09 항목). 사진은 테이블을 나눠 구조로 막았지만
     * content 는 단건 조회([18])에서 실제로 쓰이므로 여기서는 select 절을 좁히는 쪽을 택한다.
     */
    List<ReportSummary> findSummariesByChildIdOrderByCreatedAtAsc(Long childId);

    /** 기간 계산용: 직전 평가(period_end 최댓값). 없으면 empty → 반 start_date 사용. */
    Optional<ChildReport> findTopByChildIdOrderByPeriodEndDesc(Long childId);

    /** 월말 자동 멱등(FR-9): 그달 평가가 이미 있으면 AI 호출 전에 skip. */
    boolean existsByChildIdAndReportMonth(Long childId, String reportMonth);

    /** 목록 전용 projection — content 가 들어올 자리가 없다. */
    interface ReportSummary {
        Long getId();

        ReportType getReportType();

        LocalDate getPeriodStart();

        LocalDate getPeriodEnd();

        String getReportMonth();

        LocalDateTime getCreatedAt();
    }
}
