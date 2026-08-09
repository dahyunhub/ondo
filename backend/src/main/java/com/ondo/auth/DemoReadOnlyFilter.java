package com.ondo.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ondo.common.exception.ErrorCode;
import com.ondo.common.response.ApiError;
import com.ondo.config.DemoProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 읽기 전용 데모 계정 가드. 데모 교사로 인증된 요청 중 <b>변경 메서드</b>(POST/PUT/PATCH/DELETE)를
 * 컨트롤러에 닿기 전에 403(DEMO_READ_ONLY)으로 막는다. 방문자가 라이브 데모를 자유롭게 둘러보되
 * 시연 데이터는 훼손하지 못하게 하는 안전장치.
 * <p>
 * JwtAuthFilter 뒤에 두어 principal(teacherId)이 채워진 뒤 판정한다. 로그인·회원가입 등 인증 전
 * 공개 POST 는 principal 이 없어 자연히 통과한다. {@code ondo.demo.enabled=false} 면 아무것도 하지 않는다.
 */
public class DemoReadOnlyFilter extends OncePerRequestFilter {

    private static final Set<String> MUTATING = Set.of("POST", "PUT", "PATCH", "DELETE");

    // 데모 계정에도 허용하는 변경 요청 — 인앱 피드백은 데이터 훼손이 아니라 수집이라, 포트폴리오
    // 방문자(데모 계정)의 의견도 받는다. 자체 append-only 테이블에만 쓰므로 시연 데이터에 영향 없다.
    private static final Set<String> EXEMPT_PATHS = Set.of("/api/v1/feedback");

    // 보안 필터는 auto-config(Jackson) 보다 이른 시점에 만들어져 공용 ObjectMapper 주입이 실패할 수 있다.
    // 에러 바디는 단순 레코드라 자체 인스턴스로 충분하다.
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final TeacherRepository teacherRepository;
    private final DemoProperties demoProperties;

    /** 데모 교사 id 는 시드 후 불변 → 최초 1회만 조회해 캐싱한다. */
    private volatile Long demoTeacherId;

    public DemoReadOnlyFilter(TeacherRepository teacherRepository, DemoProperties demoProperties) {
        this.teacherRepository = teacherRepository;
        this.demoProperties = demoProperties;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (demoProperties.isEnabled()
                && MUTATING.contains(request.getMethod())
                && !EXEMPT_PATHS.contains(request.getRequestURI())
                && isDemoPrincipal()) {
            writeForbidden(request, response);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean isDemoPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Long teacherId)) {
            return false;
        }
        Long demoId = resolveDemoTeacherId();
        return demoId != null && demoId.equals(teacherId);
    }

    private Long resolveDemoTeacherId() {
        Long cached = demoTeacherId;
        if (cached != null) {
            return cached;
        }
        // 시드가 startup 에 끝나므로 대개 첫 조회에 존재한다. 아직 없으면 null → 이번 요청은 통과(다음에 재조회).
        Long resolved = teacherRepository.findByEmail(demoProperties.getEmail())
                .map(t -> t.getId())
                .orElse(null);
        demoTeacherId = resolved;
        return resolved;
    }

    private void writeForbidden(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ErrorCode code = ErrorCode.DEMO_READ_ONLY;
        ApiError body = ApiError.of(code.getStatus().value(), code.name(), code.getDefaultMessage(),
                request.getRequestURI());
        response.setStatus(code.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        OBJECT_MAPPER.writeValue(response.getWriter(), body);
    }
}
