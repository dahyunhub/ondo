package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.PasswordChangedResponse;
import com.ondo.auth.dto.TeacherMeResponse;
import com.ondo.auth.jwt.JwtProvider;
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
    private final JwtProvider jwtProvider;

    public TeacherService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder,
                          ProfilePhotoService photoService, JwtProvider jwtProvider) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.photoService = photoService;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public TeacherMeResponse updateName(Long teacherId, String name) {
        Teacher teacher = findTeacher(teacherId);
        teacher.changeName(name.trim());
        return new TeacherMeResponse(teacher.getId(), teacher.getEmail(), teacher.getName(),
                photoService.updatedAtOrNull(OwnerKind.TEACHER, teacher.getId()));
    }

    /**
     * 현재 비밀번호가 일치할 때만 변경. 불일치는 로그인과 동일한 AUTH_INVALID_CREDENTIALS(401).
     *
     * <p>변경 시점에 이전 토큰이 모두 무효화되므로, 이 요청을 보낸 기기가 곧바로 로그아웃되지
     * 않도록 새 토큰을 발급해 돌려준다. 다른 기기의 세션은 끊긴다.
     */
    @Transactional
    public PasswordChangedResponse changePassword(Long teacherId, String currentPassword, String newPassword) {
        Teacher teacher = findTeacher(teacherId);
        // 소셜 전용 계정(password_hash NULL)은 비밀번호가 없어 변경 대상이 아니다(matches() 전 가드).
        if (!teacher.hasPassword()) {
            throw new BusinessException(ErrorCode.SOCIAL_ACCOUNT_NO_PASSWORD);
        }
        if (!passwordEncoder.matches(currentPassword, teacher.getPasswordHash())) {
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        teacher.changePassword(passwordEncoder.encode(newPassword));
        return new PasswordChangedResponse(
                jwtProvider.createToken(teacher.getId(), teacher.getEmail()),
                "Bearer",
                jwtProvider.getExpirationSeconds());
    }

    private Teacher findTeacher(Long teacherId) {
        return teacherRepository.findById(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TEACHER_NOT_FOUND));
    }
}
