package com.ondo.feedback.domain;

import com.ondo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인앱 피드백 1건(실사용자 의견의 durable 기록).
 * <p>
 * 이메일 알림은 best-effort(실패를 삼킴)라 유실될 수 있으므로, 원본은 이 테이블에 남긴다.
 * teacher_id 는 로그인 사용자를 가리키되(현재 엔드포인트는 인증 필수) 추후 익명 수집을 위해 NULL 을 허용한다.
 */
@Entity
@Table(name = "feedback")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feedback extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(length = 30)
    private String category;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(length = 200)
    private String page;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    private Feedback(Long teacherId, String message, String category, String page, String userAgent) {
        this.teacherId = teacherId;
        this.message = message;
        this.category = category;
        this.page = page;
        this.userAgent = userAgent;
    }

    public static Feedback of(Long teacherId, String message, String category, String page, String userAgent) {
        return new Feedback(teacherId, message, category, page, userAgent);
    }
}
