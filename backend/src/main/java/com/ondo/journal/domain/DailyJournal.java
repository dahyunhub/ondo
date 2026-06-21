package com.ondo.journal.domain;

import com.ondo.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하루치 보육일지(FR-3). 테이블 daily_journal 은 Flyway V1 정본 — 매핑만 한다(DDL 생성 금지, ddl-auto=validate).
 * content 는 평탄화 5영역 JSON 문자열(ai-integration-spec §3). UNIQUE(teacher_id, classroom_id, journal_date).
 */
@Entity
@Table(name = "daily_journal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyJournal extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "journal_date", nullable = false)
    private LocalDate journalDate;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JournalStatus status;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    @Column(nullable = false)
    private int version;

    private DailyJournal(Long teacherId, Long classroomId, LocalDate journalDate,
                         String content, JournalStatus status, LocalDateTime analyzedAt) {
        this.teacherId = teacherId;
        this.classroomId = classroomId;
        this.journalDate = journalDate;
        this.content = content;
        this.status = status;
        this.analyzedAt = analyzedAt;
        this.version = 1;
    }

    /** AI 분석으로 생성된 초안(DRAFT). analyzedAt 은 생성 시각. */
    public static DailyJournal createDraft(Long teacherId, Long classroomId, LocalDate journalDate,
                                           String content, LocalDateTime analyzedAt) {
        return new DailyJournal(teacherId, classroomId, journalDate, content, JournalStatus.DRAFT, analyzedAt);
    }

    /** 교사의 수정·확정(FR-5, AI 없음). content(평탄화 JSON)·status 만 갱신. analyzedAt 은 분석 시각이라 유지. */
    public void update(String content, JournalStatus status) {
        this.content = content;
        this.status = status;
    }

    /** 재분석·덮어쓰기(FR-6). 새 AI 초안이므로 DRAFT 로 되돌리고 analyzedAt·version 을 갱신 → 재분석 안내 해소. */
    public void reanalyze(String content, LocalDateTime analyzedAt) {
        this.content = content;
        this.status = JournalStatus.DRAFT;
        this.analyzedAt = analyzedAt;
        this.version += 1;
    }
}
