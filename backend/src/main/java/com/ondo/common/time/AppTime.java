package com.ondo.common.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 앱의 '하루 경계' 단일 출처(Asia/Seoul, KST). 한국 유치원 사용자의 달력일을 기준으로 한다.
 * 메모 createdAt 은 UTC LocalDateTime 으로 저장되므로(BaseTimeEntity/JpaConfig), KST 달력일을
 * 묶을 땐 그 날의 KST 자정~다음날 자정을 'UTC LocalDateTime' 경계로 변환해 비교한다.
 */
public final class AppTime {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private AppTime() {
    }

    /** KST 기준 오늘. */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /** KST 기준 이번 달. */
    public static YearMonth thisMonth() {
        return YearMonth.now(ZONE);
    }

    /** KST 달력일 {@code date}의 시작(자정)을 UTC LocalDateTime 으로. (메모 묶음 하한, 포함) */
    public static LocalDateTime startOfDayUtc(LocalDate date) {
        return date.atStartOfDay(ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }

    /** KST 달력일 {@code date} 다음날 시작을 UTC LocalDateTime 으로. (메모 묶음 상한, 미포함) */
    public static LocalDateTime startOfNextDayUtc(LocalDate date) {
        return startOfDayUtc(date.plusDays(1));
    }
}
