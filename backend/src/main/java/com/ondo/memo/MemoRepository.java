package com.ondo.memo;

import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    /** 타임라인: 최신순 전체(soft delete 제외 — @SQLRestriction). */
    List<Memo> findByChildIdOrderByCreatedAtDesc(Long childId);

    /**
     * 일지 분석 묶음(Story 3.5): 특정 반 소속(soft delete 제외) 아동의 그 날짜([start, end)) 메모 전체.
     * index→memo 매핑 안정성을 위해 id 오름차순. child 서브쿼리에도 @SQLRestriction(deleted_at IS NULL)이 적용된다.
     */
    @Query("""
            SELECT m FROM Memo m
            WHERE m.childId IN (SELECT c.id FROM Child c WHERE c.classroomId = :classroomId)
              AND m.createdAt >= :start AND m.createdAt < :end
            ORDER BY m.id ASC
            """)
    List<Memo> findClassroomBundle(@Param("classroomId") Long classroomId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    /**
     * 개인평가 묶음(Story 4.1): 특정 아이의 기간 [start, end) 메모 전체(soft delete 제외 — @SQLRestriction).
     * id 오름차순(렌더 안정성). findClassroomBundle 의 아이 단위 버전.
     */
    @Query("""
            SELECT m FROM Memo m
            WHERE m.childId = :childId
              AND m.createdAt >= :start AND m.createdAt < :end
            ORDER BY m.id ASC
            """)
    List<Memo> findChildBundle(@Param("childId") Long childId,
                               @Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end);

    /**
     * 관찰 온도(spec-child-warmth): 특정 반 소속 아동의 {@code since} 이후 메모 시각만. 본문은 필요 없어
     * {@code (childId, createdAt)} 만 뽑는다. child 서브쿼리·메모 모두 @SQLRestriction 이 적용돼
     * 숨긴 아이·삭제 메모는 자동 제외된다. 정렬 불필요(앱에서 집계).
     *
     * @return 행: [childId(Number), createdAt(LocalDateTime, UTC)]
     */
    @Query("""
            SELECT m.childId, m.createdAt FROM Memo m
            WHERE m.childId IN (SELECT c.id FROM Child c WHERE c.classroomId = :classroomId)
              AND m.createdAt >= :since
            """)
    List<Object[]> findRecentMemoTimes(@Param("classroomId") Long classroomId,
                                       @Param("since") LocalDateTime since);

    /** 타임라인: 특정 영역 필터(최신순). */
    List<Memo> findByChildIdAndCurriculumAreaOrderByCreatedAtDesc(Long childId, CurriculumArea area);

    /** 타임라인: 미분류(curriculum_area IS NULL) 필터(최신순). */
    List<Memo> findByChildIdAndCurriculumAreaIsNullOrderByCreatedAtDesc(Long childId);
}
