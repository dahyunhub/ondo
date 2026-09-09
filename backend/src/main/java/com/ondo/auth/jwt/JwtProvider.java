package com.ondo.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 무상태 JWT 발급·검증(HS256). 짧은 만료(~1h), 리프레시 없음(NFR-6).
 * subject = teacherId, claim "email".
 */
@Component
public class JwtProvider {

    private static final String CLAIM_EMAIL = "email";

    private final SecretKey key;
    private final long expirationSeconds;

    public JwtProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.expirationSeconds = properties.expirationSeconds();
    }

    public String createToken(Long teacherId, String email) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(expirationSeconds);
        return Jwts.builder()
                .subject(String.valueOf(teacherId))
                .claim(CLAIM_EMAIL, email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰 파싱 후 teacherId(subject) 반환.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료 시
     * @throws io.jsonwebtoken.JwtException        서명/형식 오류 시
     */
    public Long parseTeacherId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    /**
     * teacherId 와 발급시각을 함께 반환. 발급시각은 비밀번호 변경 이후 토큰인지 판정하는 데 쓴다.
     * iat 가 없는 토큰(이 서버가 발급한 것이 아님)은 가장 오래된 시각으로 취급해, 무효화 기준이
     * 있으면 거절되도록 한다.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료 시
     * @throws io.jsonwebtoken.JwtException        서명/형식 오류 시
     */
    public TokenClaims parse(String token) {
        Claims claims = parseClaims(token);
        Date issuedAt = claims.getIssuedAt();
        return new TokenClaims(Long.valueOf(claims.getSubject()),
                issuedAt != null ? issuedAt.toInstant() : Instant.EPOCH);
    }

    /** 토큰에서 꺼내 쓰는 값. issuedAt 은 JWT 규격상 <b>초 단위</b>다. */
    public record TokenClaims(Long teacherId, Instant issuedAt) {
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
