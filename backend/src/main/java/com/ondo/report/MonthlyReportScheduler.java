package com.ondo.report;

import com.ondo.child.ChildRepository;
import com.ondo.child.domain.Child;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * 월말 자동 개인평가 스케줄러(FR-9, ai-integration-spec §7). 반 전체 아이를 순차 순회하며 그달 평가를 생성한다.
 * - 아이 1명 단위 try-catch 격리: 한 아이의 실패(AI 오류·UNIQUE 경합 등)가 다른 아이로 전파되지 않는다(부분 성공·재실행 가능).
 * - 멱등: 이미 생성된 분(UNIQUE child_id, report_month)·그달 메모 없는 아이는 ReportService.createMonthly 에서 skip.
 * - 순차: 일반 for-loop(병렬 금지)로 AI 동시 폭주 방지(NFR-4). 저장 TX 는 ReportPersistService(REQUIRES_NEW).
 */
@Component
public class MonthlyReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyReportScheduler.class);

    private final ChildRepository childRepository;
    private final ReportService reportService;

    public MonthlyReportScheduler(ChildRepository childRepository, ReportService reportService) {
        this.childRepository = childRepository;
        this.reportService = reportService;
    }

    /** 매월 말일(기본 02:00 UTC) 실행. 주기는 ondo.report.monthly-cron 으로 override. */
    @Scheduled(cron = "${ondo.report.monthly-cron:0 0 2 L * *}", zone = "UTC")
    public void scheduledRun() {
        runForMonth(YearMonth.now(ZoneOffset.UTC));
    }

    /** 대상 월에 대해 전체 아이를 순회한다. 테스트·수동 트리거용 진입점. */
    public MonthlySummary runForMonth(YearMonth month) {
        int created = 0;
        int skippedExists = 0;
        int skippedNoMemo = 0;
        int failed = 0;

        for (Child child : childRepository.findAll()) { // @SQLRestriction 으로 soft delete 제외
            try {
                switch (reportService.createMonthly(child.getId(), month)) {
                    case CREATED -> created++;
                    case SKIPPED_EXISTS -> skippedExists++;
                    case SKIPPED_NO_MEMO -> skippedNoMemo++;
                }
            } catch (Exception e) {
                failed++;
                // 실명 로깅 금지(NFR-1) — childId·month 만 남긴다.
                log.warn("월말 자동 평가 실패 — childId={}, month={}", child.getId(), month, e);
            }
        }

        MonthlySummary summary = new MonthlySummary(created, skippedExists, skippedNoMemo, failed);
        log.info("월말 자동 평가 완료 — month={}, {}", month, summary);
        return summary;
    }

    /** 한 달 실행 집계. */
    public record MonthlySummary(int created, int skippedExists, int skippedNoMemo, int failed) {
    }
}
