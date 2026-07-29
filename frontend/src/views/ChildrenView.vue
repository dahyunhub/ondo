<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import SproutLoader from '../components/SproutLoader.vue'
import ChildFormModal from '../components/ChildFormModal.vue'

const router = useRouter()
const { isDesktop } = useViewport()
const classroomId = session.classroom?.id
const children = ref([])
const hiddenChildren = ref([])
const loading = ref(true)
const loadError = ref('')
const adding = ref(false)
const showHidden = ref(false)
const restoring = ref(false)
const restoreError = ref('')
const warmth = ref({}) // childId -> 'WARM' | 'LOW'. 비어 있으면 표시 안 함

function fmtBirth(d) { return d ? d.replaceAll('-', '.') : '' }

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    children.value = await api.get(`/classrooms/${classroomId}/children`)
  } catch (e) {
    loadError.value = e.message || '명단을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

// 숨긴 아이(soft delete) 명단 — 실패해도 본 화면은 정상(조용히 무시).
async function loadHidden() {
  try {
    hiddenChildren.value = await api.get(`/classrooms/${classroomId}/children/hidden`)
  } catch { /* 본 명단에 영향 없음 */ }
}

// 관찰 온도(spec-child-warmth) — 부가 정보라 실패해도 명단은 그대로 보여준다.
// enabled:false(콜드 스타트)면 아무 표시도 하지 않는다. 안내 문구조차 두지 않는다.
// 어느 경로로 끝나든 먼저 비운다 — 재조회(아이 추가·복원)로 대상이 늘어 콜드 스타트로 떨어지면
// 이전 판정의 링이 남아 "enabled:false 면 아무 표시도 안 한다"를 스스로 어기게 된다.
async function loadWarmth() {
  warmth.value = {}
  try {
    const res = await api.get(`/classrooms/${classroomId}/warmth`)
    if (!res?.enabled) return
    warmth.value = Object.fromEntries(res.items.map((i) => [i.childId, i.level]))
  } catch { /* 명단 렌더에 영향 없음 */ }
}

function openTimeline(c) { router.push({ name: 'timeline', params: { childId: c.id } }) }
function onSaved() { adding.value = false; load(); loadHidden(); loadWarmth() }

// 숨김 해제(복원) — 활성 명단으로 되돌린다.
async function restoreChild(c) {
  if (restoring.value) return
  restoring.value = true
  restoreError.value = ''
  try {
    await api.post(`/children/${c.id}/restore`)
    await Promise.all([load(), loadHidden(), loadWarmth()])
    if (!hiddenChildren.value.length) showHidden.value = false
  } catch (e) {
    restoreError.value = e.message || '복원 중 문제가 발생했어요.'
  } finally {
    restoring.value = false
  }
}

const count = computed(() => children.value.length)
const hiddenCount = computed(() => hiddenChildren.value.length)
onMounted(() => { load(); loadHidden(); loadWarmth() })
</script>

<template>
  <!-- ============ 데스크톱 ============ -->
  <div v-if="isDesktop" class="dt-page">
    <header class="dt-head">
      <div>
        <div class="jr-display">아이들</div>
        <div class="dt-sub">{{ session.classroom?.name }} · 가나다순 · {{ count }}명</div>
      </div>
      <div class="head-actions">
        <button v-if="hiddenCount" class="jr-btn jr-btn--ghost" @click="showHidden = true"><AppIcon name="search" :size="18" /> 숨긴 아이 {{ hiddenCount }}명</button>
        <button class="jr-btn jr-btn--primary" @click="adding = true"><AppIcon name="plus" :size="21" :stroke="2.6" /> 아이 등록하기</button>
      </div>
    </header>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <div v-else-if="loadError" class="err-box">
      <p class="err">{{ loadError }}</p>
      <button class="jr-btn jr-btn--secondary" @click="load"><AppIcon name="swap" :size="18" /> 다시 시도</button>
    </div>
    <div v-else-if="!count" class="empty-box">
      <SproutLoader :size="92" />
      <div class="empty-t">아직 등록된 아이가 없어요</div>
      <div class="empty-d">{{ session.classroom?.name }}의 첫 아이를 등록해 볼까요?</div>
      <button class="jr-btn jr-btn--primary" @click="adding = true"><AppIcon name="plus" :size="20" :stroke="2.6" /> 아이 등록하기</button>
    </div>
    <div v-else class="grid dt">
      <button v-for="c in children" :key="c.id" class="kid jr-card" @click="openTimeline(c)">
        <Avatar :name="c.name" size="lg" :photo-url="`/children/${c.id}/photo`" :photo-key="c.photoUpdatedAt || ''"
                :warmth="warmth[c.id] || ''" />
        <span class="kid-name">{{ c.name }}</span>
        <span class="kid-chip">{{ fmtBirth(c.birthDate) }} · {{ c.gender === 'MALE' ? '남' : '여' }}</span>
      </button>
    </div>
  </div>

  <!-- ============ 모바일 ============ -->
  <div v-else class="children-m">
    <header class="m-head screen">
      <div class="title-row">
        <span class="jr-h1">아이들</span>
        <span class="meta">{{ session.classroom?.name }} · {{ count }}명</span>
      </div>
      <div class="action-row">
        <span class="sort">가나다순</span>
        <div class="m-actions">
          <button v-if="hiddenCount" class="jr-btn jr-btn--ghost jr-btn--sm" @click="showHidden = true">
            <AppIcon name="search" :size="16" /> 숨긴 {{ hiddenCount }}
          </button>
          <button class="jr-btn jr-btn--primary add-btn" @click="adding = true">
            <AppIcon name="plus" :size="20" :stroke="2.6" /> 아이 등록하기
          </button>
        </div>
      </div>
    </header>

    <div class="screen list-wrap">
      <p v-if="loading" class="muted">불러오는 중…</p>
      <div v-else-if="loadError" class="err-box">
        <p class="err">{{ loadError }}</p>
        <button class="jr-btn jr-btn--secondary" @click="load"><AppIcon name="swap" :size="18" /> 다시 시도</button>
      </div>
      <div v-else-if="!count" class="empty-box">
        <SproutLoader :size="84" />
        <div class="empty-t">아직 등록된 아이가 없어요</div>
        <div class="empty-d">{{ session.classroom?.name }}의 첫 아이를 등록해 볼까요?</div>
        <button class="jr-btn jr-btn--primary" @click="adding = true"><AppIcon name="plus" :size="20" :stroke="2.6" /> 아이 등록하기</button>
      </div>
      <div v-else class="grid">
        <button v-for="c in children" :key="c.id" class="kid jr-card" @click="openTimeline(c)">
          <Avatar :name="c.name" size="lg" :photo-url="`/children/${c.id}/photo`" :photo-key="c.photoUpdatedAt || ''"
                  :warmth="warmth[c.id] || ''" />
          <span class="kid-name">{{ c.name }}</span>
          <span class="kid-chip">{{ fmtBirth(c.birthDate) }} · {{ c.gender === 'MALE' ? '남' : '여' }}</span>
        </button>
      </div>
    </div>
  </div>

  <ChildFormModal v-if="adding" mode="add" :classroom-id="classroomId" @close="adding = false" @saved="onSaved" />

  <!-- 숨긴 아이(명단에서 숨김) 보기 — 기록 참고용, 읽기 전용 -->
  <div v-if="showHidden" class="hidden-overlay" @click.self="showHidden = false">
    <div class="hidden-sheet">
      <div class="hidden-top">
        <span class="jr-h2">숨긴 아이</span>
        <button class="hidden-close" @click="showHidden = false"><AppIcon name="x" :size="18" /></button>
      </div>
      <div class="jr-banner hidden-note">
        <AppIcon name="check" :size="20" :stroke="2.4" style="color:var(--brand-700);flex:0 0 auto" />
        <span>명단에서만 숨겨졌고 <b>기록은 그대로 보존</b>돼 있어요. <b>복원</b>하면 다시 명단에 나타나요.</span>
      </div>
      <p v-if="restoreError" class="err" style="margin-bottom:12px">{{ restoreError }}</p>
      <div v-if="hiddenChildren.length" class="grid hidden-grid">
        <div v-for="c in hiddenChildren" :key="c.id" class="kid jr-card hidden-kid">
          <div class="hk-visual">
            <Avatar :name="c.name" size="lg" :photo-url="`/children/${c.id}/photo`" :photo-key="c.photoUpdatedAt || ''" />
            <span class="kid-name">{{ c.name }}</span>
            <span class="kid-chip">{{ fmtBirth(c.birthDate) }} · {{ c.gender === 'MALE' ? '남' : '여' }}</span>
          </div>
          <button class="restore-btn" :disabled="restoring" @click="restoreChild(c)">
            <AppIcon name="swap" :size="14" /> 복원
          </button>
        </div>
      </div>
      <p v-else class="muted" style="text-align:center;padding:24px">숨긴 아이가 없어요.</p>
    </div>
  </div>
</template>

<style scoped>
.dt-head { display: flex; align-items: flex-end; margin-bottom: 24px; }
.dt-sub { font-size: 15px; color: var(--text-sub); margin-top: 6px; white-space: nowrap; }
.head-actions { margin-left: auto; display: flex; gap: 10px; align-items: center; }

.children-m { display: flex; flex-direction: column; }
.m-head { padding-top: 12px; padding-bottom: 8px; }
.title-row { display: flex; align-items: baseline; gap: 10px; }
.meta { font-size: 14px; color: var(--text-sub); }
.action-row { display: flex; align-items: center; gap: 10px; margin-top: 14px; }
.sort { font-size: 13px; color: var(--text-faint); font-weight: 600; }
.m-actions { margin-left: auto; display: flex; gap: 8px; align-items: center; }
.add-btn { min-height: 44px; padding: 0 18px; }
.list-wrap { padding-top: 8px; padding-bottom: 28px; flex: 1; }

.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.grid.dt { grid-template-columns: repeat(5, 1fr); gap: 16px; }
.kid {
  display: flex; flex-direction: column; align-items: center; gap: 9px; padding: 20px 12px; cursor: pointer;
  text-align: center; border: none; font-family: inherit; background: var(--surface);
}
.kid-name { font-size: 15px; font-weight: 800; }
.kid-chip {
  display: inline-flex; align-items: center; font-size: 11.5px; font-weight: 700; padding: 5px 10px; border-radius: 999px;
  background: var(--surface-soft); color: var(--text-sub); font-variant-numeric: tabular-nums;
}
.muted { color: var(--text-sub); }
.err { color: var(--warn); font-weight: 600; }
.empty-box { display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px; padding: 40px 24px; }
.empty-t { font-size: 17px; font-weight: 800; color: var(--text-sub); margin-top: 16px; }
.empty-d { font-size: 14px; color: var(--text-faint); line-height: 1.5; margin-bottom: 14px; }
.err-box { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; padding: 8px 2px; }

/* 숨긴 아이 모달 */
.hidden-overlay { position: fixed; inset: 0; z-index: 40; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .hidden-overlay { align-items: center; padding: 24px; } }
.hidden-sheet { background: var(--surface); width: 100%; max-width: 560px; max-height: 88vh; overflow-y: auto; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
@media (min-width: 520px) { .hidden-sheet { border-radius: 24px; padding: 26px 28px; } }
.hidden-top { display: flex; align-items: center; margin-bottom: 14px; }
.hidden-close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }
.hidden-note { margin-bottom: 18px; font-size: 13px; font-weight: 600; line-height: 1.5; }
.hidden-grid { grid-template-columns: repeat(3, 1fr); }
@media (max-width: 519px) { .hidden-grid { grid-template-columns: 1fr 1fr; } }
.hidden-kid { cursor: default; gap: 12px; }
.hk-visual { display: flex; flex-direction: column; align-items: center; gap: 9px; opacity: .55; filter: grayscale(.5); }
.restore-btn { display: inline-flex; align-items: center; gap: 5px; border: 1.5px solid var(--brand-500); background: var(--brand-100); color: var(--brand-700); border-radius: 999px; padding: 6px 14px; font-family: inherit; font-size: 12.5px; font-weight: 800; cursor: pointer; }
.restore-btn:disabled { opacity: .5; cursor: default; }
</style>
