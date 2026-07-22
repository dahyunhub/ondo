package com.ondo.auth;

import com.ondo.auth.dto.KakaoLoginRequest;
import com.ondo.auth.dto.LoginRequest;
import com.ondo.auth.dto.LoginResponse;
import com.ondo.auth.dto.RegisterRequest;
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

    public AuthController(AuthService authService) {
        this.authService = authService;
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

    /** 카카오 로그인 — 인가 코드 검증 후 이메일 로그인과 동일한 LoginResponse(200) 반환. */
    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        return ResponseEntity.ok(authService.kakaoLogin(request));
    }
}
