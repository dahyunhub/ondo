package com.ondo.child;

import com.ondo.child.dto.ChildRequest;
import com.ondo.child.dto.ChildResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 아이 등록·조회·수정·삭제(FR-11, API [3]~[6]). 소유권은 ChildService 에서 강제.
 */
@RestController
@RequestMapping("/api/v1")
public class ChildController {

    private final ChildService childService;

    public ChildController(ChildService childService) {
        this.childService = childService;
    }

    @GetMapping("/classrooms/{classroomId}/children")
    public List<ChildResponse> getChildren(@AuthenticationPrincipal Long teacherId,
                                           @PathVariable Long classroomId) {
        return childService.getChildren(teacherId, classroomId);
    }

    /** 숨긴 아이(soft delete) 명단 — 기록 참고용. */
    @GetMapping("/classrooms/{classroomId}/children/hidden")
    public List<ChildResponse> getHiddenChildren(@AuthenticationPrincipal Long teacherId,
                                                 @PathVariable Long classroomId) {
        return childService.getHiddenChildren(teacherId, classroomId);
    }

    @PostMapping("/classrooms/{classroomId}/children")
    public ResponseEntity<ChildResponse> registerChild(@AuthenticationPrincipal Long teacherId,
                                                        @PathVariable Long classroomId,
                                                        @Valid @RequestBody ChildRequest request) {
        ChildResponse response = childService.registerChild(teacherId, classroomId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/children/{childId}")
    public ChildResponse updateChild(@AuthenticationPrincipal Long teacherId,
                                     @PathVariable Long childId,
                                     @Valid @RequestBody ChildRequest request) {
        return childService.updateChild(teacherId, childId, request);
    }

    @DeleteMapping("/children/{childId}")
    public ResponseEntity<Void> deleteChild(@AuthenticationPrincipal Long teacherId,
                                            @PathVariable Long childId) {
        childService.deleteChild(teacherId, childId);
        return ResponseEntity.noContent().build();
    }

    /** 관찰 온도에서 잠시 접어두기(API [21]) — 14일 후 자동 만료. */
    @PostMapping("/children/{childId}/warmth-snooze")
    public ResponseEntity<Void> snoozeWarmth(@AuthenticationPrincipal Long teacherId,
                                             @PathVariable Long childId) {
        childService.snoozeWarmth(teacherId, childId);
        return ResponseEntity.noContent().build();
    }

    /** 접어두기 해제(API [22]) — 즉시 판정 대상 복귀. 접혀 있지 않아도 204(멱등). */
    @DeleteMapping("/children/{childId}/warmth-snooze")
    public ResponseEntity<Void> clearWarmthSnooze(@AuthenticationPrincipal Long teacherId,
                                                  @PathVariable Long childId) {
        childService.clearWarmthSnooze(teacherId, childId);
        return ResponseEntity.noContent().build();
    }

    /** 숨김 해제(복원) — 명단에서 숨긴 아이를 다시 활성화. */
    @PostMapping("/children/{childId}/restore")
    public ChildResponse restoreChild(@AuthenticationPrincipal Long teacherId,
                                      @PathVariable Long childId) {
        return childService.restoreChild(teacherId, childId);
    }
}
