package com.ondo.child.domain;

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
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * 아이(FR-11, NFR-1, NFR-2). name 은 실명(DB 에만 저장, 외부 AI 전송 금지).
 * token_alias 는 반 내 유일한 안정 가명(등록 시 부여). soft delete: deleted_at + @SQLRestriction.
 */
@Entity
@Table(name = "child")
@SQLRestriction("deleted_at is null")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Child extends BaseTimeEntity {

    /** 접어두기 기간(일). 온도 판정 창(14일)과 같게 맞춘다 — 더 짧으면 접어도 판정이 안 바뀐다. */
    public static final int WARMTH_SNOOZE_DAYS = 14;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(name = "token_alias", nullable = false)
    private String tokenAlias;

    /**
     * 관찰 온도 '잠시 접어두기' 만료일(KST 달력일). NULL=접히지 않음.
     * 결석 등으로 볼 기회가 없던 아이가 계속 LOW 로 뜨는 걸 막는 장치이며, <b>온도 판정에만</b> 작용한다.
     */
    @Column(name = "warmth_snoozed_until")
    private LocalDate warmthSnoozedUntil;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private Child(Long classroomId, String name, LocalDate birthDate, Gender gender, String tokenAlias) {
        this.classroomId = classroomId;
        this.name = normalizeName(name);
        this.birthDate = birthDate;
        this.gender = gender;
        this.tokenAlias = tokenAlias;
    }

    public static Child create(Long classroomId, String name, LocalDate birthDate, Gender gender, String tokenAlias) {
        return new Child(classroomId, name, birthDate, gender, tokenAlias);
    }

    public void update(String name, LocalDate birthDate, Gender gender) {
        this.name = normalizeName(name);
        this.birthDate = birthDate;
        this.gender = gender;
    }

    /** 이름 앞뒤 공백 제거(정렬·표시 일관성). */
    private static String normalizeName(String name) {
        return name == null ? null : name.strip();
    }

    /** soft delete. 물리 삭제 대신 deleted_at 을 채워 기록을 보존한다(NFR-2). */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now(ZoneOffset.UTC);
    }

    /** 관찰 온도 판정에서 잠시 제외한다. 기간은 고정 {@value #WARMTH_SNOOZE_DAYS}일 — 무기한 스누즈는 두지 않는다. */
    public void snoozeWarmth(LocalDate today) {
        this.warmthSnoozedUntil = today.plusDays(WARMTH_SNOOZE_DAYS);
    }

    /** 접어두기 해제 — 즉시 판정 대상으로 돌아온다. */
    public void clearWarmthSnooze() {
        this.warmthSnoozedUntil = null;
    }

    /**
     * 지금 접힌 상태인가. 만료일 <b>당일에는 이미 만료</b>다({@code today < until}).
     *
     * <p>자동 만료가 이 기능의 안전 속성이다 — 접어두고 잊으면 그 아이는 안전망에서 영구히 빠지고,
     * 월말 개인평가 재료가 없다는 걸 그때 알게 된다. 그건 온도 기능이 막으려던 사고 그 자체다.
     */
    public boolean isWarmthSnoozed(LocalDate today) {
        return warmthSnoozedUntil != null && today.isBefore(warmthSnoozedUntil);
    }
}
