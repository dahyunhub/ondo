package com.ondo.auth;

import com.ondo.auth.dto.KakaoLoginRequest;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.LoginResponse;
import com.ondo.auth.dto.PasswordResetConfirmRequest;
import com.ondo.auth.dto.PasswordResetRequest;
import com.ondo.auth.dto.RegisterRequest;
import com.ondo.auth.reset.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** 회원가입(FR-12) — 201, 가입 즉시 로그인과 동일한 토큰 발급(자동 로그인). */
    @PostMapping("/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * 비밀번호 재설정 요청(API [23]) — 메일로 재설정 링크 발송.
     *
     * <p><b>가입 여부와 무관하게 항상 204</b> 를 준다. 응답이 갈리면 이 엔드포인트가
     * "이 이메일이 가입돼 있나?"를 알려주는 조회기가 되기 때문이다(spec-password-reset).
     */
    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.request(request.email());
        return ResponseEntity.noContent().build();
    }

    /** 비밀번호 재설정 확정(API [24]) — 토큰 검증 후 비밀번호 교체. 자동 로그인은 하지 않는다. */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirm(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    /** 카카오 로그인 — 인가 코드 검증 후 이메일 로그인과 동일한 LoginResponse(200) 반환. */
    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoLogin(request));
    }
}
