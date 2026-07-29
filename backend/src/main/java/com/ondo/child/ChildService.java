package com.ondo.child;

import com.ondo.child.domain.Child;
import com.ondo.child.dto.ChildRequest;
import com.ondo.child.dto.ChildResponse;
import com.ondo.classroom.ClassroomRepository;
import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.common.time.AppTime;
import com.ondo.photo.ProfilePhotoService;
import com.ondo.photo.domain.OwnerKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ChildService {

    private final ChildRepository childRepository;
    private final ClassroomRepository classroomRepository;
    private final ProfilePhotoService photoService;

    public ChildService(ChildRepository childRepository, ClassroomRepository classroomRepository,
                        ProfilePhotoService photoService) {
        this.childRepository = childRepository;
        this.classroomRepository = classroomRepository;
        this.photoService = photoService;
    }

    /** 반 명단(가나다순, 삭제 제외). 반 소유권 검증. 사진 갱신시각은 일괄 조회로 채운다. */
    public List<ChildResponse> getChildren(Long teacherId, Long classroomId) {
        verifyClassroomOwnership(teacherId, classroomId);
        List<Child> children = childRepository.findByClassroomIdOrderByNameAscIdAsc(classroomId);
        Map<Long, LocalDateTime> photoTimes = photoService.updatedAtByOwnerId(
                OwnerKind.CHILD, children.stream().map(Child::getId).toList());
        return children.stream()
                .map(c -> ChildResponse.from(c, photoTimes.get(c.getId())))
                .toList();
    }

    /** 숨긴 아이(soft delete) 명단 — 기록 참고용. 반 소유권 검증. */
    public List<ChildResponse> getHiddenChildren(Long teacherId, Long classroomId) {
        verifyClassroomOwnership(teacherId, classroomId);
        return childRepository.findHiddenByClassroomId(classroomId).stream()
                .map(ChildResponse::from)
                .toList();
    }

    @Transactional
    public ChildResponse registerChild(Long teacherId, Long classroomId, ChildRequest request) {
        verifyClassroomOwnership(teacherId, classroomId);
        String tokenAlias = generateTokenAlias(classroomId);
        Child child = childRepository.save(
                Child.create(classroomId, request.name(), request.birthDate(), request.gender(), tokenAlias));
        return ChildResponse.from(child);
    }

    @Transactional
    public ChildResponse updateChild(Long teacherId, Long childId, ChildRequest request) {
        Child child = findOwnedChild(teacherId, childId);
        child.update(request.name(), request.birthDate(), request.gender());
        return ChildResponse.from(child, photoService.updatedAtOrNull(OwnerKind.CHILD, childId));
    }

    @Transactional
    public void deleteChild(Long teacherId, Long childId) {
        Child child = findOwnedChild(teacherId, childId);
        child.softDelete();
    }

    /** 숨김 해제(복원). 숨긴 아이는 @SQLRestriction 때문에 일반 조회로 안 잡혀 네이티브로 소유권 확인 후 복원한다. */
    @Transactional
    public ChildResponse restoreChild(Long teacherId, Long childId) {
        Child child = childRepository.findByIdIncludingDeleted(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        classroomRepository.findByIdAndTeacherId(child.getClassroomId(), teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        childRepository.restoreById(childId, LocalDateTime.now(java.time.ZoneOffset.UTC));
        return childRepository.findById(childId)
                .map(c -> ChildResponse.from(c, photoService.updatedAtOrNull(OwnerKind.CHILD, childId)))
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
    }

    /**
     * 관찰 온도에서 잠시 접어두기(spec-child-warmth-snooze). 결석 등으로 볼 기회가 없던 아이가
     * 계속 LOW 로 뜨는 걸 막는다. 기간은 고정 14일이고 만료되면 자동으로 판정 대상에 돌아온다.
     * 이미 접힌 아이를 다시 접으면 오늘 기준으로 갱신된다(누적 아님).
     */
    @Transactional
    public void snoozeWarmth(Long teacherId, Long childId) {
        findOwnedChild(teacherId, childId).snoozeWarmth(AppTime.today());
    }

    /** 접어두기 해제 — 즉시 판정 대상으로 돌아온다. 접혀 있지 않아도 성공(멱등). */
    @Transactional
    public void clearWarmthSnooze(Long teacherId, Long childId) {
        findOwnedChild(teacherId, childId).clearWarmthSnooze();
    }

    /** 아이 소유권 검증(프로필 사진 등 외부 모듈에서 호출). 미존재/삭제/타 교사 → CHILD_NOT_FOUND. */
    public void assertOwnedChild(Long teacherId, Long childId) {
        findOwnedChild(teacherId, childId);
    }

    private void verifyClassroomOwnership(Long teacherId, Long classroomId) {
        classroomRepository.findByIdAndTeacherId(classroomId, teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLASSROOM_NOT_FOUND));
    }

    /** 아이 + 소유권을 함께 검증. 미존재/삭제/타 교사 소유는 모두 CHILD_NOT_FOUND(존재 비노출). */
    private Child findOwnedChild(Long teacherId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        classroomRepository.findByIdAndTeacherId(child.getClassroomId(), teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        return child;
    }

    /**
     * 반 내 유일한 안정 가명 부여(예: 아이A, 아이B...). 삭제 아이도 alias 점유 → 전체 행 수+1 로 인덱스 산정.
     */
    private String generateTokenAlias(Long classroomId) {
        long next = childRepository.countAllIncludingDeleted(classroomId) + 1;
        return "아이" + toLetters(next);
    }

    /** 1→A, 26→Z, 27→AA ... (엑셀 열 방식). */
    private static String toLetters(long index) {
        StringBuilder sb = new StringBuilder();
        long n = index;
        while (n > 0) {
            n--;
            sb.insert(0, (char) ('A' + (n % 26)));
            n /= 26;
        }
        return sb.toString();
    }
}
