package com.jaram.memo.domain;

import com.jaram.common.entity.BaseTimeEntity;
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
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 관찰 메모(FR-1). 아이에 연결되며 작성 교사 소유. 자유 입력 + 선택 3항목(놀이/상호작용/태도).
 * curriculum_area 는 저장 시 NULL(밤 분석에서 채움). soft delete: deleted_at + @SQLRestriction.
 */
@Entity
@Table(name = "memo")
@SQLRestriction("deleted_at is null")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(length = 2000)
    private String content;

    @Column(name = "play_activity", length = 500)
    private String playActivity;

    @Column(length = 500)
    private String interaction;

    @Column(length = 500)
    private String attitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "curriculum_area", length = 20)
    private CurriculumArea curriculumArea;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Memo(Long childId, Long teacherId, String content,
                 String playActivity, String interaction, String attitude) {
        this.childId = childId;
        this.teacherId = teacherId;
        this.content = blankToNull(content);
        this.playActivity = blankToNull(playActivity);
        this.interaction = blankToNull(interaction);
        this.attitude = blankToNull(attitude);
    }

    public static Memo create(Long childId, Long teacherId, String content,
                              String playActivity, String interaction, String attitude) {
        return new Memo(childId, teacherId, content, playActivity, interaction, attitude);
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** 누리과정 영역 수정(FR-7, Story 2.3). 자동 분류 결과를 교사가 교정. */
    public void changeCurriculumArea(CurriculumArea area) {
        this.curriculumArea = area;
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.strip();
    }
}
