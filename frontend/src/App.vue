<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useViewport } from './lib/useViewport'
import { auth } from './stores/auth'
import { notice } from './stores/notice'
import Sidebar from './components/Sidebar.vue'
import BottomTab from './components/BottomTab.vue'
import FeedbackWidget from './components/FeedbackWidget.vue'

const route = useRoute()
const { isDesktop } = useViewport()

// 읽기 전용 데모 계정으로 로그인했는지 — 이메일로 식별(백엔드 ondo.demo.email 과 동일).
const DEMO_EMAIL = 'demo@ondo.app'
const isDemo = computed(() => auth.teacher?.email === DEMO_EMAIL && route.meta.shell === true)

// 셸(사이드바/하단탭) 적용 라우트
const useShell = computed(() => route.meta.shell === true)
// 모바일 하단 탭 노출 (메모는 집중 입력 화면이라 제외)
// analysis 는 하단탭 항목은 아니지만(BottomTab 은 4탭) 탭바 자체는 유지해야 홈·아이들·마이로 나갈 수 있다 — '일지' 탭 활성.
const TAB_ROUTES = ['home', 'children', 'timeline', 'journal', 'analysis', 'me']
const showTab = computed(() => TAB_ROUTES.includes(route.name))
</script>

<template>
  <div class="jr app-root">
    <!-- 셸 라우트: 데스크톱=사이드바+본문 / 모바일=본문+하단탭 -->
    <template v-if="useShell">
      <div v-if="isDesktop" class="shell-desktop">
        <Sidebar />
        <main class="shell-main"><RouterView /></main>
      </div>
      <div v-else class="shell-mobile">
        <main class="shell-mobile-main"><RouterView /></main>
        <BottomTab v-if="showTab" />
      </div>
    </template>

    <!-- 전체화면 라우트(로그인·반선택): 화면이 자체 반응형 처리 -->
    <RouterView v-else />

    <!-- 읽기 전용 데모 표식 — 레이아웃을 밀지 않는 플로팅 배지(클릭 통과) -->
    <div v-if="isDemo" class="demo-badge">🍊 읽기 전용 데모</div>

    <!-- 인앱 피드백 — 앱 셸(로그인 이후) 화면에서만 노출. 데모 계정도 보낼 수 있다. -->
    <FeedbackWidget v-if="useShell" />

    <!-- 전역 토스트(데모 읽기 전용 안내 등) -->
    <Transition name="toast">
      <div v-if="notice.message" class="app-toast">{{ notice.message }}</div>
    </Transition>
  </div>
</template>

<style scoped>
.demo-badge {
  position: fixed;
  top: 14px;
  right: 16px;
  z-index: 60;
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  color: var(--brand-700, #b25a1e);
  background: var(--brand-100, #fff1e6);
  border: 1px solid var(--brand-200, #ffd9bd);
  box-shadow: 0 2px 8px rgba(0, 0, 0, .08);
  pointer-events: none;
  user-select: none;
}

.app-toast {
  position: fixed;
  left: 50%;
  bottom: 84px;
  transform: translateX(-50%);
  z-index: 70;
  max-width: min(88vw, 420px);
  padding: 11px 18px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  text-align: center;
  color: #fff;
  background: rgba(30, 30, 30, .92);
  box-shadow: 0 6px 20px rgba(0, 0, 0, .22);
}

.toast-enter-active, .toast-leave-active { transition: opacity .25s, transform .25s; }
.toast-enter-from, .toast-leave-to { opacity: 0; transform: translate(-50%, 8px); }
</style>
