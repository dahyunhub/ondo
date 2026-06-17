package com.jaram.memo;

import com.jaram.memo.dto.TimelineEntry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 아이별 타임라인 조회(FR-7, API [8]). 메모 데이터라 memo 패키지에 둔다.
 */
@RestController
public class TimelineController {

    private final MemoService memoService;

    public TimelineController(MemoService memoService) {
        this.memoService = memoService;
    }

    @GetMapping("/api/v1/children/{childId}/timeline")
    public List<TimelineEntry> timeline(@AuthenticationPrincipal Long teacherId,
                                        @PathVariable Long childId,
                                        @RequestParam(required = false) String area) {
        return memoService.getTimeline(teacherId, childId, area);
    }
}
