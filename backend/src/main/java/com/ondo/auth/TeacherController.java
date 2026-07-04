package com.ondo.auth;

import com.ondo.auth.dto.ChangePasswordRequest;
import com.ondo.auth.dto.TeacherMeResponse;
import com.ondo.auth.dto.UpdateMyProfileRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 본인 프로필 API. 사진은 PhotoController(/teachers/me/photo) 담당.
 */
@RestController
@RequestMapping("/api/v1/teachers/me")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @PatchMapping
    public TeacherMeResponse updateMyProfile(@AuthenticationPrincipal Long teacherId,
                                             @Valid @RequestBody UpdateMyProfileRequest request) {
        return teacherService.updateName(teacherId, request.name());
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changeMyPassword(@AuthenticationPrincipal Long teacherId,
                                                 @Valid @RequestBody ChangePasswordRequest request) {
        teacherService.changePassword(teacherId, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
