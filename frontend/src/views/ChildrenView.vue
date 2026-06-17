<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { session } from '../stores/session'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import ChildFormModal from '../components/ChildFormModal.vue'

const router = useRouter()
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

function openTimeline(c) {
  router.push({ name: 'timeline', params: { childId: c.id } })
}

function onSaved() { adding.value = false; load() }

const count = computed(() => children.value.length)
onMounted(load)
</script>

<template>
  <div class="children">
    <header class="hdr screen">
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
          <span class="kid-sub">{{ fmtBirth(c.birthDate) }} · {{ c.gender === 'MALE' ? '남' : '여' }}</span>
        </button>
        <button class="addcard" @click="adding = true">
          <span class="addcard-ic"><AppIcon name="plus" :size="26" :stroke="2.6" /></span>
          <span class="addcard-t">아이 등록하기</span>
          <span class="addcard-d">새 친구를 맞이해요</span>
        </button>
      </div>
    </div>

    <!-- 빠른 메모 FAB -->
    <RouterLink to="/memo" class="fab">
      <AppIcon name="plus" :size="24" :stroke="2.6" /> 메모
    </RouterLink>

    <ChildFormModal v-if="adding" mode="add" :classroom-id="classroomId" @close="adding = false" @saved="onSaved" />
  </div>
</template>

<style scoped>
.children { display: flex; flex-direction: column; position: relative; }
.hdr { padding-top: 12px; padding-bottom: 8px; }
.title-row { display: flex; align-items: baseline; gap: 10px; }
.meta { font-size: 14px; color: var(--text-sub); }
.action-row { display: flex; align-items: center; gap: 10px; margin-top: 14px; }
.sort { font-size: 13px; color: var(--text-faint); font-weight: 600; }
.add-btn { margin-left: auto; min-height: 44px; padding: 0 18px; }
.list-wrap { padding-top: 8px; padding-bottom: 90px; flex: 1; }
.grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.kid { display: flex; flex-direction: column; align-items: center; gap: 9px; padding: 20px 12px; cursor: pointer; text-align: center; border: none; font-family: inherit; background: var(--surface); }
.kid-name { font-size: 15px; font-weight: 800; }
.kid-sub { font-size: 11.5px; color: var(--text-sub); font-weight: 600; font-variant-numeric: tabular-nums; }
.addcard { display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 9px; padding: 20px 12px; cursor: pointer; text-align: center; font-family: inherit; border-radius: var(--r-card); border: 2px dashed var(--brand-300); background: var(--brand-100); }
.addcard-ic { width: 56px; height: 56px; border-radius: 50%; background: var(--surface); display: flex; align-items: center; justify-content: center; color: var(--brand-700); box-shadow: var(--shadow-sm); }
.addcard-t { font-size: 15px; font-weight: 800; color: var(--text); }
.addcard-d { font-size: 11.5px; color: var(--text-sub); font-weight: 600; }
.muted { color: var(--text-sub); }
.empty { padding: 20px 4px; line-height: 1.6; }
.err { color: var(--warn); font-weight: 600; }
.fab {
  position: fixed; right: 20px; bottom: 86px; z-index: 9;
  display: inline-flex; align-items: center; gap: 8px; height: 56px; padding: 0 22px 0 18px;
  border-radius: 999px; background: var(--brand-500); color: var(--text); font-weight: 800; font-size: 16px;
  text-decoration: none; box-shadow: 0 8px 22px rgba(245, 185, 64, 0.5);
}
/* app-col 폭에 맞춰 FAB 위치 보정 */
@media (min-width: 520px) { .fab { right: calc(50% - 240px + 20px); } }
</style>
