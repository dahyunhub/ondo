package com.ondo.report.domain;

import com.ondo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 개인 관찰 평가(FR-8/FR-9). 테이블 child_report 는 Flyway V1 정본 — 매핑만 한다(DDL 생성 금지, ddl-auto=validate).
 * content 는 {summary, areas:[{area,text}]} JSON 문자열(ai-integration-spec §4).
 * MANUAL 은 report_month=null 이라 UNIQUE(child_id, report_month) 에 걸리지 않아 누적된다.
 */
@Entity
@Table(name = "child_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChildReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "report_month")
    private String reportMonth;

    @Column(columnDefinition = "MEDIUMTEXT", nullable = false)
    private String content;

    private ChildReport(Long childId, ReportType reportType, LocalDate periodStart, LocalDate periodEnd,
                        String reportMonth, String content) {
        this.childId = childId;
        this.reportType = reportType;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.reportMonth = reportMonth;
        this.content = content;
    }

    /** 수동 생성(FR-8). report_month=null 로 누적(덮어쓰지 않음). */
    public static ChildReport createManual(Long childId, LocalDate periodStart, LocalDate periodEnd, String content) {
        return new ChildReport(childId, ReportType.MANUAL, periodStart, periodEnd, null, content);
    }

    /** 월말 자동 생성(FR-9). report_month("yyyy-MM")로 UNIQUE(child_id, report_month) 멱등. 기간=그달 1일~말일. */
    public static ChildReport createMonthly(Long childId, LocalDate periodStart, LocalDate periodEnd,
                                            String reportMonth, String content) {
        return new ChildReport(childId, ReportType.MONTHLY, periodStart, periodEnd, reportMonth, content);
    }
}
