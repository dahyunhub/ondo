package com.ondo.journal;

import com.ondo.journal.domain.JournalMemoLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JournalMemoLinkRepository extends JpaRepository<JournalMemoLink, Long> {

    /** 재분석 시 링크 재구성 — 기존 링크 일괄 삭제(즉시 실행 bulk DELETE → 같은 memoId 재삽입 충돌 방지). */
    @Modifying
    @Query("DELETE FROM JournalMemoLink l WHERE l.dailyJournalId = :journalId")
    void deleteByDailyJournalId(@Param("journalId") Long journalId);
}
