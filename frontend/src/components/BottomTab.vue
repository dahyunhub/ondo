<script setup>
import { useRoute } from 'vue-router'
import AppIcon from './AppIcon.vue'

const route = useRoute()
const items = [
  { to: '/', label: '홈', icon: 'home' },
  { to: '/children', label: '아이들', icon: 'children' },
  { to: '/journal', label: '일지', icon: 'journal' },
  { to: '/me', label: '마이', icon: 'me' },
]
// 모바일엔 분석 탭이 없다 — 분석은 일지의 하위 흐름이라 /analysis 체류 시 '일지' 탭을 활성 유지.
const isOn = (to) => {
  if (to === '/') return route.path === '/'
  if (to === '/journal') return route.path.startsWith('/journal') || route.path.startsWith('/analysis')
  return route.path.startsWith(to)
}
</script>

<template>
  <nav class="tabbar">
    <RouterLink v-for="it in items" :key="it.to" :to="it.to" class="tab" :class="{ on: isOn(it.to) }">
      <span class="ico"><AppIcon :name="it.icon" :size="23" :stroke="isOn(it.to) ? 2.3 : 2" /></span>
      <span class="lab">{{ it.label }}</span>
    </RouterLink>
  </nav>
</template>

<style scoped>
.tabbar {
  position: sticky; bottom: 0; z-index: 8;
  display: flex; padding: 10px 8px 18px;
  background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(12px);
  border-top: 1px solid var(--hair);
}
.tab {
  flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px;
  text-decoration: none; color: var(--text-faint);
}
.tab.on { color: var(--text); }
.ico {
  display: flex; align-items: center; justify-content: center;
  width: 56px; height: 30px; border-radius: 999px; background: transparent;
}
.tab.on .ico { background: var(--brand-100); }
.lab { font-size: 11px; font-weight: 600; }
.tab.on .lab { font-weight: 800; }
</style>
