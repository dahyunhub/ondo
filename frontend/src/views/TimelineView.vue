<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../lib/api'
import { session } from '../stores/session'
import { areaMeta, AREA_ORDER, FILTER_ORDER } from '../lib/areas'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import NuriChip from '../components/NuriChip.vue'
import SproutLoader from '../components/SproutLoader.vue'
import ChildFormModal from '../components/ChildFormModal.vue'

const route = useRoute()
const router = useRouter()
const classroomId = session.classroom?.id
const rawId = Number(route.params.childId)
const childId = Number.isInteger(rawId) && rawId > 0 ? rawId : null

const child = ref(null)
const entries = ref([])
const loading = ref(true)
const error = ref('')
const filter = ref('ALL')        // 'ALL' | enum | 'UNCLASSIFIED'
const areaMenuFor = ref(null)    // memoId | null
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
  if (childId === null) {
    error.value = '잘못된 접근이에요.'
    loading.value = false
    return
  }
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
    // 활성 필터가 방금 지정한 영역이 아니면 메모가 화면에서 사라져 삭제처럼 보임 → 전체로 풀어 유지
    if (filter.value !== 'ALL' && filter.value !== areaEnum) {
      filter.value = 'ALL'
    }
    await loadTimeline()
  } catch (e) {
    error.value = e.message || '영역 수정에 실패했어요.'
  } finally {
    patching.value = false
  }
}

function onChildSaved() { editChild.value = false; loadChild() }
function onChildDeleted() { router.replace({ name: 'children' }) }

// createdAt 은 UTC Instant('…Z') — 브라우저 로컬 타임존으로 표시/그룹.
const pad = (n) => String(n).padStart(2, '0')
function fmtTime(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}
function dateKey(iso) {
  const d = new Date(iso)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}
function dateLabel(iso) {
  const d = new Date(iso)
  return `${d.getMonth() + 1}월 ${d.getDate()}일`
}

// 로컬 날짜별 그룹 (최신순 유지)
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

onMounted(reload)
</script>

<template>
  <div class="tl">
    <header class="hdr screen">
      <button class="icbtn" @click="router.push({ name: 'children' })"><AppIcon name="back" :size="24" /></button>
      <div class="head" v-if="child">
        <Avatar :name="child.name" size="lg" />
        <div class="head-info">
          <div class="jr-h1 name">{{ child.name }}</div>
          <div class="sub">{{ session.classroom?.name }} · 관찰기록 <b>{{ total }}</b>개</div>
        </div>
      </div>
      <button class="icbtn" style="margin-left:auto" @click="editChild = true" aria-label="정보 수정">
        <AppIcon name="dots" :size="22" />
      </button>
    </header>

    <div class="screen body">
      <p v-if="loading" class="muted">불러오는 중…</p>
      <p v-else-if="error" class="err">{{ error }}</p>

      <!-- 빈 상태 -->
      <div v-else-if="filter === 'ALL' && total === 0" class="empty">
        <SproutLoader :size="92" />
        <div class="jr-h2" style="margin-top:18px">아직 기록이 없어요</div>
        <p class="empty-d">{{ child ? child.name : '이 아이' }}의 오늘 첫 메모를 남겨볼까요?</p>
        <RouterLink :to="{ name: 'memo', query: { childId } }" class="jr-btn jr-btn--primary" style="margin-top:22px">
          <AppIcon name="plus" :size="20" :stroke="2.6" /> 첫 메모 남기기
        </RouterLink>
      </div>

      <template v-else>
        <!-- 개인평가(Epic 4 예정) -->
        <div class="eval-soon">
          <span class="eval-ic"><AppIcon name="me" :size="17" /></span>
          <span class="eval-t">개인 관찰평가</span>
          <span class="eval-badge">Epic 4 예정</span>
        </div>

        <!-- 필터 -->
        <div class="filter-bar">
          <div class="filter-title">관찰 기록 타임라인</div>
          <div class="chips">
            <button class="jr-toggle" :class="{ 'is-on': filter === 'ALL' }" @click="setFilter('ALL')">전체</button>
            <button
              v-for="f in FILTER_ORDER" :key="f" class="jr-toggle" :class="{ 'is-on': filter === f }"
              @click="setFilter(f)"
            >
              <span class="cdot" :style="{ background: areaMeta(f === 'UNCLASSIFIED' ? null : f).color,
                border: f === 'UNCLASSIFIED' ? '1px dashed #9b9384' : 'none' }" />
              {{ areaMeta(f === 'UNCLASSIFIED' ? null : f).ko }}
            </button>
          </div>
        </div>

        <p v-if="total === 0" class="muted" style="padding:8px 2px">이 영역의 기록이 없어요.</p>

        <!-- 타임라인 -->
        <div class="groups">
          <div v-for="g in groups" :key="g.key" class="group">
            <div class="date"><AppIcon name="cal" :size="15" /> {{ g.label }}</div>
            <div class="items">
              <div
                v-for="e in g.items" :key="e.id" class="block"
                :style="{ '--strip': areaMeta(e.curriculumArea).color }"
                :class="{ open: areaMenuFor === e.id }"
              >
                <div class="block-top">
                  <NuriChip :area="e.curriculumArea" />
                  <button
                    class="area-edit" :class="{ uncat: !e.curriculumArea }"
                    @click="areaMenuFor = areaMenuFor === e.id ? null : e.id"
                  >
                    <template v-if="!e.curriculumArea">영역 지정 <AppIcon name="chevD" :size="13" :stroke="2.4" /></template>
                    <AppIcon v-else name="pencil" :size="13" />
                  </button>
                  <span class="time">{{ fmtTime(e.createdAt) }}</span>
                </div>
                <div class="content">{{ e.content }}</div>

                <!-- 영역 지정 드롭다운 -->
                <div v-if="areaMenuFor === e.id" class="menu">
                  <div class="menu-title">누리과정 영역 지정</div>
                  <button
                    v-for="a in AREA_ORDER" :key="a" class="menu-item"
                    :class="{ on: e.curriculumArea === a }" :disabled="patching" @click="pickArea(e.id, a)"
                  >
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

    <!-- 메뉴 바깥 클릭 닫기 -->
    <div v-if="areaMenuFor !== null" class="menu-backdrop" @click="areaMenuFor = null" />

    <ChildFormModal
      v-if="editChild && child" mode="edit" :child="child"
      @close="editChild = false" @saved="onChildSaved" @deleted="onChildDeleted"
    />
  </div>
</template>

<style scoped>
.tl { display: flex; flex-direction: column; }
.hdr { display: flex; align-items: center; gap: 8px; padding-top: 10px; padding-bottom: 12px; }
.icbtn { border: none; background: transparent; color: var(--text-sub); cursor: pointer; padding: 4px; }
.head { display: flex; align-items: center; gap: 12px; min-width: 0; }
.head-info { min-width: 0; }
.name { font-size: 22px; }
.sub { font-size: 13.5px; color: var(--text-sub); margin-top: 2px; white-space: nowrap; }
.sub b { color: var(--text); }
.body { padding-bottom: 28px; }
.muted { color: var(--text-sub); }
.err { color: var(--warn); font-weight: 600; }
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 40px 24px; min-height: 320px; }
.empty-d { font-size: 16px; color: var(--text-sub); margin-top: 8px; line-height: 1.5; }
.eval-soon { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-radius: 16px; background: var(--surface); box-shadow: var(--shadow-sm); margin-bottom: 16px; color: var(--text-sub); }
.eval-ic { width: 28px; height: 28px; border-radius: 9px; background: var(--brand-300); color: var(--text); display: flex; align-items: center; justify-content: center; flex: 0 0 auto; }
.eval-t { font-size: 14.5px; font-weight: 800; color: var(--text); }
.eval-badge { margin-left: auto; font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }
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
