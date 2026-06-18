<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const { isDesktop } = useViewport()
const classroomId = session.classroom?.id

const FIELDS = [
  { key: 'playActivity', label: '놀이' },
  { key: 'interaction', label: '의사소통·상호작용' },
  { key: 'attitude', label: '수업태도' },
]

function parseChildId(v) {
  const n = Number(v)
  return Number.isInteger(n) && n > 0 ? n : null
}

const children = ref([])
const selectedId = ref(parseChildId(route.query.childId))
const form = reactive({ playActivity: '', interaction: '', attitude: '' })
const openField = ref(null)
const loading = ref(true)
const saving = ref(false)
const error = ref('')
const toast = ref('')
let navTimer = null

const today = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
const selectedChild = computed(() => children.value.find((c) => c.id === selectedId.value) || null)
const canSave = computed(() => !!selectedChild.value && FIELDS.some((f) => form[f.key].trim()))
function shortName(n) { return n ? n.slice(n.length > 2 ? 1 : 0) : '아이' }

async function load() {
  loading.value = true
  try {
    children.value = await api.get(`/classrooms/${classroomId}/children`)
    if (selectedId.value && !children.value.some((c) => c.id === selectedId.value)) selectedId.value = null
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '아이 목록을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!canSave.value || saving.value) return
  error.value = ''
  saving.value = true
  try {
    await api.post('/memos', {
      childId: selectedId.value,
      playActivity: form.playActivity || null,
      interaction: form.interaction || null,
      attitude: form.attitude || null,
    })
    toast.value = `${shortName(selectedChild.value?.name)} 페이지에 저장됐어요`
    const id = selectedId.value
    navTimer = setTimeout(() => router.push({ name: 'timeline', params: { childId: id } }), 1100)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '저장 중 문제가 발생했어요.'
    saving.value = false
  }
}

onMounted(load)
onBeforeUnmount(() => { if (navTimer) clearTimeout(navTimer) })
</script>

<template>
  <div :class="isDesktop ? 'dt-page memo-dt' : 'memo-m'">
    <!-- 헤더 -->
    <template v-if="isDesktop">
      <div class="dt-title-row">
        <span class="jr-display">빠른 메모</span>
        <span class="dt-tip">아이 선택 → 내용 입력 → 저장, 세 동작이면 끝</span>
      </div>
      <div class="dt-date">{{ today }} · {{ session.classroom?.name }}</div>
    </template>
    <header v-else class="m-head screen">
      <button class="back" @click="router.back()"><AppIcon name="back" :size="24" /></button>
      <span class="jr-h1 ttl">빠른 메모</span>
      <span class="hint"><AppIcon name="clock" :size="15" /> 10초면 충분해요</span>
    </header>

    <div :class="isDesktop ? 'memo-dt-inner' : 'screen body'">
      <!-- 1. 아이 선택 -->
      <div class="step">
        <span class="step-n">1</span><span class="step-t">아이 선택</span>
        <span class="step-hint">{{ isDesktop ? '반 전체가 한눈에' : '옆으로 밀어 더 보기' }}</span>
      </div>
      <div class="pick-wrap" :class="{ dt: isDesktop }">
        <div class="pick-row" :class="{ dt: isDesktop }">
          <button v-for="c in children" :key="c.id" class="pick" :class="{ lg: isDesktop }" @click="selectedId = c.id">
            <span class="pick-ava" :class="{ on: c.id === selectedId }">
              <Avatar :name="c.name" size="lg" />
              <span v-if="c.id === selectedId" class="pick-chk"><AppIcon name="check" :size="11" :stroke="3.4" /></span>
            </span>
            <span class="pick-name" :class="{ on: c.id === selectedId }">{{ c.name }}</span>
          </button>
          <p v-if="children.length === 0" class="muted" style="padding:8px 0">먼저 아이를 등록해 주세요.</p>
        </div>
      </div>

      <!-- 2. 내용 입력 (+ 데스크톱 저장 카드) -->
      <div :class="{ 'dt-grid': isDesktop }">
        <div class="col-input">
          <div class="step">
            <span class="step-n">2</span><span class="step-t">내용 입력</span>
            <span class="step-hint">칸을 누르면 크게</span>
          </div>
          <div class="jr-card guide">
            <div v-for="(f, i) in FIELDS" :key="f.key" class="jr-guide-row" @click="openField = i">
              <span class="lab">{{ f.label }}</span>
              <span class="inp" :class="{ empty: !form[f.key] }">{{ form[f.key] || '톡톡 적어요' }}</span>
              <AppIcon name="expand" :size="17" style="color:var(--text-faint);flex:0 0 auto;margin-left:8px" />
            </div>
          </div>
          <p v-if="error" class="err">{{ error }}</p>
        </div>

        <!-- 데스크톱: 우측 저장 카드 -->
        <div v-if="isDesktop" class="col-save">
          <div style="height:31px" />
          <div class="jr-card jr-card--soft save-card">
            <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" :disabled="!canSave || saving" @click="save">
              <AppIcon name="check" :size="22" :stroke="2.6" /> 저장하기
            </button>
            <p v-if="!canSave" class="save-hint"><AppIcon name="help" :size="15" /> 아이를 고르고 내용을 한 줄만 적어주세요</p>
            <p v-else class="save-note">저장하면 <b>{{ shortName(selectedChild?.name) }} 페이지</b>에<br />바로 기록돼요 · 영역은 밤에 자동 분류</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 모바일: 하단 고정 저장 -->
    <div v-if="!isDesktop" class="footer">
      <p v-if="!canSave" class="save-hint"><AppIcon name="help" :size="15" /> 아이를 고르고 내용을 한 줄만 적어주세요</p>
      <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" :disabled="!canSave || saving" @click="save">
        <AppIcon name="check" :size="22" :stroke="2.6" />
        {{ selectedChild ? shortName(selectedChild.name) + ' 메모 저장' : '메모 저장' }}
      </button>
    </div>

    <!-- 입력칸 확대 모달 -->
    <div v-if="openField !== null" class="overlay" :class="{ dt: isDesktop }" @click.self="openField = null">
      <div class="sheet" :class="{ dt: isDesktop }">
        <div class="sheet-top">
          <Avatar :name="selectedChild?.name || '아이'" size="sm" />
          <div class="sheet-title">
            <div class="t">{{ shortName(selectedChild?.name) }} · {{ FIELDS[openField].label }}</div>
            <div class="d">크게 보고 편하게 적어요</div>
          </div>
          <button class="close" @click="openField = null"><AppIcon name="x" :size="18" /></button>
        </div>
        <textarea
          v-model="form[FIELDS[openField].key]" class="jr-textarea" rows="5" autofocus
          :placeholder="`${FIELDS[openField].label}에서 관찰한 모습을 적어요`" style="font-size:16px;line-height:1.6"
        />
        <button class="jr-btn jr-btn--primary jr-btn--block" style="margin-top:16px" @click="openField = null">
          <AppIcon name="check" :size="20" :stroke="2.6" /> 완료
        </button>
      </div>
    </div>

    <!-- 저장 토스트 -->
    <Transition name="fade">
      <div v-if="toast" class="toast-wrap" :class="{ dt: isDesktop }">
        <div class="jr-toast"><span class="tick"><AppIcon name="check" :size="14" :stroke="3" /></span>{{ toast }}</div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.memo-m { display: flex; flex-direction: column; min-height: 100%; }
.memo-dt-inner { max-width: 760px; }
.memo-dt { position: relative; }

/* 데스크톱 헤더 */
.dt-title-row { display: flex; align-items: baseline; gap: 12px; }
.dt-tip { font-size: 14px; color: var(--text-sub); }
.dt-date { font-size: 14px; color: var(--text-faint); margin: 4px 0 28px; }

/* 모바일 헤더 */
.m-head { display: flex; align-items: center; gap: 8px; padding-top: 10px; padding-bottom: 14px; }
.back { border: none; background: transparent; color: var(--text); cursor: pointer; padding: 4px; }
.ttl { font-size: 21px; }
.hint { margin-left: auto; font-size: 13px; color: var(--text-faint); display: flex; align-items: center; gap: 4px; }
.body { flex: 1; padding-bottom: 16px; }

.step { display: flex; align-items: center; gap: 9px; margin-bottom: 13px; }
.step-n { width: 22px; height: 22px; border-radius: 50%; background: var(--brand-500); color: var(--text); font-size: 12px; font-weight: 800; display: flex; align-items: center; justify-content: center; flex: 0 0 auto; }
.step-t { font-size: 18px; font-weight: 700; }
.step-hint { margin-left: auto; font-size: 12px; color: var(--text-faint); }

.pick-wrap { position: relative; margin-bottom: 26px; }
.pick-wrap.dt { margin-bottom: 30px; }
.pick-row { display: flex; gap: 14px; overflow-x: auto; padding-bottom: 4px; }
.pick-row.dt { flex-wrap: wrap; gap: 18px; overflow: visible; }
.pick { flex: 0 0 auto; display: flex; flex-direction: column; align-items: center; gap: 6px; width: 64px; border: none; background: transparent; cursor: pointer; font-family: inherit; }
.pick.lg { width: 72px; }
.pick-ava { position: relative; border-radius: 50%; padding: 3px; background: transparent; }
.pick-ava.on { background: var(--brand-500); box-shadow: 0 4px 12px rgba(245, 185, 64, .4); }
.pick-chk { position: absolute; right: -2px; bottom: -2px; width: 20px; height: 20px; border-radius: 50%; background: var(--success); border: 2px solid var(--surface); display: flex; align-items: center; justify-content: center; color: #fff; }
.pick-name { font-size: 12px; font-weight: 600; color: var(--text-sub); white-space: nowrap; }
.pick-name.on { font-weight: 800; color: var(--text); }

.dt-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 22px; align-items: start; }
.guide { padding: 4px 18px; }
.guide .inp { flex: 1; min-width: 0; color: var(--text); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; padding: 4px 0; font-size: 15px; }
.guide .inp.empty { color: var(--text-faint); }
.save-card { padding: 18px; text-align: center; }
.save-note { font-size: 13px; color: var(--text-faint); margin-top: 12px; line-height: 1.5; }
.save-note b { color: var(--text-sub); }

.footer { padding: 14px 20px 24px; }
.save-hint { display: flex; align-items: center; justify-content: center; gap: 6px; font-size: 13px; color: var(--text-faint); font-weight: 600; margin: 0 0 10px; }
.err { color: var(--warn); font-weight: 600; font-size: 13.5px; padding: 0 2px; }
.muted { color: var(--text-sub); }

/* 모달 */
.overlay { position: fixed; inset: 0; z-index: 30; background: rgba(40, 30, 20, .34); display: flex; align-items: flex-end; justify-content: center; }
.overlay.dt { align-items: center; padding: 40px; }
.sheet { background: var(--surface); width: 100%; max-width: 520px; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
.sheet.dt { border-radius: 24px; padding: 26px 28px; }
.sheet-top { display: flex; align-items: center; gap: 10px; margin-bottom: 16px; }
.sheet-title .t { font-size: 16px; font-weight: 800; }
.sheet-title .d { font-size: 12px; color: var(--text-sub); }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }

.toast-wrap { position: fixed; left: 20px; right: 20px; bottom: 110px; z-index: 40; display: flex; justify-content: center; }
.toast-wrap.dt { left: 50%; right: auto; transform: translateX(-50%); width: 440px; bottom: 48px; }
.toast-wrap .jr-toast { width: 100%; justify-content: center; }
</style>
