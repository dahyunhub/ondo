package com.ondo.auth;

import com.ondo.auth.domain.Teacher;
import com.ondo.auth.dto.KakaoLoginRequest;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.LoginResponse;
import com.ondo.auth.dto.RegisterRequest;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.auth.kakao.KakaoOAuthClient;
import com.ondo.auth.kakao.KakaoOAuthClient.KakaoUser;
import com.ondo.auth.throttle.AuthAttemptScope;
import com.ondo.auth.throttle.AuthThrottleService;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.photo.ProfilePhotoService;
import com.ondo.photo.domain.OwnerKind;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final TeacherRepository teacherRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final ProfilePhotoService photoService;
    private final KakaoOAuthClient kakaoClient;
    private final AuthThrottleService throttleService;

    public AuthService(TeacherRepository teacherRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider,
                       ProfilePhotoService photoService, KakaoOAuthClient kakaoClient,
                       AuthThrottleService throttleService) {
        this.teacherRepository = teacherRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.photoService = photoService;
        this.kakaoClient = kakaoClient;
        this.throttleService = throttleService;
    }

    public LoginResponse login(LoginRequest request) {
        // 시도 제한은 계정 조회보다 먼저다. 존재하지 않는 이메일도 똑같이 세고 똑같이 429 를 주므로
        // 429 응답이 "그 계정이 있다"는 신호가 되지 않는다(계정 열거 방지 유지).
        String throttleKey = AuthThrottleService.key(request.email());
        throttleService.assertNotLocked(AuthAttemptScope.LOGIN, throttleKey);

        Optional<Teacher> found = teacherRepository.findByEmail(request.email());
        // 소셜 전용 계정(password_hash NULL)은 비번 로그인 불가 — matches() 전 null 가드(NPE 금지).
        if (found.isEmpty() || !found.get().hasPassword()
                || !passwordEncoder.matches(request.password(), found.get().getPasswordHash())) {
            throttleService.record(AuthAttemptScope.LOGIN, throttleKey);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        throttleService.clear(AuthAttemptScope.LOGIN, throttleKey);
        return issueToken(found.get());
    }

    /**
     * 카카오 로그인(FR-10 확장). 인가 코드를 카카오로 검증한 뒤 계정을 find-or-create/연동하고,
     * 이메일 로그인과 동일한 토큰을 발급한다. 연동 정책:
     * <ol>
     *   <li>(provider, provider_id) 일치 계정 → 그대로 재로그인</li>
     *   <li>없으면, 카카오가 준 <b>검증된</b> 이메일이 기존 계정과 일치 → 자동 연동</li>
     *   <li>그래도 없으면 신규 생성(이메일 미제공/미검증이면 email NULL)</li>
     * </ol>
     * 클래스가 readOnly 라 쓰기 트랜잭션을 명시한다.
     */
    @Transactional
    public LoginResponse kakaoLogin(KakaoLoginRequest request) {
        KakaoUser kakaoUser = kakaoClient.exchange(request.code(), request.redirectUri());

        Teacher teacher = teacherRepository
                .findByProviderAndProviderId(Teacher.PROVIDER_KAKAO, kakaoUser.providerId())
                .orElseGet(() -> linkOrCreate(kakaoUser));

        return issueToken(teacher);
    }

    /** provider_id 매칭 실패 시: 검증된 이메일로 기존 계정 자동 연동 → 없으면 신규 생성. */
    private Teacher linkOrCreate(KakaoUser kakaoUser) {
        boolean usableEmail = kakaoUser.email() != null && kakaoUser.emailVerified();
        if (usableEmail) {
            Teacher existing = teacherRepository.findByEmail(kakaoUser.email()).orElse(null);
            if (existing != null) {
                // provider 매칭은 앞에서 이미 실패했다. 그런데도 이메일로 찾은 계정에 provider 가 이미
                // 있다면 = 다른 소셜 identity 다. 그 행을 덮어쓰면 원 소유자가 탈취/락아웃되므로 거부한다.
                // provider 가 null 인 이메일/비번 계정만 카카오로 연동한다.
                if (existing.getProvider() != null) {
                    throw new BusinessException(ErrorCode.AUTH_KAKAO_FAILED);
                }
                existing.linkKakao(kakaoUser.providerId());
                return existing;
            }
        }
        // 미검증/미제공 이메일은 저장하지 않는다(계정 탈취·UNIQUE 선점 방지) — email NULL 신규 생성.
        String email = usableEmail ? kakaoUser.email() : null;
        return teacherRepository.save(
                Teacher.createFromKakao(kakaoUser.providerId(), email, kakaoUser.nickname()));
    }

    /**
     * 회원가입(FR-12). 이메일 중복은 EMAIL_ALREADY_EXISTS(409). 비밀번호는 BCrypt 해시로 저장하고,
     * 가입 성공 시 로그인과 동일한 토큰을 발급해 자동 로그인시킨다.
     * 클래스가 readOnly 라 쓰기 트랜잭션을 명시한다.
     */
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (teacherRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        Teacher teacher = teacherRepository.save(
                Teacher.create(request.email(), passwordEncoder.encode(request.password()), request.name()));
        return issueToken(teacher);
    }

    /** 로그인·가입 공통 토큰 발급(자동 로그인 응답 조립). */
    private LoginResponse issueToken(Teacher teacher) {
        String accessToken = jwtProvider.createToken(teacher.getId(), teacher.getEmail());
        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtProvider.getExpirationSeconds(),
                new LoginResponse.TeacherSummary(teacher.getId(), teacher.getEmail(), teacher.getName(),
                        photoService.updatedAtOrNull(OwnerKind.TEACHER, teacher.getId())));
    }
}
