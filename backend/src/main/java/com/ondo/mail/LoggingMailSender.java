package com.ondo.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 비-prod 메일 발송 — 실제로 보내지 않고 본문을 로그로 남긴다.
 * SMTP 계정 없이도 재설정 링크를 눈으로 확인하며 전 구간을 검증할 수 있게 하는 개발용 구현.
 *
 * <p>본문에 재설정 링크가 그대로 찍히므로 <b>prod 에서는 절대 활성화되지 않는다</b>(@Profile("!prod")).
 */
@Component
@Profile("!prod")
public class LoggingMailSender implements MailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailSender.class);

    @Override
    public void send(String to, String subject, String body) {
        log.info("""
                [dev-mail] 실제 발송하지 않고 내용만 출력합니다.
                  to      : {}
                  subject : {}
                  body    :
                {}""", to, subject, body);
    }
}
