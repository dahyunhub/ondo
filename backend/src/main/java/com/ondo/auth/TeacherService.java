package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.TeacherMeResponse;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.photo.ProfilePhotoService;
import com.ondo.photo.domain.OwnerKind;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 본인 프로필 관리(이름·비밀번호). 인증된 teacherId 기준으로만 동작 — 타인 수정 경로 없음.
 */
@Service
@Transactional(readOnly = true)
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfilePhotoService photoService;

    public TeacherService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder,
                          ProfilePhotoService photoService) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.photoService = photoService;
    }

    @Transactional
    public TeacherMeResponse updateName(Long teacherId, String name) {
        Teacher teacher = findTeacher(teacherId);
        teacher.changeName(name.trim());
        return new TeacherMeResponse(teacher.getId(), teacher.getEmail(), teacher.getName(),
                photoService.updatedAtOrNull(OwnerKind.TEACHER, teacher.getId()));
    }

    /** 현재 비밀번호가 일치할 때만 변경. 불일치는 로그인과 동일한 AUTH_INVALID_CREDENTIALS(401). */
    @Transactional
    public void changePassword(Long teacherId, String currentPassword, String newPassword) {
        Teacher teacher = findTeacher(teacherId);
        if (!passwordEncoder.matches(currentPassword, teacher.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        teacher.changePassword(passwordEncoder.encode(newPassword));
    }

    private Teacher findTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));
    }
}
