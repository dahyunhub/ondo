<script setup>
// 데스크톱 좌측 사이드바 — 목업 Sidebar 대응. 로고·내비·교사 카드.
import { useRoute } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import Logo from './Logo.vue'
import AppIcon from './AppIcon.vue'
import Avatar from './Avatar.vue'

const route = useRoute()
const items = [
  { to: '/', name: 'home', label: '홈', icon: 'home' },
  { to: '/children', name: 'children', label: '아이들', icon: 'children' },
  { to: '/journal', name: 'journal', label: '일지·분석', icon: 'journal' },
  { to: '/me', name: 'me', label: '마이', icon: 'me' },
]
// 타임라인·메모도 '아이들'/'홈' 흐름에 속함
function isOn(it) {
  if (it.name === 'home') return route.name === 'home' || route.name === 'memo'
  if (it.name === 'children') return route.name === 'children' || route.name === 'timeline'
  return route.name === it.name
}
</script>

<template>
  <aside class="sidebar">
    <div class="logo"><Logo variant="lockup" :height="44" /></div>
    <nav class="nav">
      <RouterLink v-for="it in items" :key="it.to" :to="it.to" class="item" :class="{ on: isOn(it) }">
        <AppIcon :name="it.icon" :size="22" :stroke="isOn(it) ? 2.3 : 2" /> {{ it.label }}
      </RouterLink>
    </nav>
    <div class="teacher">
      <Avatar :name="auth.teacher?.name || '선생님'" size="sm" />
      <div class="t-info">
        <div class="t-name">{{ auth.teacher?.name || '선생님' }} 선생님</div>
        <div class="t-cls">{{ session.classroom?.name || '' }}</div>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 248px; flex: 0 0 auto; height: 100vh; position: sticky; top: 0;
  background: var(--surface); border-right: 1px solid var(--hair);
  display: flex; flex-direction: column; padding: 26px 18px;
}
.logo { padding: 0 8px 24px; }
.nav { display: flex; flex-direction: column; gap: 4px; }
.item {
  display: flex; align-items: center; gap: 13px; padding: 13px 14px; border-radius: 14px;
  color: var(--text-sub); font-weight: 600; font-size: 16px; text-decoration: none; transition: background .12s;
}
.item:hover { background: var(--surface-soft); }
.item.on { background: var(--brand-100); color: var(--text); font-weight: 800; }
.teacher {
  margin-top: auto; display: flex; align-items: center; gap: 11px;
  padding: 12px; border-radius: 16px; background: var(--surface-soft);
}
.t-info { line-height: 1.3; min-width: 0; }
.t-name { font-size: 14px; font-weight: 700; white-space: nowrap; }
.t-cls { font-size: 12px; color: var(--text-sub); white-space: nowrap; }
</style>
