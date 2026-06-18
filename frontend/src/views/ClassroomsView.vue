<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Logo from '../components/Logo.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const { isDesktop } = useViewport()
const classrooms = ref([])
const loading = ref(true)
const error = ref('')
const selectedId = ref(null)

const nowYear = new Date().getFullYear()
function tagOf(year) {
  if (year === nowYear) return '올해'
  if (year === nowYear - 1) return '작년'
  return `${year}학년도`
}
const isNow = (c) => tagOf(c.year) === '올해'

const selected = computed(() => classrooms.value.find((c) => c.id === selectedId.value) || null)

async function load() {
  loading.value = true
  error.value = ''
  try {
    classrooms.value = await api.get('/classrooms')
    if (classrooms.value.length) selectedId.value = classrooms.value[0].id
  } catch (e) {
    error.value = e.message || '반 목록을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

function start() {
  if (!selected.value) return
  session.select(selected.value)
  router.replace({ name: 'home' })
}

onMounted(load)
</script>

<template>
  <div class="cls fullscreen" :class="{ desk: isDesktop }">
    <!-- 데스크톱 좌측 브랜드 -->
    <section v-if="isDesktop" class="brand-panel">
      <Logo variant="vertical" :height="200" />
      <div class="bp-title">오늘도 우리 함께해요 ☀️</div>
      <div class="bp-desc">짧은 메모만 남기면 AI가 일지를 만들고,<br />기록은 아이별로 차곡차곡 쌓여요.</div>
    </section>

    <section class="pick-area">
      <div class="pick-inner">
        <!-- 모바일 상단 로고 -->
        <div v-if="!isDesktop" class="m-brand">
          <Logo variant="vertical" :height="148" />
          <div class="m-sub">오늘도 우리 함께해요 ☀️</div>
        </div>

        <template v-if="isDesktop">
          <div class="jr-h1 head">{{ auth.teacher?.name || '선생님' }} 선생님, 환영해요</div>
          <div class="jr-body head-sub">담당 반을 선택해 주세요</div>
        </template>
        <div v-else class="m-label">담당 반을 선택해 주세요</div>

        <p v-if="loading" class="muted">불러오는 중…</p>
        <p v-else-if="error" class="err">{{ error }}</p>
        <p v-else-if="!classrooms.length" class="muted">담당하는 반이 아직 없어요.</p>

        <div v-else class="list" :class="{ wide: isDesktop }">
          <button
            v-for="c in classrooms" :key="c.id"
            class="card" :class="{ on: c.id === selectedId }"
            @click="selectedId = c.id"
          >
            <span class="ic" :class="{ on: c.id === selectedId }">
              <AppIcon :name="isNow(c) ? 'sun' : 'leaf'" :size="26" />
            </span>
            <span class="info">
              <span class="row">
                <span class="name">{{ c.name }}</span>
                <span class="tag" :class="{ now: isNow(c) }">{{ tagOf(c.year) }}</span>
              </span>
              <span class="sub">{{ c.year }}학년도 · 원아 {{ c.childCount }}명</span>
            </span>
            <AppIcon v-if="c.id === selectedId" name="check" :size="22" :stroke="2.6" class="chk" />
          </button>
        </div>

        <button
          v-if="!loading && classrooms.length"
          class="jr-btn jr-btn--primary jr-btn--lg start"
          :class="{ block: !isDesktop }"
          @click="start"
        >
          {{ selected ? selected.name + '으로 시작하기' : '시작하기' }} <AppIcon name="chevR" :size="20" />
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.cls { display: flex; flex-direction: column; }
.cls.desk { flex-direction: row; height: 100vh; }

.brand-panel {
  flex: 0 0 46%; background: linear-gradient(165deg, #FFF6DC, #FFFBF2);
  border-right: 1px solid var(--hair);
  display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 40px;
}
.bp-title { font-size: 18px; font-weight: 700; color: #7a5e22; margin-top: 18px; }
.bp-desc { font-size: 14px; color: #8a6c2c; margin-top: 24px; line-height: 1.6; max-width: 300px; }

.pick-area { flex: 1; display: flex; flex-direction: column; justify-content: center; padding: 24px; }
.cls.desk .pick-area { padding: 0 56px; }
.pick-inner { width: 100%; margin: 0 auto; max-width: 460px; }
.cls.desk .pick-inner { max-width: 480px; margin: 0; }

.m-brand { text-align: center; display: flex; flex-direction: column; align-items: center; padding: 28px 0 24px; }
.m-sub { font-size: 16px; color: var(--text-sub); margin-top: 14px; }
.m-label { font-size: 14px; font-weight: 800; color: var(--text-sub); margin-bottom: 14px; }
.head { margin-bottom: 4px; }
.head-sub { color: var(--text-sub); margin-bottom: 26px; }

.list { display: flex; flex-direction: column; gap: 12px; }
.list.wide { gap: 14px; max-width: 420px; }
.card {
  display: flex; align-items: center; gap: 14px; padding: 16px 18px; border-radius: 18px; cursor: pointer;
  background: var(--surface); border: 2px solid var(--hair-strong); box-shadow: var(--shadow-sm);
  font-family: inherit; text-align: left; width: 100%; transition: all .14s;
}
.list.wide .card { padding: 18px 22px; }
.card.on { background: var(--brand-100); border-color: var(--brand-500); box-shadow: 0 6px 18px rgba(245, 185, 64, .25); }
.ic { width: 52px; height: 52px; border-radius: 16px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; background: var(--surface-soft); color: var(--text-sub); }
.ic.on { background: var(--brand-300); color: #9A6B12; }
.info { flex: 1; min-width: 0; }
.row { display: flex; align-items: center; gap: 8px; }
.name { font-size: 18px; font-weight: 800; }
.tag { font-size: 11px; font-weight: 700; padding: 4px 9px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }
.tag.now { background: rgba(127, 209, 174, .22); color: #3C8F62; }
.sub { display: block; font-size: 13px; color: var(--text-sub); margin-top: 3px; font-weight: 600; }
.chk { color: var(--brand-700); flex: 0 0 auto; }
.start { margin-top: 22px; }
.start.block { display: flex; width: 100%; }
.cls.desk .start { align-self: flex-start; padding-left: 32px; padding-right: 32px; }
.muted { color: var(--text-sub); }
.err { color: var(--warn); font-weight: 600; }
</style>
