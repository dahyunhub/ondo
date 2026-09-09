package com.ondo.auth.jwt;

import com.ondo.auth.TeacherRepository;
import com.ondo.common.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Bearer JWT 를 파싱해 SecurityContext 에 인증을 채운다. 무상태.
 * 토큰이 없거나 무효면 그대로 통과시키고, 보호 경로 접근 시 SecurityConfig 의 EntryPoint 가 401 로 응답한다.
 * 만료/무효는 요청 속성으로 구분해 EntryPoint 가 적절한 ErrorCode 를 고른다.
 *
 * 빈으로 등록하지 않고 SecurityConfig 에서 직접 생성한다(서블릿 체인 자동 등록 방지).
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ATTR_AUTH_ERROR = "ondo.authError";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final TeacherRepository teacherRepository;

    public JwtAuthFilter(JwtProvider jwtProvider, TeacherRepository teacherRepository) {
        this.jwtProvider = jwtProvider;
        this.teacherRepository = teacherRepository;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                JwtProvider.TokenClaims claims = jwtProvider.parse(token);
                if (isRevokedByPasswordChange(claims)) {
                    request.setAttribute(ATTR_AUTH_ERROR, ErrorCode.AUTH_TOKEN_REVOKED);
                } else {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            claims.teacherId(), null, List.of(new SimpleGrantedAuthority("ROLE_TEACHER")));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException e) {
                request.setAttribute(ATTR_AUTH_ERROR, ErrorCode.AUTH_TOKEN_EXPIRED);
            } catch (JwtException | IllegalArgumentException e) {
                request.setAttribute(ATTR_AUTH_ERROR, ErrorCode.AUTH_UNAUTHENTICATED);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 비밀번호가 바뀐 뒤에 발급된 토큰인지 판정한다. 무상태 JWT 는 스스로 폐기할 수 없어,
     * 계정에 남긴 변경 시각보다 먼저 발급된 토큰을 여기서 거절하는 방식으로 무효화한다.
     *
     * <p>JWT 의 iat 는 초 단위라 변경 시각도 초로 잘라 비교한다. 같은 초에 발급된 토큰은
     * 통과하는데, 이는 <b>변경 직후 재발급한 토큰을 살리기 위한</b> 선택이며 그 대가로 최대 1초의
     * 창이 남는다(그 순간에 발급된 남의 토큰까지 살아남는다). 초 단위 iat 를 쓰는 한 피할 수 없고,
     * 공격자가 정확히 그 1초 안에 발급받은 토큰을 들고 있어야 하므로 감수한다.
     *
     * <p>비용: 인증된 요청마다 스칼라 1건을 읽는다(엔티티 로드 없음).
     */
    private boolean isRevokedByPasswordChange(JwtProvider.TokenClaims claims) {
        return teacherRepository.findPasswordChangedAt(claims.teacherId())
                .map(changedAt -> claims.issuedAt()
                        .isBefore(changedAt.toInstant(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS)))
                .orElse(false);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
