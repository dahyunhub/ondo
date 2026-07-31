package com.ondo.classroom;

import com.ondo.classroom.domain.Classroom;
import com.ondo.classroom.dto.ClassroomCreateRequest;
import com.ondo.classroom.dto.ClassroomResponse;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ClassroomService {

    private final ClassroomRepository classroomRepository;

    public ClassroomService(ClassroomRepository classroomRepository) {
        this.classroomRepository = classroomRepository;
    }

    /**
     * 현재 교사가 소유한 반만 반환(repository 레벨 소유권 강제). 타 교사 반은 조회되지 않는다.
     */
    public List<ClassroomResponse> getMyClassrooms(Long teacherId) {
        return classroomRepository.findClassroomSummaryRows(teacherId).stream()
                .map(ClassroomService::toResponse)
                .toList();
    }

    /**
     * 새 반 생성. start_date 는 학년도 3월 2일로 자동 설정(한국 학년도 시작). childCount=0.
     * 같은 교사·이름·학년도 중복은 사전 차단(UNIQUE 제약은 동시 생성 경합 안전망).
     */
    @Transactional
    public ClassroomResponse create(Long teacherId, ClassroomCreateRequest request) {
        String name = request.name().trim();
        if (classroomRepository.existsByTeacherIdAndNameAndYear(teacherId, name, request.year())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "같은 학년도에 같은 이름의 반이 이미 있어요.");
        }
        LocalDate startDate = LocalDate.of(request.year(), 3, 2);
        Classroom saved = classroomRepository.save(
                Classroom.create(teacherId, name, request.year(), request.ageClass(), startDate));
        return new ClassroomResponse(saved.getId(), saved.getName(), saved.getYear(),
                saved.getAgeClass(), saved.getStartDate(), 0L);
    }

    private static ClassroomResponse toResponse(Object[] row) {
        return new ClassroomResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                ((Number) row[2]).intValue(),
                row[3] == null ? null : ((Number) row[3]).intValue(), // age_class — 미지정 반은 null
                toLocalDate(row[4]),
                ((Number) row[5]).longValue());
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
}
