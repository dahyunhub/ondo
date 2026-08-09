package com.ondo.feedback;

import com.ondo.feedback.domain.Feedback;
import com.ondo.feedback.dto.FeedbackRequest;
import com.ondo.mail.MailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인앱 피드백 처리. 원본을 DB 에 남기고(durable 기록), 설정된 관리자 주소로 알림 메일을 보낸다.
 * <p>
 * 메일은 best-effort — {@link MailSender} 가 실패를 삼키므로 알림이 실패해도 피드백은 이미 저장돼 있다.
 * 수신 주소({@code ondo.feedback.to})가 비어 있으면 저장만 하고 메일은 생략한다.
 */
@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    private static final int USER_AGENT_MAX = 500;

    private final FeedbackRepository repository;
    private final MailSender mailSender;
    private final FeedbackProperties properties;

    public FeedbackService(FeedbackRepository repository, MailSender mailSender, FeedbackProperties properties) {
        this.repository = repository;
        this.mailSender = mailSender;
        this.properties = properties;
    }

    @Transactional
    public void submit(Long teacherId, FeedbackRequest request, String userAgent) {
        Feedback saved = repository.save(Feedback.of(
                teacherId,
                request.message().trim(),
                blankToNull(request.category()),
                blankToNull(request.page()),
                truncate(userAgent, USER_AGENT_MAX)));
        log.info("피드백 접수 #{} (teacherId={}, category={}, page={})",
                saved.getId(), teacherId, saved.getCategory(), saved.getPage());
        notifyAdmin(saved, teacherId);
    }

    private void notifyAdmin(Feedback feedback, Long teacherId) {
        String to = properties.to();
        if (to == null || to.isBlank()) {
            return; // 수신 주소 미설정 → 저장만.
        }
        String category = feedback.getCategory() != null ? feedback.getCategory() : "의견";
        String subject = "[온도 피드백] " + category + " #" + feedback.getId();
        String body = """
                새 피드백이 접수됐어요.

                분류: %s
                교사 ID: %s
                화면: %s
                접수 시각(UTC): %s

                내용:
                %s
                """.formatted(
                category,
                teacherId,
                feedback.getPage() != null ? feedback.getPage() : "-",
                feedback.getCreatedAt(),
                feedback.getMessage());
        mailSender.send(to, subject, body);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
