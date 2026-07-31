package com.ondo.mail;

/**
 * 메일 발송 추상화. {@code AiClient} 와 같은 이유로 인터페이스를 둔다 —
 * 외부 인프라(SMTP)가 없어도 전 구간을 구현·검증할 수 있어야 하기 때문이다.
 *
 * <p>구현은 프로파일로 갈린다: prod 는 {@link SmtpMailSender}, 그 외는 {@link LoggingMailSender}.
 */
public interface MailSender {

    /**
     * 메일 1통 발송. <b>호출부로 예외를 던지지 않는다</b> — 발송 실패가 API 응답을 바꾸면
     * 응답 차이로 계정 존재 여부가 새어 나간다(spec-password-reset). 실패는 내부에서 로깅한다.
     */
    void send(String to, String subject, String body);
}
