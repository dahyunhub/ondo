package com.ondo.classroom;

import com.ondo.classroom.domain.Classroom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {

    /** 중복 반 생성 사전 차단(UNIQUE(teacher_id, name, year)와 동일 기준). */
    boolean existsByTeacherIdAndNameAndYear(Long teacherId, String name, Integer year);

    /** 소유권 검증용 — 현재 교사가 소유한 반만. 타 교사 반은 빈 결과(존재 비노출). */
    Optional<Classroom> findByIdAndTeacherId(Long id, Long teacherId);

    /**
     * 현재 교사가 소유한 반 목록을 아이 수(soft delete 제외)와 함께 조회한다(API [2]).
     * 학년도 내림차순 → 반 이름 오름차순 정렬. child 테이블 직접 카운트(Child 엔티티는 Story 1.4).
     * 반환 행: [id(Number), name(String), year(Number), age_class(Number|null), start_date(Date/LocalDate), childCount(Number)]
     */
    @Query(value = """
            SELECT c.id, c.name, c.year, c.age_class, c.start_date,
                   (SELECT COUNT(*) FROM child ch WHERE ch.classroom_id = c.id AND ch.deleted_at IS NULL)
            FROM classroom c
            WHERE c.teacher_id = :teacherId
            ORDER BY c.year DESC, c.name ASC
            """, nativeQuery = true)
    List<Object[]> findClassroomSummaryRows(@Param("teacherId") Long teacherId);
}
