package com.ondo.config;

import com.ondo.auth.DemoReadOnlyFilter;
import com.ondo.auth.TeacherRepository;
import com.ondo.auth.jwt.JwtAuthFilter;
import com.ondo.auth.jwt.JwtProperties;
import com.ondo.auth.jwt.JwtProvider;
import com.ondo.auth.jwt.RestAuthenticationEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 무상태 보안 설정. /auth/login · /auth/register · /actuator/health 만 공개, 그 외 /api/v1/** 인증 필수(NFR-6).
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({JwtProperties.class, DemoProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtProvider jwtProvider,
                                                   RestAuthenticationEntryPoint authenticationEntryPoint,
                                                   TeacherRepository teacherRepository,
                                                   DemoProperties demoProperties)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/kakao",
                                "/api/v1/auth/password-reset/request", "/api/v1/auth/password-reset/confirm",
                                "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(new JwtAuthFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                // JwtAuthFilter 뒤 — principal 이 채워진 다음 데모 계정의 변경 요청을 차단한다.
                .addFilterAfter(new DemoReadOnlyFilter(teacherRepository, demoProperties),
                        JwtAuthFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
