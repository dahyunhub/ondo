<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import ChildFormModal from '../components/ChildFormModal.vue'

const router = useRouter()
const { isDesktop } = useViewport()
const classroomId = session.classroom?.id
const children = ref([])
const loading = ref(true)
const loadError = ref('')
const adding = ref(false)

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

function openTimeline(c) { router.push({ name: 'timeline', params: { childId: c.id } }) }
function onSaved() { adding.value = false; load() }

const count = computed(() => children.value.length)
onMounted(load)
</script>

<template>
  <!-- ============ 데스크톱 ============ -->
  <div v-if="isDesktop" class="dt-page">
    <header class="dt-head">
      <div>
        <div class="jr-display">아이들</div>
        <div class="dt-sub">{{ session.classroom?.name }} · 가나다순 · {{ count }}명</div>
      </div>
      <button class="jr-btn jr-btn--primary" @click="adding = true"><AppIcon name="plus" :size="21" :stroke="2.6" /> 아이 등록하기</button>
    </header>

    <p v-if="loading" class="muted">불러오는 중…</p>
    <p v-else-if="loadError" class="err">{{ loadError }}</p>
    <p v-else-if="!count" class="muted empty">아직 등록된 아이가 없어요. ‘아이 등록하기’로 시작해 보세요.</p>
    <div v-else class="grid dt">
      <button v-for="c in children" :key="c.id" class="kid jr-card" @click="openTimeline(c)">
        <Avatar :name="c.name" size="lg" />
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
        <button class="jr-btn jr-btn--primary add-btn" @click="adding = true">
          <AppIcon name="plus" :size="20" :stroke="2.6" /> 아이 등록하기
        </button>
      </div>
    </header>

    <div class="screen list-wrap">
      <p v-if="loading" class="muted">불러오는 중…</p>
      <p v-else-if="loadError" class="err">{{ loadError }}</p>
      <p v-else-if="!count" class="muted empty">아직 등록된 아이가 없어요. ‘아이 등록하기’로 시작해 보세요.</p>
      <div v-else class="grid">
        <button v-for="c in children" :key="c.id" class="kid jr-card" @click="openTimeline(c)">
          <Avatar :name="c.name" size="lg" />
          <span class="kid-name">{{ c.name }}</span>
          <span class="kid-chip">{{ fmtBirth(c.birthDate) }} · {{ c.gender === 'MALE' ? '남' : '여' }}</span>
        </button>
      </div>
    </div>
  </div>

  <ChildFormModal v-if="adding" mode="add" :classroom-id="classroomId" @close="adding = false" @saved="onSaved" />
</template>

<style scoped>
.dt-head { display: flex; align-items: flex-end; margin-bottom: 24px; }
.dt-sub { font-size: 15px; color: var(--text-sub); margin-top: 6px; white-space: nowrap; }
.dt-head .jr-btn { margin-left: auto; }

.children-m { display: flex; flex-direction: column; }
.m-head { padding-top: 12px; padding-bottom: 8px; }
.title-row { display: flex; align-items: baseline; gap: 10px; }
.meta { font-size: 14px; color: var(--text-sub); }
.action-row { display: flex; align-items: center; gap: 10px; margin-top: 14px; }
.sort { font-size: 13px; color: var(--text-faint); font-weight: 600; }
.add-btn { margin-left: auto; min-height: 44px; padding: 0 18px; }
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
.empty { padding: 20px 4px; line-height: 1.6; }
.err { color: var(--warn); font-weight: 600; }
</style>
