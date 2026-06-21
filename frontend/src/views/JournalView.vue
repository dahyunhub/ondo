<script setup>
// 일지·분석 허브 + 하루 일지 생성/검토 플로우(Epic 3, Story 3.5~3.7 연결).
// 백엔드: POST /journals/analyze · GET /journals?classroomId&date · PUT /journals/{id} · POST /journals/{id}/analyze
// 한 아이 분석(A)은 Epic 4 범위라 아직 안내만 한다. "반 일지 목록" 전용 엔드포인트는 없어
// 허브의 최근 목록은 '오늘 일지' 단건만 노출한다(MVP, 옵션 A).
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { api, ApiError } from '../lib/api'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import { AREA_ORDER, AREA_META } from '../lib/areas'
import AppIcon from '../components/AppIcon.vue'
import NuriChip from '../components/NuriChip.vue'
import SproutLoader from '../components/SproutLoader.vue'

const { isDesktop } = useViewport()
const classroomId = session.classroom?.id

function isoToday() {
  const d = new Date()
  const z = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${z(d.getMonth() + 1)}-${z(d.getDate())}`
}
const todayIso = isoToday()
const todayLabel = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

// 화면 상태: hub | loading | draft | busy | error
const view = ref('hub')
const journal = ref(null) // { id, status, content, analyzedAt, reanalysisNeeded, newMemoIds }
const editing = ref(false)
const editContent = reactive({})
const showOverwrite = ref(false)
const errorMsg = ref('')
const saving = ref(false)
const toast = ref('')
let toastTimer = null

// 요약 + 누리 5영역 — 저장/표시 순서
const BLOCKS = ['summary', ...AREA_ORDER]
const SUMMARY_LABEL = '요약'
function stripColor(key) {
  return key === 'summary' ? 'var(--brand-500)' : (AREA_META[key]?.color || 'var(--brand-500)')
}

const isConfirmed = computed(() => journal.value?.status === 'CONFIRMED')
const newMemoCount = computed(() => journal.value?.newMemoIds?.length || 0)

function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2000)
}

function setJournal(j) {
  journal.value = {
    id: j.id,
    status: j.status,
    content: j.content || {},
    analyzedAt: j.analyzedAt,
    reanalysisNeeded: j.reanalysisNeeded ?? false,
    newMemoIds: j.newMemoIds ?? [],
  }
}

// 진입 시 오늘 일지가 이미 있는지 확인(없으면 허브 빈 상태 유지).
async function loadExisting() {
  try {
    const res = await api.get(`/journals?classroomId=${classroomId}&date=${todayIso}`)
    setJournal(res)
  } catch (e) {
    if (e instanceof ApiError && e.code === 'JOURNAL_NOT_FOUND') return
    // 그 외 오류는 허브를 그대로 두고 조용히 넘긴다(치명적 아님).
  }
}

function openDraft() {
  if (!journal.value) return
  editing.value = false
  view.value = 'draft'
}

// 오늘 메모 → 일지 생성. 이미 있으면 열기만.
async function startAnalyze() {
  if (journal.value) { openDraft(); return }
  view.value = 'loading'
  try {
    const res = await api.post('/journals/analyze', { classroomId, date: todayIso })
    setJournal(res)
    view.value = 'draft'
  } catch (e) {
    await handleAnalyzeError(e)
  }
}

async function handleAnalyzeError(e) {
  const code = e instanceof ApiError ? e.code : null
  if (code === 'JOURNAL_ALREADY_EXISTS') {
    await loadExisting()
    if (journal.value) { view.value = 'draft'; return }
  }
  if (code === 'ANALYSIS_IN_PROGRESS') { view.value = 'busy'; return }
  errorMsg.value = e instanceof ApiError ? e.message : '분석 중 문제가 발생했어요.'
  view.value = 'error'
}

function askReanalyze() { showOverwrite.value = true }

async function doReanalyze() {
  showOverwrite.value = false
  view.value = 'loading'
  try {
    const res = await api.post(`/journals/${journal.value.id}/analyze`)
    setJournal(res)
    editing.value = false
    view.value = 'draft'
  } catch (e) {
    await handleAnalyzeError(e)
  }
}

function startEdit() {
  for (const k of BLOCKS) editContent[k] = journal.value.content[k] ?? ''
  editing.value = true
}
function cancelEdit() { editing.value = false }

function collectContent(source) {
  const out = {}
  for (const k of BLOCKS) out[k] = (source[k] ?? '').trim()
  return out
}

// content·status 저장(AI 미사용). 빈 영역이 있으면 막는다(백엔드 계약).
async function persist(status, content) {
  if (BLOCKS.some((k) => !content[k])) { showToast('빈 칸을 모두 채워 주세요.'); return false }
  if (saving.value) return false
  saving.value = true
  try {
    const res = await api.put(`/journals/${journal.value.id}`, { content, status })
    journal.value = { ...journal.value, content: res.content, status: res.status, analyzedAt: res.analyzedAt }
    return true
  } catch (e) {
    showToast(e instanceof ApiError ? e.message : '저장 중 문제가 발생했어요.')
    return false
  } finally {
    saving.value = false
  }
}

async function saveEdit() {
  if (await persist(journal.value.status, collectContent(editContent))) {
    editing.value = false
    showToast('저장됐어요')
  }
}

async function confirmJournal() {
  const content = editing.value ? collectContent(editContent) : collectContent(journal.value.content)
  if (await persist('CONFIRMED', content)) {
    editing.value = false
    showToast('일지를 확정했어요')
  }
}

function backToHub() { view.value = 'hub'; editing.value = false }
function soon() { showToast('한 아이 분석은 곧 제공돼요 (Epic 4 준비 중)') }

onMounted(loadExisting)
onBeforeUnmount(() => clearTimeout(toastTimer))
</script>

<template>
  <div :class="isDesktop ? 'dt-page jh-dt' : 'jh-m'">
    <!-- ===== 허브 ===== -->
    <template v-if="view === 'hub'">
      <template v-if="isDesktop">
        <div class="jr-display" style="margin-bottom:6px">일지·분석</div>
        <div class="dt-sub" style="margin-bottom:26px">{{ todayLabel }} · {{ session.classroom?.name }}</div>
      </template>
      <header v-else class="m-head screen"><span class="jr-h1">일지·분석</span></header>

      <div :class="isDesktop ? '' : 'screen body'">
        <div class="cta" :class="{ dt: isDesktop }" @click="startAnalyze">
          <span class="cta-ic"><AppIcon name="sparkle" :size="26" /></span>
          <div class="cta-tx">
            <div class="t">{{ journal ? '오늘 일지 보기' : '오늘 메모로 일지 쓰기' }}</div>
            <div class="d">{{ journal ? '만든 일지를 검토·확정해요' : '하루 일지를 바로 만들어요' }}</div>
          </div>
          <AppIcon name="chevR" :size="22" />
        </div>

        <div class="ask">어떤 걸 분석할까요?</div>
        <div class="analysis" :class="{ dt: isDesktop }">
          <button class="abig" @click="soon">
            <div class="ab-top"><span class="ab-ic" style="background:rgba(201,168,232,.3)"><AppIcon name="me" :size="24" /></span>
              <div><div class="ab-t"><span class="ab-id">A</span>한 아이 분석</div><div class="ab-sub">개인 관찰 평가</div></div></div>
            <div class="ab-desc">한 아이의 기록을 모아 상담·발달평가용 관찰 평가를 만들어요.</div>
            <div class="ab-go">곧 제공 <span class="ab-badge">Epic 4</span></div>
          </button>
          <button class="abig" @click="startAnalyze">
            <div class="ab-top"><span class="ab-ic" style="background:var(--brand-300)"><AppIcon name="journal" :size="24" /></span>
              <div><div class="ab-t"><span class="ab-id">B</span>오늘 전체 분석</div><div class="ab-sub">하루 일지</div></div></div>
            <div class="ab-desc">오늘 반 전체 메모로 매일 제출하는 일지를 만들어요.</div>
            <div class="ab-go">{{ journal ? '오늘 일지 열기' : '바로 시작' }} <AppIcon name="chevR" :size="16" /></div>
          </button>
        </div>

        <div class="recent-h"><span class="jr-h2" :style="isDesktop ? '' : 'font-size:18px'">최근 만든 일지</span></div>
        <button v-if="journal" class="recent-card" @click="openDraft">
          <span class="rc-ic"><AppIcon name="journal" :size="22" /></span>
          <div class="rc-body">
            <div class="rc-t">오늘 하루 일지</div>
            <div class="rc-d">{{ todayLabel }}</div>
          </div>
          <span class="rc-status" :class="isConfirmed ? 'on' : ''">{{ isConfirmed ? '확정' : 'AI 초안' }}</span>
          <AppIcon name="chevR" :size="20" />
        </button>
        <div v-else class="recent-empty">
          <AppIcon name="journal" :size="26" style="color:var(--text-faint)" />
          <div class="re-t">아직 만든 일지가 없어요</div>
          <div class="re-d">메모가 쌓이면 AI가 일지를 만들어 여기에 모아드릴게요.</div>
        </div>
      </div>
    </template>

    <!-- ===== 로딩 ===== -->
    <div v-else-if="view === 'loading'" class="state-center">
      <SproutLoader :size="isDesktop ? 132 : 116" />
      <div class="jr-h1" style="margin-top:22px;text-align:center">오늘 메모를 모아<br>일지를 쓰고 있어요</div>
      <div class="state-sub">잠시만요 — 약 15초 걸려요 ☀️</div>
      <div class="dots"><span v-for="i in 3" :key="i" class="jr-dot-pulse" :style="{ animationDelay: (i - 1) * 0.22 + 's' }" /></div>
      <div class="state-pill"><AppIcon name="check" :size="15" :stroke="2.6" style="color:var(--success)" /> 메모는 안전하게 저장돼 있어요</div>
    </div>

    <!-- ===== 동시 분석 진행 중 ===== -->
    <div v-else-if="view === 'busy'" class="state-center">
      <div class="state-circle brand"><AppIcon name="clock" :size="42" style="color:var(--brand-700)" /></div>
      <div class="jr-h1" style="text-align:center">분석이 이미 진행 중이에요</div>
      <div class="state-sub">지금 만들고 있는 일지가 끝나면<br>이어서 진행할 수 있어요.</div>
      <button class="jr-btn jr-btn--secondary" style="margin-top:24px" @click="backToHub">돌아가기</button>
    </div>

    <!-- ===== 에러 ===== -->
    <div v-else-if="view === 'error'" class="state-center">
      <div class="state-circle warn"><AppIcon name="heart" :size="42" :stroke="2" style="color:var(--warn)" /></div>
      <div class="jr-h1" style="text-align:center">앗, 분석이 잠시 멈췄어요</div>
      <div class="state-sub">{{ errorMsg }}</div>
      <div class="jr-banner jr-banner--warn err-banner">
        <AppIcon name="check" :size="26" :stroke="2.4" style="color:var(--warn);flex:0 0 auto" />
        <div>
          <div class="eb-t">작성하신 메모는 그대로 저장돼 있어요.</div>
          <div class="eb-d">하나도 사라지지 않았으니 안심하세요.</div>
        </div>
      </div>
      <div class="err-actions">
        <button class="jr-btn jr-btn--primary" style="flex:1" @click="startAnalyze"><AppIcon name="swap" :size="20" /> 다시 시도</button>
        <button class="jr-btn jr-btn--secondary" style="flex:1" @click="backToHub">돌아가기</button>
      </div>
    </div>

    <!-- ===== 초안 검토·수정 ===== -->
    <template v-else-if="view === 'draft' && journal">
      <div class="draft-head">
        <button class="back-btn" @click="backToHub"><AppIcon name="back" :size="24" /></button>
        <div>
          <div class="jr-h1" :style="isDesktop ? '' : 'font-size:21px'">하루 일지</div>
          <div class="dh-sub">{{ todayLabel }} · {{ session.classroom?.name }} · {{ isConfirmed ? '확정됨' : 'AI 초안' }}</div>
        </div>
      </div>

      <div :class="isDesktop ? '' : 'screen body'" class="draft-body">
        <!-- 재분석 안내 -->
        <div v-if="journal.reanalysisNeeded && !editing" class="jr-banner reanalysis">
          <AppIcon name="sparkle" :size="22" style="color:var(--brand-700);flex:0 0 auto" />
          <span>새 메모 {{ newMemoCount }}건 — 재분석이 필요해요<br><span class="rb-sub">추가된 메모를 반영해 다시 만들까요?</span></span>
        </div>

        <div v-if="!editing" class="edit-hint"><AppIcon name="pencil" :size="13" /> 수정 버튼으로 내용을 다듬을 수 있어요</div>

        <div class="blocks">
          <div v-for="key in BLOCKS" :key="key" class="jr-area-block" :style="{ '--strip': stripColor(key) }">
            <div class="ab-head">
              <NuriChip v-if="key !== 'summary'" :area="key" />
              <span v-else class="sum-chip">{{ SUMMARY_LABEL }}</span>
            </div>
            <textarea v-if="editing" v-model="editContent[key]" class="ab-edit" rows="3" />
            <div v-else class="ab-text">{{ journal.content[key] }}</div>
          </div>
        </div>
      </div>

      <div class="actions" :class="{ dt: isDesktop }">
        <template v-if="editing">
          <button class="jr-btn jr-btn--ghost" :disabled="saving" @click="cancelEdit">취소</button>
          <button class="jr-btn jr-btn--primary" style="flex:1" :disabled="saving" @click="saveEdit"><AppIcon name="check" :size="20" :stroke="2.6" /> 저장</button>
        </template>
        <template v-else>
          <button class="jr-btn jr-btn--ghost" @click="startEdit"><AppIcon name="pencil" :size="19" /> 수정</button>
          <button class="jr-btn jr-btn--secondary" @click="askReanalyze"><AppIcon name="swap" :size="19" /> 다시</button>
          <button class="jr-btn jr-btn--primary" style="flex:1" :disabled="saving" @click="confirmJournal">
            <AppIcon name="check" :size="21" :stroke="2.6" /> {{ isConfirmed ? '다시 확정' : '확정' }}
          </button>
        </template>
      </div>
    </template>

    <!-- ===== 덮어쓰기 확인 모달 ===== -->
    <Transition name="fade">
      <div v-if="showOverwrite" class="modal-mask" @click.self="showOverwrite = false">
        <div class="modal">
          <div class="modal-ic"><AppIcon name="swap" :size="28" style="color:var(--brand-700)" /></div>
          <div class="jr-h2">기존 일지를 덮어쓸까요?</div>
          <div class="modal-d">다시 분석하면 지금 일지가 새 결과로 바뀌어요.<br>원본 메모는 그대로예요.</div>
          <div class="modal-actions">
            <button class="jr-btn jr-btn--secondary" style="flex:1" @click="showOverwrite = false">취소</button>
            <button class="jr-btn jr-btn--primary" style="flex:1" @click="doReanalyze"><AppIcon name="swap" :size="19" /> 덮어쓰고 분석</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div v-if="toast" class="toast-wrap" :class="{ dt: isDesktop }"><div class="jr-toast">{{ toast }}</div></div>
    </Transition>
  </div>
</template>

<style scoped>
.jh-m { display: flex; flex-direction: column; }
.jh-dt { max-width: 760px; }
.m-head { padding-top: 6px; padding-bottom: 12px; }
.body { padding-bottom: 28px; }
.dt-sub { font-size: 15px; color: var(--text-sub); }

.cta { display: flex; align-items: center; gap: 14px; padding: 16px 18px; border-radius: 20px; cursor: pointer; background: linear-gradient(135deg, #FFD45E, #F5B940); box-shadow: 0 8px 22px rgba(245, 185, 64, .4); color: var(--text); margin-bottom: 18px; }
.cta.dt { max-width: 520px; }
.cta-ic { width: 46px; height: 46px; border-radius: 14px; flex: 0 0 auto; background: rgba(255, 255, 255, .45); display: flex; align-items: center; justify-content: center; }
.cta-tx { flex: 1; }
.cta-tx .t { font-size: 16px; font-weight: 800; }
.cta-tx .d { font-size: 13px; color: #7a5e22; font-weight: 600; margin-top: 2px; }

.ask { font-size: 13px; font-weight: 800; color: var(--text-sub); margin-bottom: 10px; }
.analysis { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.analysis.dt { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; max-width: 760px; }
.abig {
  display: flex; flex-direction: column; gap: 12px; padding: 20px; border-radius: var(--r-card); cursor: pointer;
  background: var(--surface); border: 1.5px solid var(--hair); box-shadow: var(--shadow); font-family: inherit; text-align: left;
}
.ab-top { display: flex; align-items: center; gap: 12px; }
.ab-ic { width: 48px; height: 48px; border-radius: 14px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; color: var(--text); }
.ab-t { font-size: 16.5px; font-weight: 800; }
.ab-id { color: var(--text-faint); margin-right: 5px; }
.ab-sub { font-size: 12.5px; color: var(--text-sub); font-weight: 600; }
.ab-desc { font-size: 13.5px; color: var(--text-sub); line-height: 1.55; }
.ab-go { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 800; color: var(--text-faint); }
.ab-badge { font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }

.recent-h { margin-bottom: 14px; }
.recent-empty {
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px;
  padding: 32px 20px; border-radius: var(--r-card); background: var(--surface); box-shadow: var(--shadow-sm); max-width: 760px;
}
.re-t { font-size: 15px; font-weight: 800; color: var(--text-sub); }
.re-d { font-size: 13px; color: var(--text-faint); line-height: 1.5; }

.recent-card {
  display: flex; align-items: center; gap: 13px; width: 100%; max-width: 760px; padding: 15px 18px; cursor: pointer;
  border-radius: var(--r-card); background: var(--surface); border: 1.5px solid var(--hair); box-shadow: var(--shadow-sm); font-family: inherit; text-align: left;
}
.rc-ic { width: 42px; height: 42px; border-radius: 12px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; background: var(--brand-100); color: var(--brand-700); }
.rc-body { flex: 1; min-width: 0; }
.rc-t { font-size: 15px; font-weight: 800; }
.rc-d { font-size: 12.5px; color: var(--text-sub); margin-top: 2px; }
.rc-status { font-size: 11.5px; font-weight: 800; padding: 4px 10px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); white-space: nowrap; }
.rc-status.on { background: var(--brand-100); color: var(--brand-700); }

/* 상태 화면(로딩/busy/error) */
.state-center { min-height: 60vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 30px; }
.state-sub { font-size: 15px; color: var(--text-sub); margin-top: 10px; text-align: center; line-height: 1.5; }
.dots { display: flex; gap: 8px; margin-top: 22px; }
.dots .jr-dot-pulse { width: 9px; height: 9px; border-radius: 50%; background: var(--brand-500); }
.state-pill { margin-top: 30px; display: flex; align-items: center; gap: 7px; font-size: 13px; color: var(--text-faint); background: var(--surface-soft); padding: 9px 16px; border-radius: 999px; }
.state-circle { width: 88px; height: 88px; border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-bottom: 20px; }
.state-circle.brand { background: var(--brand-100); }
.state-circle.warn { background: rgba(240, 140, 125, .14); }
.err-banner { margin-top: 24px; padding: 18px 20px; border-radius: 20px; max-width: 360px; }
.eb-t { font-size: 16px; font-weight: 800; }
.eb-d { font-size: 13px; color: var(--text-sub); margin-top: 3px; }
.err-actions { display: flex; gap: 10px; margin-top: 26px; width: 100%; max-width: 360px; }

/* 초안 검토 */
.draft-head { display: flex; align-items: center; gap: 8px; padding-bottom: 14px; }
.back-btn { border: none; background: transparent; color: var(--text); cursor: pointer; padding: 4px; display: flex; }
.dh-sub { font-size: 13px; color: var(--text-sub); margin-top: 2px; }
.draft-body { display: flex; flex-direction: column; }
.reanalysis { margin-bottom: 16px; }
.rb-sub { font-weight: 600; color: var(--text-sub); }
.edit-hint { font-size: 12.5px; color: var(--text-faint); display: flex; align-items: center; gap: 5px; margin-bottom: 12px; }
.blocks { display: flex; flex-direction: column; gap: 12px; }
.ab-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.sum-chip { font-size: 12.5px; font-weight: 800; color: var(--brand-700); }
.ab-text { font-size: 14.5px; line-height: 1.6; color: var(--text); white-space: pre-wrap; }
.ab-edit {
  width: 100%; font-family: inherit; font-size: 14.5px; line-height: 1.6; color: var(--text); resize: vertical;
  border: 1.5px solid var(--hair-strong); border-radius: 12px; padding: 10px 12px; background: var(--surface-soft); min-height: 76px;
}
.ab-edit:focus { outline: none; border-color: var(--brand-500); background: var(--surface); }

.actions { display: flex; gap: 9px; padding: 16px 0 26px; }
.actions.dt { max-width: 760px; justify-content: flex-end; }
.actions.dt .jr-btn--primary { flex: 0 0 auto !important; padding-left: 30px; padding-right: 30px; }

/* 모달 */
.modal-mask { position: fixed; inset: 0; z-index: 60; background: rgba(40, 30, 20, .36); display: flex; align-items: center; justify-content: center; padding: 24px; }
.modal { background: var(--surface); border-radius: 24px; padding: 28px 26px; width: 100%; max-width: 420px; box-shadow: var(--shadow-lg); text-align: center; }
.modal-ic { width: 56px; height: 56px; border-radius: 50%; background: var(--brand-100); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; }
.modal-d { font-size: 14px; color: var(--text-sub); line-height: 1.55; margin-top: 8px; }
.modal-actions { display: flex; gap: 10px; margin-top: 24px; }

.toast-wrap { position: fixed; left: 20px; right: 20px; bottom: 110px; z-index: 40; display: flex; justify-content: center; }
.toast-wrap.dt { left: 50%; right: auto; transform: translateX(-50%); bottom: 48px; }
.fade-enter-active, .fade-leave-active { transition: opacity .2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>
