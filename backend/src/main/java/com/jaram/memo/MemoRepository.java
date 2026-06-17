package com.jaram.memo;

import com.jaram.memo.domain.CurriculumArea;
import com.jaram.memo.domain.Memo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    /** 타임라인: 최신순 전체(soft delete 제외 — @SQLRestriction). */
    List<Memo> findByChildIdOrderByCreatedAtDesc(Long childId);

    /** 타임라인: 특정 영역 필터(최신순). */
    List<Memo> findByChildIdAndCurriculumAreaOrderByCreatedAtDesc(Long childId, CurriculumArea area);

    /** 타임라인: 미분류(curriculum_area IS NULL) 필터(최신순). */
    List<Memo> findByChildIdAndCurriculumAreaIsNullOrderByCreatedAtDesc(Long childId);
}
