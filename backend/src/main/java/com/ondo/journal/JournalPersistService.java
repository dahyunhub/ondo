package com.ondo.journal;

import com.ondo.common.exception.BusinessException;
import com.ondo.common.exception.ErrorCode;
import com.ondo.journal.domain.DailyJournal;
import com.ondo.journal.domain.JournalMemoLink;
import com.ondo.memo.MemoRepository;
import com.ondo.memo.domain.CurriculumArea;
import com.ondo.memo.domain.Memo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 일지 결과 저장 전담(REQUIRES_NEW). AI 호출이 끝난 뒤에만 짧게 TX 를 연다 — 오케스트레이션(JournalService)이
 * '주입받아' 호출하므로 self-invocation 회피(프록시 적용). 정본: architecture L272, ai-integration-spec §7.
 */
@Service
public class JournalPersistService {

    private final DailyJournalRepository dailyJournalRepository;
    private final JournalMemoLinkRepository journalMemoLinkRepository;
    private final MemoRepository memoRepository;

    public JournalPersistService(DailyJournalRepository dailyJournalRepository,
                                 JournalMemoLinkRepository journalMemoLinkRepository,
                                 MemoRepository memoRepository) {
        this.dailyJournalRepository = dailyJournalRepository;
        this.journalMemoLinkRepository = journalMemoLinkRepository;
        this.memoRepository = memoRepository;
    }

    /**
     * daily_journal 초안 저장 + 각 메모 영역 자동 분류(FR-2) + journal_memo_link 기록(추적성).
     * 메모는 이 TX 안에서 재조회해 dirty checking 으로 갱신한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult save(Long teacherId, Long classroomId, LocalDate date, String contentJson,
                              List<Long> memoIds, Map<Long, CurriculumArea> memoIdToArea, LocalDateTime analyzedAt) {
        DailyJournal journal;
        try {
            // saveAndFlush 로 UNIQUE(teacher,classroom,date) 위반을 여기서 잡아 계약 코드로 변환한다.
            // (선검사-삽입 사이 경합·다중 인스턴스에서 generic DATA_CONFLICT 대신 JOURNAL_ALREADY_EXISTS 보장)
            journal = dailyJournalRepository.saveAndFlush(
                    DailyJournal.createDraft(teacherId, classroomId, date, contentJson, analyzedAt));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.JOURNAL_ALREADY_EXISTS);
        }

        // 저장 시점에도 살아있는 메모만 영역 갱신 + 링크 — 분석 후 soft-delete 된 메모가 끼는 드리프트 방지.
        Set<Long> activeIds = new HashSet<>();
        for (Memo memo : memoRepository.findAllById(memoIds)) {
            activeIds.add(memo.getId());
            CurriculumArea area = memoIdToArea.get(memo.getId());
            if (area != null) {
                memo.changeCurriculumArea(area); // 영속 상태 → flush 시 UPDATE
            }
        }

        // 원래 묶음 순서(id asc) 유지, 살아있는 메모만 추적 링크 기록
        List<Long> linkedMemoIds = memoIds.stream().filter(activeIds::contains).toList();
        for (Long memoId : linkedMemoIds) {
            journalMemoLinkRepository.save(JournalMemoLink.of(journal.getId(), memoId));
        }

        return new PersistResult(journal.getId(), journal.getAnalyzedAt(), linkedMemoIds);
    }

    /**
     * 재분석 덮어쓰기(Story 3.7): 같은 daily_journal 행 UPDATE(content·DRAFT·analyzedAt) + journal_memo_link 재구성
     * (기존 삭제 후 현재 active 묶음으로 재삽입) + 메모 영역 재분류. REQUIRES_NEW.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistResult overwrite(Long journalId, String contentJson,
                                   List<Long> memoIds, Map<Long, CurriculumArea> memoIdToArea, LocalDateTime analyzedAt) {
        DailyJournal journal = dailyJournalRepository.findById(journalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.JOURNAL_NOT_FOUND));
        journal.reanalyze(contentJson, analyzedAt);

        journalMemoLinkRepository.deleteByDailyJournalId(journalId); // 링크 재구성: 기존 삭제

        Set<Long> activeIds = new HashSet<>();
        for (Memo memo : memoRepository.findAllById(memoIds)) {
            activeIds.add(memo.getId());
            CurriculumArea area = memoIdToArea.get(memo.getId());
            if (area != null) {
                memo.changeCurriculumArea(area);
            }
        }
        List<Long> linkedMemoIds = memoIds.stream().filter(activeIds::contains).toList();
        for (Long memoId : linkedMemoIds) {
            journalMemoLinkRepository.save(JournalMemoLink.of(journalId, memoId));
        }
        return new PersistResult(journal.getId(), journal.getAnalyzedAt(), linkedMemoIds);
    }

    public record PersistResult(Long journalId, LocalDateTime analyzedAt, List<Long> linkedMemoIds) {
    }
}
