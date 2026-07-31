package com.ondo.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 운영 메일 발송. Spring 표준 SMTP 설정(`spring.mail.*`)을 그대로 쓰므로
 * Gmail → Resend/SES 등으로 제공자를 바꿔도 환경변수만 갈아끼우면 된다.
 *
 * <p>발송 실패는 <b>삼켜서 로깅만</b> 한다. 실패를 위로 던지면 요청 API 가 500 을 내고,
 * 그 응답 차이만으로 "가입된 이메일인지"가 드러난다(spec-password-reset Never 조항).
 * 대신 본문·링크는 로그에 남기지 않는다 — 운영 로그가 계정 탈취 경로가 되면 안 된다.
 */
@Component
@Profile("prod")
public class SmtpMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailSender.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties properties;

    public SmtpMailSender(JavaMailSender javaMailSender, MailProperties properties) {
        this.javaMailSender = javaMailSender;
        this.properties = properties;
    }

    @Override
    public void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(properties.from());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            javaMailSender.send(message);
        } catch (Exception e) {
            // 수신자·제목까지만. 본문(=재설정 링크)은 남기지 않는다.
            log.error("메일 발송 실패: to={}, subject={}", to, subject, e);
        }
    }
}
