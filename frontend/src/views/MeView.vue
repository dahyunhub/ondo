<script setup>
// 마이 — 프로필 · 반 전환 · 설정. (프로필수정/알림/내보내기/도움말은 추후)
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const { isDesktop } = useViewport()

const teacher = computed(() => auth.teacher || {})
const teacherName = computed(() => teacher.value.name || '선생님')
const teacherEmail = computed(() => teacher.value.email || '')

const classrooms = ref([])
const switchOpen = ref(false)
const toast = ref('')

const nowYear = new Date().getFullYear()
function tagOf(year) {
  if (year === nowYear) return '올해'
  if (year === nowYear - 1) return '작년'
  return `${year}학년도`
}
const isNow = (c) => tagOf(c.year) === '올해'
const current = computed(() => session.classroom)
const isPast = computed(() => current.value && !isNow(current.value))

async function openSwitch() {
  switchOpen.value = true
  if (!classrooms.value.length) {
    try { classrooms.value = await api.get('/classrooms') } catch { /* 무시 */ }
  }
}
function pickClass(c) {
  session.select(c)
  switchOpen.value = false
  // 현재(올해) 반으로 전환하면 홈으로, 지난 반이면 그대로 보기
  if (isNow(c)) router.replace({ name: 'home' })
}
function notReady(label) { toast.value = `${label}은 곧 제공돼요`; setTimeout(() => (toast.value = ''), 1600) }
function logout() {
  auth.logout()
  session.clear()
  router.replace({ name: 'login' })
}

onMounted(() => {})
</script>

<template>
  <div :class="isDesktop ? 'dt-page me-dt' : 'me-m'">
    <template v-if="isDesktop"><div class="jr-display" style="margin-bottom:24px">마이</div></template>
    <header v-else class="m-head screen"><span class="jr-h1">마이</span></header>

    <div :class="isDesktop ? 'me-col' : 'screen body'">
      <!-- 프로필 -->
      <div class="jr-card profile">
        <Avatar :name="teacherName" size="lg" />
        <div class="p-info">
          <div class="p-name" :class="{ big: isDesktop }">{{ teacherName }} 선생님</div>
          <div class="p-email">{{ teacherEmail || '—' }}</div>
        </div>
        <button class="jr-btn jr-btn--secondary jr-btn--sm" @click="notReady('프로필 수정')">프로필 수정</button>
      </div>

      <!-- 지난 반 보는 중 배너 -->
      <div v-if="isPast" class="jr-banner viewing">
        <AppIcon name="clock" :size="20" style="color:var(--brand-700);flex:0 0 auto" />
        <span>{{ current.name }} {{ current.year }}학년도 기록을 보는 중이에요. 지난 반 아이들 기록을 확인할 수 있어요.</span>
      </div>

      <!-- 설정 -->
      <div v-if="isDesktop" class="set-label">설정</div>
      <div class="jr-card settings">
        <button class="row" @click="openSwitch">
          <AppIcon name="swap" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">반 전환하기</div><div class="r-sub">현재 {{ current?.name }} · {{ current?.year }}학년도</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="notReady('알림 설정')">
          <AppIcon name="bell" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">알림 설정</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="notReady('기록 내보내기')">
          <AppIcon name="download" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">기록 내보내기</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="notReady('도움말')">
          <AppIcon name="help" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">도움말</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row danger" @click="logout">
          <AppIcon name="logout" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">로그아웃</div></div>
        </button>
      </div>
    </div>

    <!-- 반 전환 모달 -->
    <div v-if="switchOpen" class="overlay" :class="{ dt: isDesktop }" @click.self="switchOpen = false">
      <div class="sheet" :class="{ dt: isDesktop }">
        <div class="sheet-top">
          <div>
            <div class="s-title">반 전환하기</div>
            <div class="s-sub">지금까지 맡았던 반 중에서 선택하세요</div>
          </div>
          <button class="close" @click="switchOpen = false"><AppIcon name="x" :size="18" /></button>
        </div>
        <p v-if="!classrooms.length" class="muted" style="padding:14px 0">불러오는 중…</p>
        <div v-else class="switch-list">
          <button
            v-for="c in classrooms" :key="c.id" class="sw-card" :class="{ on: current && c.id === current.id }"
            @click="pickClass(c)"
          >
            <span class="sw-ic" :class="{ on: current && c.id === current.id }"><AppIcon :name="isNow(c) ? 'sun' : 'leaf'" :size="22" /></span>
            <span class="sw-info">
              <span class="sw-row"><span class="sw-name">{{ c.name }}</span><span class="sw-tag" :class="{ now: isNow(c) }">{{ tagOf(c.year) }}</span></span>
              <span class="sw-sub">{{ c.year }}학년도 · 원아 {{ c.childCount }}명</span>
            </span>
            <AppIcon v-if="current && c.id === current.id" name="check" :size="20" :stroke="2.6" style="color:var(--brand-700);flex:0 0 auto" />
            <span v-else class="sw-view">보기</span>
          </button>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="toast" class="toast-wrap" :class="{ dt: isDesktop }">
        <div class="jr-toast">{{ toast }}</div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.me-m { display: flex; flex-direction: column; }
.m-head { padding-top: 6px; padding-bottom: 12px; }
.body { padding-bottom: 28px; }
.me-col { max-width: 560px; display: flex; flex-direction: column; gap: 20px; }

.profile { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.me-col .profile { margin-bottom: 0; }
.p-info { min-width: 0; flex: 1; }
.p-name { font-size: 19px; font-weight: 700; white-space: nowrap; }
.p-name.big { font-size: 22px; }
.p-email { font-size: 13.5px; color: var(--text-sub); white-space: nowrap; margin-top: 2px; }

.viewing { margin-bottom: 18px; font-size: 13px; font-weight: 700; line-height: 1.45; }
.me-col .viewing { margin-bottom: 0; }

.set-label { font-size: 14px; font-weight: 800; color: var(--text-sub); }
.settings { padding: 4px 16px; }
.me-col .settings { padding: 4px 18px; }
.row {
  display: flex; align-items: center; gap: 13px; padding: 14px 4px; cursor: pointer; width: 100%;
  border: none; background: transparent; font-family: inherit; text-align: left; border-bottom: 1px solid var(--hair);
}
.row:last-child { border-bottom: none; }
.r-ic { color: var(--text-sub); flex: 0 0 auto; }
.r-tx { min-width: 0; }
.r-label { font-size: 15px; font-weight: 600; white-space: nowrap; }
.r-sub { font-size: 12.5px; color: var(--text-sub); font-weight: 600; white-space: nowrap; margin-top: 2px; }
.r-chev { margin-left: auto; color: var(--text-faint); flex: 0 0 auto; }
.row.danger .r-ic, .row.danger .r-label { color: var(--warn); }

/* 모달 */
.overlay { position: fixed; inset: 0; z-index: 30; background: rgba(40, 30, 20, .34); display: flex; align-items: flex-end; justify-content: center; }
.overlay.dt { align-items: center; padding: 40px; }
.sheet { background: var(--surface); width: 100%; max-width: 440px; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
.sheet.dt { border-radius: 24px; padding: 26px 28px; }
.sheet-top { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 6px; }
.s-title { font-size: 18px; font-weight: 800; }
.s-sub { font-size: 13px; color: var(--text-sub); margin-top: 2px; }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; flex: 0 0 auto; }
.switch-list { display: flex; flex-direction: column; gap: 10px; margin-top: 16px; }
.sw-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px; border-radius: 16px; cursor: pointer; width: 100%;
  background: var(--surface); border: 2px solid var(--hair-strong); font-family: inherit; text-align: left;
}
.sw-card.on { background: var(--brand-100); border-color: var(--brand-500); }
.sw-ic { width: 44px; height: 44px; border-radius: 13px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; background: var(--surface-soft); color: var(--text-sub); }
.sw-ic.on { background: var(--brand-300); color: #9A6B12; }
.sw-info { flex: 1; min-width: 0; }
.sw-row { display: flex; align-items: center; gap: 7px; }
.sw-name { font-size: 16px; font-weight: 800; }
.sw-tag { font-size: 10.5px; font-weight: 700; padding: 4px 9px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }
.sw-tag.now { background: rgba(127, 209, 174, .22); color: #3C8F62; }
.sw-sub { display: block; font-size: 12.5px; color: var(--text-sub); font-weight: 600; margin-top: 2px; white-space: nowrap; }
.sw-view { font-size: 12.5px; font-weight: 800; color: var(--brand-700); flex: 0 0 auto; }
.muted { color: var(--text-sub); }

.toast-wrap { position: fixed; left: 20px; right: 20px; bottom: 110px; z-index: 40; display: flex; justify-content: center; }
.toast-wrap.dt { left: 50%; right: auto; transform: translateX(-50%); bottom: 48px; }
</style>
