package com.ondo.config;

import com.ondo.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 메일 설정 바인딩(`ondo.mail.*`). SMTP 접속 정보(`spring.mail.*`)는 Spring Boot 자동 구성이 맡는다.
 * 실제 발송 구현은 프로파일로 갈린다 — prod=SmtpMailSender, 그 외=LoggingMailSender.
 */
@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {
}
