package com.ondo.warmth;

import com.ondo.warmth.dto.WarmthResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 아이별 관찰 온도 조회(API [20], spec-child-warmth). 소유권은 WarmthService 에서 강제.
 * 이 정보는 담당 교사 본인에게만 — 제3자(원장 등) 노출 경로를 만들지 않는다.
 */
@RestController
@RequestMapping("/api/v1")
public class WarmthController {

    private final WarmthService warmthService;

    public WarmthController(WarmthService warmthService) {
        this.warmthService = warmthService;
    }

    @GetMapping("/classrooms/{classroomId}/warmth")
    public WarmthResponse getWarmth(@AuthenticationPrincipal Long teacherId,
                                    @PathVariable Long classroomId) {
        return warmthService.getWarmth(teacherId, classroomId);
    }
}
