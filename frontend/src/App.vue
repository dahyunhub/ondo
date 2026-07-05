<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useViewport } from './lib/useViewport'
import Sidebar from './components/Sidebar.vue'
import BottomTab from './components/BottomTab.vue'

const route = useRoute()
const { isDesktop } = useViewport()

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
  </div>
</template>
