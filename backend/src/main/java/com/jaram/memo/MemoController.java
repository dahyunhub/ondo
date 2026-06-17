package com.jaram.memo;

import com.jaram.memo.dto.CurriculumAreaRequest;
import com.jaram.memo.dto.MemoRequest;
import com.jaram.memo.dto.MemoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/memos")
public class MemoController {

    private final MemoService memoService;

    public MemoController(MemoService memoService) {
        this.memoService = memoService;
    }

    @PostMapping
    public ResponseEntity<MemoResponse> create(@AuthenticationPrincipal Long teacherId,
                                               @Valid @RequestBody MemoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memoService.save(teacherId, request));
    }

    @PatchMapping("/{memoId}/curriculum-area")
    public MemoResponse updateCurriculumArea(@AuthenticationPrincipal Long teacherId,
                                             @PathVariable Long memoId,
                                             @Valid @RequestBody CurriculumAreaRequest request) {
        return memoService.updateCurriculumArea(teacherId, memoId, request.curriculumArea());
    }

    @DeleteMapping("/{memoId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Long teacherId,
                                       @PathVariable Long memoId) {
        memoService.delete(teacherId, memoId);
        return ResponseEntity.noContent().build();
    }
}
