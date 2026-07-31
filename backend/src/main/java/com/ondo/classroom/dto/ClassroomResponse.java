package com.ondo.classroom.dto;

import java.time.LocalDate;

/**
 * 담당 반 목록 응답(API [2]). childCount = soft delete 안 된 아이 수.
 */
public record ClassroomResponse(
        Long id,
        String name,
        Integer year,
        /** 만 나이(0~5). null = 미지정·혼합연령반. 아이 생년월일 기본 연도 계산에 쓴다. */
        Integer ageClass,
        LocalDate startDate,
        long childCount
) {
}
