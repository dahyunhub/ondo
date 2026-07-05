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
  { to: '/journal', name: 'journal', label: '일지', icon: 'journal' },
  { to: '/analysis', name: 'analysis', label: '분석', icon: 'sparkle' },
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
    <RouterLink to="/" class="logo" aria-label="홈으로"><Logo variant="lockup" :height="44" /></RouterLink>
    <nav class="nav">
      <RouterLink v-for="it in items" :key="it.to" :to="it.to" class="item" :class="{ on: isOn(it) }">
        <AppIcon :name="it.icon" :size="22" :stroke="isOn(it) ? 2.3 : 2" /> {{ it.label }}
      </RouterLink>
    </nav>
    <RouterLink to="/me" class="teacher" :class="{ on: route.name === 'me' }" aria-label="마이 페이지">
      <Avatar :name="auth.teacher?.name || '선생님'" size="sm"
              photo-url="/teachers/me/photo" :photo-key="auth.teacher?.photoUpdatedAt || ''" />
      <div class="t-info">
        <div class="t-name">{{ auth.teacher?.name || '선생님' }} 선생님</div>
        <div class="t-cls">{{ session.classroom?.name || '' }}</div>
      </div>
    </RouterLink>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 248px; flex: 0 0 auto; height: 100vh; position: sticky; top: 0;
  background: var(--surface); border-right: 1px solid var(--hair);
  display: flex; flex-direction: column; padding: 26px 18px;
}
.logo { display: block; padding: 0 8px 24px; cursor: pointer; }
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
  text-decoration: none; color: inherit; cursor: pointer; transition: background .12s;
}
.teacher:hover { background: var(--brand-100); }
.teacher.on { background: var(--brand-100); }
.t-info { line-height: 1.3; min-width: 0; }
.t-name { font-size: 14px; font-weight: 700; white-space: nowrap; }
.t-cls { font-size: 12px; color: var(--text-sub); white-space: nowrap; }
</style>
