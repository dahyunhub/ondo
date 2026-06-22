<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../lib/api'
import { session } from '../stores/session'
import { areaMeta, AREA_ORDER, FILTER_ORDER } from '../lib/areas'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import NuriChip from '../components/NuriChip.vue'
import SproutLoader from '../components/SproutLoader.vue'
import ChildFormModal from '../components/ChildFormModal.vue'
import ReportPanel from '../components/ReportPanel.vue'

const route = useRoute()
const router = useRouter()
const { isDesktop } = useViewport()
const classroomId = session.classroom?.id
const rawId = Number(route.params.childId)
const childId = Number.isInteger(rawId) && rawId > 0 ? rawId : null

const child = ref(null)
const entries = ref([])
const loading = ref(true)
const error = ref('')
const filter = ref('ALL')
const areaMenuFor = ref(null)
const editChild = ref(false)
const patching = ref(false)

async function loadChild() {
  const list = await api.get(`/classrooms/${classroomId}/children`)
  child.value = list.find((c) => c.id === childId) || null
}
async function loadTimeline() {
  const q = filter.value === 'ALL' ? '' : `?area=${filter.value}`
  entries.value = await api.get(`/children/${childId}/timeline${q}`)
}
async function reload() {
  loading.value = true
  error.value = ''
  if (childId === null) { error.value = '잘못된 접근이에요.'; loading.value = false; return }
  try {
    await Promise.all([child.value ? Promise.resolve() : loadChild(), loadTimeline()])
  } catch (e) {
    error.value = e.message || '타임라인을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}
async function setFilter(f) {
  filter.value = f
  try { await loadTimeline() } catch (e) { error.value = e.message }
}
async function pickArea(memoId, areaEnum) {
  if (patching.value) return
  patching.value = true
  try {
    await api.patch(`/memos/${memoId}/curriculum-area`, { curriculumArea: areaEnum })
    areaMenuFor.value = null
    if (filter.value !== 'ALL' && filter.value !== areaEnum) filter.value = 'ALL'
    await loadTimeline()
  } catch (e) {
    error.value = e.message || '영역 수정에 실패했어요.'
  } finally {
    patching.value = false
  }
}
function onChildSaved() { editChild.value = false; loadChild() }
function onChildDeleted() { router.replace({ name: 'children' }) }

const pad = (n) => String(n).padStart(2, '0')
function fmtTime(iso) { if (!iso) return ''; const d = new Date(iso); return `${pad(d.getHours())}:${pad(d.getMinutes())}` }
function dateKey(iso) { const d = new Date(iso); return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` }
function dateLabel(iso) { const d = new Date(iso); return `${d.getMonth() + 1}월 ${d.getDate()}일` }

const groups = computed(() => {
  const out = []
  for (const e of entries.value) {
    const key = dateKey(e.createdAt)
    let g = out.find((x) => x.key === key)
    if (!g) { g = { key, label: dateLabel(e.createdAt), items: [] }; out.push(g) }
    g.items.push(e)
  }
  return out
})
const total = computed(() => entries.value.length)
const showEmpty = computed(() => filter.value === 'ALL' && total.value === 0)

onMounted(reload)
</script>

<template>
  <div :class="isDesktop ? 'tl-dt' : 'tl-m'">
    <!-- ============ 데스크톱: 좌측 프로필 + 우측 타임라인 ============ -->
    <template v-if="isDesktop">
      <div class="left-pane">
        <button class="back-link" @click="router.push({ name: 'children' })"><AppIcon name="back" :size="18" /> 아이 목록</button>
        <div v-if="child" class="head">
          <Avatar :name="child.name" size="lg" />
          <div class="head-info">
            <div class="jr-h1 name" style="font-size:26px">{{ child.name }}</div>
            <div class="sub">{{ session.classroom?.name }} · 관찰기록 <b>{{ total }}</b>개</div>
          </div>
          <button class="icbtn" @click="editChild = true" aria-label="정보 수정"><AppIcon name="dots" :size="20" /></button>
        </div>
        <!-- 개인 관찰평가 (Epic 4) -->
        <ReportPanel v-if="childId" :child-id="childId" :child-name="child?.name || '아이'" style="margin-top:22px" />
      </div>

      <div class="right-pane">
        <p v-if="loading" class="muted">불러오는 중…</p>
        <div v-else-if="error" class="err-box">
          <p class="err">{{ error }}</p>
          <button class="jr-btn jr-btn--secondary" @click="reload"><AppIcon name="swap" :size="18" /> 다시 시도</button>
        </div>
        <div v-else-if="showEmpty" class="empty">
          <SproutLoader :size="92" />
          <div class="jr-h2" style="margin-top:18px">아직 기록이 없어요</div>
          <p class="empty-d">{{ child ? child.name : '이 아이' }}의 오늘 첫 메모를 남겨볼까요?</p>
          <RouterLink :to="{ name: 'memo', query: { childId } }" class="jr-btn jr-btn--primary" style="margin-top:22px">
            <AppIcon name="plus" :size="20" :stroke="2.6" /> 첫 메모 남기기
          </RouterLink>
        </div>
        <template v-else>
          <div class="rp-head">
            <span class="jr-h2">관찰 기록 타임라인</span>
            <span class="rp-tip">최신순 · 영역 칩을 눌러 수정</span>
          </div>
          <div class="chips" style="margin-bottom:22px">
            <button class="jr-toggle" :class="{ 'is-on': filter === 'ALL' }" @click="setFilter('ALL')">전체</button>
            <button v-for="f in FILTER_ORDER" :key="f" class="jr-toggle" :class="{ 'is-on': filter === f }" @click="setFilter(f)">
              <span class="cdot" :style="{ background: areaMeta(f === 'UNCLASSIFIED' ? null : f).color, border: f === 'UNCLASSIFIED' ? '1px dashed #9b9384' : 'none' }" />
              {{ areaMeta(f === 'UNCLASSIFIED' ? null : f).ko }}
            </button>
          </div>
          <p v-if="total === 0" class="muted" style="padding:8px 2px">이 영역의 기록이 없어요.</p>
          <div class="groups" style="max-width:560px">
            <div v-for="g in groups" :key="g.key" class="group">
              <div class="date"><AppIcon name="cal" :size="15" /> {{ g.label }}</div>
              <div class="items">
                <div v-for="e in g.items" :key="e.id" class="block" :style="{ '--strip': areaMeta(e.curriculumArea).color }" :class="{ open: areaMenuFor === e.id }">
                  <div class="block-top">
                    <NuriChip :area="e.curriculumArea" />
                    <button class="area-edit" :class="{ uncat: !e.curriculumArea }" @click="areaMenuFor = areaMenuFor === e.id ? null : e.id">
                      <template v-if="!e.curriculumArea">영역 지정 <AppIcon name="chevD" :size="13" :stroke="2.4" /></template>
                      <AppIcon v-else name="pencil" :size="13" />
                    </button>
                    <span class="time">{{ fmtTime(e.createdAt) }}</span>
                  </div>
                  <div class="content">{{ e.content }}</div>
                  <div v-if="areaMenuFor === e.id" class="menu">
                    <div class="menu-title">누리과정 영역 지정</div>
                    <button v-for="a in AREA_ORDER" :key="a" class="menu-item" :class="{ on: e.curriculumArea === a }" :disabled="patching" @click="pickArea(e.id, a)">
                      <span class="mdot" :style="{ background: areaMeta(a).color }" />
                      <span class="mlabel">{{ areaMeta(a).ko }}</span>
                      <AppIcon v-if="e.curriculumArea === a" name="check" :size="15" :stroke="3" style="margin-left:auto;color:var(--brand-700)" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </template>

    <!-- ============ 모바일: 스택 ============ -->
    <template v-else>
      <header class="m-head screen">
        <button class="icbtn" @click="router.push({ name: 'children' })"><AppIcon name="back" :size="24" /></button>
        <div class="head" v-if="child">
          <Avatar :name="child.name" size="lg" />
          <div class="head-info">
            <div class="jr-h1 name">{{ child.name }}</div>
            <div class="sub">{{ session.classroom?.name }} · 관찰기록 <b>{{ total }}</b>개</div>
          </div>
        </div>
        <button class="icbtn" style="margin-left:auto" @click="editChild = true" aria-label="정보 수정"><AppIcon name="dots" :size="22" /></button>
      </header>

      <div class="screen body">
        <p v-if="loading" class="muted">불러오는 중…</p>
        <div v-else-if="error" class="err-box">
          <p class="err">{{ error }}</p>
          <button class="jr-btn jr-btn--secondary" @click="reload"><AppIcon name="swap" :size="18" /> 다시 시도</button>
        </div>
        <div v-else-if="showEmpty" class="empty">
          <SproutLoader :size="92" />
          <div class="jr-h2" style="margin-top:18px">아직 기록이 없어요</div>
          <p class="empty-d">{{ child ? child.name : '이 아이' }}의 오늘 첫 메모를 남겨볼까요?</p>
          <RouterLink :to="{ name: 'memo', query: { childId } }" class="jr-btn jr-btn--primary" style="margin-top:22px">
            <AppIcon name="plus" :size="20" :stroke="2.6" /> 첫 메모 남기기
          </RouterLink>
        </div>
        <template v-else>
          <ReportPanel v-if="childId" :child-id="childId" :child-name="child?.name || '아이'" style="margin-bottom:16px" />
          <div class="filter-bar">
            <div class="filter-title">관찰 기록 타임라인</div>
            <div class="chips">
              <button class="jr-toggle" :class="{ 'is-on': filter === 'ALL' }" @click="setFilter('ALL')">전체</button>
              <button v-for="f in FILTER_ORDER" :key="f" class="jr-toggle" :class="{ 'is-on': filter === f }" @click="setFilter(f)">
                <span class="cdot" :style="{ background: areaMeta(f === 'UNCLASSIFIED' ? null : f).color, border: f === 'UNCLASSIFIED' ? '1px dashed #9b9384' : 'none' }" />
                {{ areaMeta(f === 'UNCLASSIFIED' ? null : f).ko }}
              </button>
            </div>
          </div>
          <p v-if="total === 0" class="muted" style="padding:8px 2px">이 영역의 기록이 없어요.</p>
          <div class="groups">
            <div v-for="g in groups" :key="g.key" class="group">
              <div class="date"><AppIcon name="cal" :size="15" /> {{ g.label }}</div>
              <div class="items">
                <div v-for="e in g.items" :key="e.id" class="block" :style="{ '--strip': areaMeta(e.curriculumArea).color }" :class="{ open: areaMenuFor === e.id }">
                  <div class="block-top">
                    <NuriChip :area="e.curriculumArea" />
                    <button class="area-edit" :class="{ uncat: !e.curriculumArea }" @click="areaMenuFor = areaMenuFor === e.id ? null : e.id">
                      <template v-if="!e.curriculumArea">영역 지정 <AppIcon name="chevD" :size="13" :stroke="2.4" /></template>
                      <AppIcon v-else name="pencil" :size="13" />
                    </button>
                    <span class="time">{{ fmtTime(e.createdAt) }}</span>
                  </div>
                  <div class="content">{{ e.content }}</div>
                  <div v-if="areaMenuFor === e.id" class="menu">
                    <div class="menu-title">누리과정 영역 지정</div>
                    <button v-for="a in AREA_ORDER" :key="a" class="menu-item" :class="{ on: e.curriculumArea === a }" :disabled="patching" @click="pickArea(e.id, a)">
                      <span class="mdot" :style="{ background: areaMeta(a).color }" />
                      <span class="mlabel">{{ areaMeta(a).ko }}</span>
                      <AppIcon v-if="e.curriculumArea === a" name="check" :size="15" :stroke="3" style="margin-left:auto;color:var(--brand-700)" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>
    </template>

    <!-- 메뉴 바깥 클릭 닫기 -->
    <div v-if="areaMenuFor !== null" class="menu-backdrop" @click="areaMenuFor = null" />

    <ChildFormModal v-if="editChild && child" mode="edit" :child="child" @close="editChild = false" @saved="onChildSaved" @deleted="onChildDeleted" />
  </div>
</template>

<style scoped>
/* ===== 데스크톱 2-pane ===== */
.tl-dt { display: flex; min-height: 100vh; }
.left-pane { width: 400px; flex: 0 0 auto; padding: 34px 28px; border-right: 1px solid var(--hair); }
.right-pane { flex: 1; min-width: 0; padding: 34px 40px; }
.back-link { display: flex; align-items: center; gap: 6px; border: none; background: transparent; color: var(--text-sub); cursor: pointer; font-family: inherit; font-size: 14px; font-weight: 600; margin-bottom: 22px; padding: 0; }
.rp-head { display: flex; align-items: center; margin-bottom: 18px; }
.rp-tip { margin-left: auto; font-size: 13px; color: var(--text-faint); }

/* ===== 모바일 ===== */
.tl-m { display: flex; flex-direction: column; }
.m-head { display: flex; align-items: center; gap: 8px; padding-top: 10px; padding-bottom: 12px; }
.body { padding-bottom: 28px; }

/* 공통 */
.icbtn { border: none; background: transparent; color: var(--text-sub); cursor: pointer; padding: 4px; }
.head { display: flex; align-items: center; gap: 12px; min-width: 0; }
.head-info { min-width: 0; }
.name { font-size: 22px; }
.sub { font-size: 13.5px; color: var(--text-sub); margin-top: 2px; white-space: nowrap; }
.sub b { color: var(--text); }
.muted { color: var(--text-sub); }
.err { color: var(--warn); font-weight: 600; }
.err-box { display: flex; flex-direction: column; align-items: flex-start; gap: 12px; padding: 8px 0; }
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 40px 24px; min-height: 320px; }
.empty-d { font-size: 16px; color: var(--text-sub); margin-top: 8px; line-height: 1.5; }
.filter-bar { position: sticky; top: 0; background: var(--bg); padding-bottom: 12px; margin-bottom: 4px; z-index: 2; }
.filter-title { font-size: 13px; font-weight: 800; color: var(--text-sub); margin-bottom: 10px; }
.chips { display: flex; gap: 8px; flex-wrap: wrap; }
.chips .jr-toggle { cursor: pointer; }
.cdot { width: 8px; height: 8px; border-radius: 50%; flex: 0 0 auto; }
.groups { display: flex; flex-direction: column; gap: 18px; margin-top: 8px; }
.date { font-size: 13px; font-weight: 800; color: var(--text-sub); margin-bottom: 10px; display: flex; align-items: center; gap: 8px; }
.items { display: flex; flex-direction: column; gap: 10px; }
.block { position: relative; background: var(--surface); border-radius: 16px; padding: 16px 18px 16px 22px; box-shadow: var(--shadow-sm); overflow: hidden; }
.block::before { content: ''; position: absolute; left: 0; top: 0; bottom: 0; width: 6px; background: var(--strip, var(--brand-500)); }
.block.open { overflow: visible; z-index: 5; }
.block-top { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.area-edit { display: flex; align-items: center; gap: 3px; border: none; background: transparent; border-radius: 999px; padding: 4px 6px; cursor: pointer; font-family: inherit; font-size: 11.5px; font-weight: 700; color: var(--text-faint); }
.area-edit.uncat { background: var(--brand-100); color: var(--brand-700); padding: 4px 9px; }
.time { margin-left: auto; font-size: 12px; color: var(--text-faint); font-variant-numeric: tabular-nums; }
.content { font-size: 14.5px; line-height: 1.55; }
.menu { position: absolute; top: 100%; right: 0; margin-top: 6px; z-index: 12; background: var(--surface); border-radius: 14px; box-shadow: var(--shadow-lg); border: 1px solid var(--hair); padding: 8px; width: 200px; }
.menu-title { font-size: 11.5px; font-weight: 800; color: var(--text-faint); padding: 4px 8px 8px; }
.menu-item { display: flex; align-items: center; gap: 9px; padding: 8px; border-radius: 9px; cursor: pointer; width: 100%; border: none; background: transparent; font-family: inherit; }
.menu-item.on { background: var(--brand-100); }
.menu-item:hover { background: var(--surface-soft); }
.mdot { width: 9px; height: 9px; border-radius: 50%; flex: 0 0 auto; }
.mlabel { font-size: 13.5px; font-weight: 600; color: var(--text); }
.menu-item.on .mlabel { font-weight: 800; }
.menu-backdrop { position: fixed; inset: 0; z-index: 4; }
</style>
