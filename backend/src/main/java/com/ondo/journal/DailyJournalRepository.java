package com.ondo.journal;

import com.ondo.journal.domain.DailyJournal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyJournalRepository extends JpaRepository<DailyJournal, Long> {

    /** 일지 1건 유일성(UNIQUE teacher_id, classroom_id, journal_date) 검사. 이미 있으면 JOURNAL_ALREADY_EXISTS. */
    boolean existsByTeacherIdAndClassroomIdAndJournalDate(Long teacherId, Long classroomId, LocalDate journalDate);

    /** 소유권 검증용(조회·수정) — 본인 일지만. 타 교사는 빈 결과 → JOURNAL_NOT_FOUND(존재 비노출). */
    Optional<DailyJournal> findByIdAndTeacherId(Long id, Long teacherId);

    /** [12] 반·날짜 일지 단건 조회(재분석 안내용). 없으면 JOURNAL_NOT_FOUND. */
    Optional<DailyJournal> findByTeacherIdAndClassroomIdAndJournalDate(Long teacherId, Long classroomId, LocalDate journalDate);
}
