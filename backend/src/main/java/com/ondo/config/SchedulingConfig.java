package com.ondo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화(FR-9 월말 자동 개인평가). @Scheduled 빈(MonthlyReportScheduler)이 동작하도록 켠다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
