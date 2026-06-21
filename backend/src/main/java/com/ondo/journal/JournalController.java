package com.ondo.journal;

import com.ondo.journal.dto.JournalAnalyzeRequest;
import com.ondo.journal.dto.JournalDetailResponse;
import com.ondo.journal.dto.JournalListResponse;
import com.ondo.journal.dto.JournalResponse;
import com.ondo.journal.dto.JournalUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 보육일지 API(Epic 3). 정본: api-spec §5.
 */
@RestController
@RequestMapping("/api/v1/journals")
public class JournalController {

    private final JournalService journalService;

    public JournalController(JournalService journalService) {
        this.journalService = journalService;
    }

    /** [11] 하루치 일지 초안 생성 — 201 DRAFT. */
    @PostMapping("/analyze")
    public ResponseEntity<JournalResponse> analyze(@AuthenticationPrincipal Long teacherId,
                                                   @Valid @RequestBody JournalAnalyzeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(journalService.analyze(teacherId, request));
    }

    /** [12] 반·날짜 일지 조회 + 재분석 안내 — 200. */
    @GetMapping
    public JournalListResponse getByClassroomAndDate(@AuthenticationPrincipal Long teacherId,
                                                     @RequestParam Long classroomId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return journalService.getByClassroomAndDate(teacherId, classroomId, date);
    }

    /** [15] 재분석·덮어쓰기(FR-6) — 200. */
    @PostMapping("/{journalId}/analyze")
    public JournalResponse reanalyze(@AuthenticationPrincipal Long teacherId,
                                     @PathVariable Long journalId) {
        return journalService.reanalyze(teacherId, journalId);
    }

    /** [13] 단건 조회 — 200. */
    @GetMapping("/{journalId}")
    public JournalDetailResponse get(@AuthenticationPrincipal Long teacherId,
                                     @PathVariable Long journalId) {
        return journalService.getJournal(teacherId, journalId);
    }

    /** [14] 수정·확정(FR-5) — 200. */
    @PutMapping("/{journalId}")
    public JournalDetailResponse update(@AuthenticationPrincipal Long teacherId,
                                        @PathVariable Long journalId,
                                        @Valid @RequestBody JournalUpdateRequest request) {
        return journalService.updateJournal(teacherId, journalId, request);
    }
}
