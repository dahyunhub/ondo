package com.ondo.feedback;

import com.ondo.feedback.dto.FeedbackRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    /** 인앱 피드백 전송 — 201. 데모 계정도 허용(DemoReadOnlyFilter 예외). */
    @PostMapping
    public ResponseEntity<Void> submit(@AuthenticationPrincipal Long teacherId,
                                       @Valid @RequestBody FeedbackRequest request,
                                       @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        feedbackService.submit(teacherId, request, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
