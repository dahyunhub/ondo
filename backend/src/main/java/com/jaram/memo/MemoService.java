package com.jaram.memo;

import com.jaram.child.ChildRepository;
import com.jaram.child.domain.Child;
import com.jaram.classroom.ClassroomRepository;
import com.jaram.common.exception.BusinessException;
import com.jaram.common.exception.ErrorCode;
import com.jaram.memo.domain.CurriculumArea;
import com.jaram.memo.domain.Memo;
import com.jaram.memo.dto.MemoRequest;
import com.jaram.memo.dto.MemoResponse;
import com.jaram.memo.dto.TimelineEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class MemoService {

    private final MemoRepository memoRepository;
    private final ChildRepository childRepository;
    private final ClassroomRepository classroomRepository;

    public MemoService(MemoRepository memoRepository, ChildRepository childRepository,
                       ClassroomRepository classroomRepository) {
        this.memoRepository = memoRepository;
        this.childRepository = childRepository;
        this.classroomRepository = classroomRepository;
    }

    /** 메모 저장(FR-1, NFR-5). 외부 AI 호출 없이 동기·즉시 저장. */
    @Transactional
    public MemoResponse save(Long teacherId, MemoRequest req) {
        if (allBlank(req.content(), req.playActivity(), req.interaction(), req.attitude())) {
            throw new BusinessException(ErrorCode.MEMO_EMPTY);
        }
        verifyChildOwnership(teacherId, req.childId());
        Memo memo = memoRepository.save(Memo.create(
                req.childId(), teacherId, req.content(), req.playActivity(), req.interaction(), req.attitude()));
        return MemoResponse.from(memo);
    }

    /** 아이별 타임라인(FR-7, Story 2.2). area: null/blank=전체, "UNCLASSIFIED"=미분류, 그 외=5영역 enum. 최신순. */
    public List<TimelineEntry> getTimeline(Long teacherId, Long childId, String area) {
        verifyChildOwnership(teacherId, childId);
        List<Memo> memos;
        if (area == null || area.isBlank()) {
            memos = memoRepository.findByChildIdOrderByCreatedAtDesc(childId);
        } else if ("UNCLASSIFIED".equals(area)) {
            memos = memoRepository.findByChildIdAndCurriculumAreaIsNullOrderByCreatedAtDesc(childId);
        } else {
            CurriculumArea parsed;
            try {
                parsed = CurriculumArea.valueOf(area);
            } catch (IllegalArgumentException e) {
                throw new BusinessException(ErrorCode.INVALID_CURRICULUM_AREA);
            }
            memos = memoRepository.findByChildIdAndCurriculumAreaOrderByCreatedAtDesc(childId, parsed);
        }
        return memos.stream().map(TimelineEntry::from).toList();
    }

    /** 메모 영역 수정(FR-7, Story 2.3). 소유 교사만. */
    @Transactional
    public MemoResponse updateCurriculumArea(Long teacherId, Long memoId, CurriculumArea area) {
        Memo memo = memoRepository.findById(memoId)
                .filter(m -> m.getTeacherId().equals(teacherId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMO_NOT_FOUND));
        memo.changeCurriculumArea(area);
        return MemoResponse.from(memo);
    }

    /** 메모 soft delete(FR-1). 소유 교사만. */
    @Transactional
    public void delete(Long teacherId, Long memoId) {
        Memo memo = memoRepository.findById(memoId)
                .filter(m -> m.getTeacherId().equals(teacherId))
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMO_NOT_FOUND));
        memo.softDelete();
    }

    /** 아이가 존재하고 현재 교사 소유 반에 속하는지 검증. 아니면 CHILD_NOT_FOUND(존재 비노출). */
    private void verifyChildOwnership(Long teacherId, Long childId) {
        Child child = childRepository.findById(childId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
        classroomRepository.findByIdAndTeacherId(child.getClassroomId(), teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHILD_NOT_FOUND));
    }

    private static boolean allBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return false;
        }
        return true;
    }
}
