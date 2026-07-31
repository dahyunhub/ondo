package com.ondo.auth.reset;

import com.ondo.auth.TeacherRepository;
import com.ondo.auth.domain.Teacher;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.mail.MailProperties;
import com.ondo.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * 비밀번호 재설정(spec-password-reset).
 *
 * <p>설계의 중심은 <b>계정 열거 방지</b>다. 가입된 이메일이든, 없는 이메일이든, 카카오 전용 계정이든
 * {@link #request(String)} 는 <b>아무것도 알려주지 않고 조용히 끝난다</b>. 예외를 던지거나 분기별로
 * 다른 응답을 주면 이 엔드포인트가 "이 이메일이 가입돼 있나?"를 확인해 주는 조회기가 된다.
 */
@Service
@Transactional(readOnly = true)
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 토큰 원문 바이트 수. Base64URL 로 약 43자. */
    private static final int TOKEN_BYTES = 32;

    private final TeacherRepository teacherRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSender mailSender;
    private final MailProperties mailProperties;

    public PasswordResetService(TeacherRepository teacherRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailSender mailSender,
                                MailProperties mailProperties) {
        this.teacherRepository = teacherRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
    }

    /**
     * 재설정 요청. <b>어떤 경우에도 예외를 던지지 않는다</b> — 호출부는 항상 같은 200 을 준다.
     *
     * <p>세 갈래가 있지만 바깥에서는 구분되지 않는다:
     * 미가입(아무것도 안 함) / 카카오 전용(안내 메일만) / 일반 계정(토큰 + 링크 메일).
     */
    @Transactional
    public void request(String email) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        Optional<Teacher> found = teacherRepository.findByEmail(normalize(email));
        if (found.isEmpty()) {
            return; // 미가입 — 흔적을 남기지 않는다
        }
        Teacher teacher = found.get();

        if (teacher.getPasswordHash() == null) {
            // 카카오 전용 계정: 바꿀 비밀번호가 없다. 토큰 없이 안내 메일만.
            mailSender.send(teacher.getEmail(), "[온도] 비밀번호 재설정 안내", kakaoGuideBody());
            return;
        }

        // 재발급 시 이전 링크는 즉시 죽인다 — 메일함에 살아있는 링크가 쌓이지 않게.
        invalidateOutstanding(teacher.getId(), now);

        String rawToken = generateToken();
        tokenRepository.save(PasswordResetToken.issue(teacher.getId(), sha256(rawToken), now));
        mailSender.send(teacher.getEmail(), "[온도] 비밀번호 재설정", resetBody(rawToken));
    }

    /**
     * 재설정 확정. 토큰이 유효하지 않으면 만료·재사용·위조를 <b>구분하지 않고</b> 같은 에러를 낸다.
     */
    @Transactional
    public void confirm(String rawToken, String newPassword) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        PasswordResetToken token = tokenRepository.findByTokenHash(sha256(rawToken))
                .filter(t -> t.isUsable(now))
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_INVALID));

        Teacher teacher = teacherRepository.findById(token.getTeacherId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESET_TOKEN_INVALID));

        teacher.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed(now);
        // 같은 계정의 다른 링크도 함께 폐기 — 비밀번호가 바뀌었으니 전부 무의미하다.
        invalidateOutstanding(teacher.getId(), now);
        log.info("비밀번호 재설정 완료: teacherId={}", teacher.getId());
    }

    private void invalidateOutstanding(Long teacherId, LocalDateTime now) {
        List<PasswordResetToken> outstanding = tokenRepository.findByTeacherIdAndUsedAtIsNull(teacherId);
        outstanding.forEach(t -> t.invalidate(now));
    }

    private String resetBody(String rawToken) {
        String link = mailProperties.normalizedBaseUrl() + "/reset-password?token=" + rawToken;
        return """
                선생님, 안녕하세요.

                아래 링크에서 새 비밀번호를 설정해 주세요.

                %s

                이 링크는 %d분 동안만 쓸 수 있고, 한 번 사용하면 만료돼요.
                본인이 요청한 게 아니라면 이 메일은 무시하셔도 괜찮아요. 비밀번호는 그대로 유지됩니다.

                — 온도"""
                .formatted(link, PasswordResetToken.VALID_MINUTES);
    }

    private String kakaoGuideBody() {
        return """
                선생님, 안녕하세요.

                이 계정은 카카오 로그인으로 가입되어 있어 따로 설정된 비밀번호가 없어요.
                로그인 화면에서 '카카오로 시작하기'를 눌러 들어와 주세요.

                본인이 요청한 게 아니라면 이 메일은 무시하셔도 괜찮아요.

                — 온도""";
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 토큰 원문 → 저장·조회용 해시. 솔트를 쓰지 않는 이유는 원문이 고엔트로피 난수라 사전공격 대상이 아니기 때문. */
    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원 환경", e);
        }
    }
}
