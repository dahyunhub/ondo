<script setup>
// 소개 페이지용 데스크톱(브라우저 창) 목업 프레임.
// 실제 앱의 데스크톱 셸(좌측 사이드바 + 본문)을 축소 재현한다. 본문은 slot 으로 받는다.
import Logo from './Logo.vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  active: { type: String, default: 'home' }, // 강조할 사이드바 항목 key
  // 창 본문 높이. 숫자(px) 또는 'auto'(콘텐츠에 맞춤). 타임라인은 고정 높이로 잘린 스크롤 느낌,
  // 일지는 'auto' 로 전체가 다 보이게.
  height: { type: [Number, String], default: 384 },
})
const bodyStyle = { height: typeof props.height === 'number' ? props.height + 'px' : props.height }

const NAV = [
  { key: 'home', label: '홈', icon: 'home' },
  { key: 'children', label: '아이들', icon: 'children' },
  { key: 'journal', label: '일지', icon: 'journal' },
  { key: 'analysis', label: '분석', icon: 'sparkle' },
  { key: 'me', label: '마이', icon: 'me' },
]
</script>

<template>
  <div class="win">
    <div class="win-bar" aria-hidden="true">
      <span class="tl r"></span><span class="tl y"></span><span class="tl g"></span>
    </div>
    <div class="win-body" :style="bodyStyle">
      <aside class="dm-side">
        <div class="dm-logo"><Logo variant="lockup" :height="26" /></div>
        <nav class="dm-nav">
          <div v-for="it in NAV" :key="it.key" class="dm-item" :class="{ on: it.key === active }">
            <AppIcon :name="it.icon" :size="17" :stroke="it.key === active ? 2.3 : 2" /> {{ it.label }}
          </div>
        </nav>
        <div class="dm-teacher">
          <span class="jr-avatar jr-avatar--sm" style="background:#E0AE3C">도현</span>
          <div class="dm-t-info">
            <div class="dm-t-name">이도현 선생님</div>
            <div class="dm-t-cls">햇살반</div>
          </div>
        </div>
      </aside>
      <main class="dm-main"><slot /></main>
    </div>
  </div>
</template>

<style scoped>
.win {
  width: 100%;
  border-radius: 16px;
  overflow: hidden;
  background: var(--surface, #fff);
  box-shadow: 0 26px 60px rgba(120, 90, 40, 0.24), 0 0 0 1px var(--hair, rgba(120,100,70,.1));
  display: flex;
  flex-direction: column;
}
.win-bar {
  height: 34px;
  flex: 0 0 auto;
  background: var(--surface-soft, #fffbf2);
  border-bottom: 1px solid var(--hair, rgba(120,100,70,.1));
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 14px;
}
.tl { width: 11px; height: 11px; border-radius: 50%; }
.tl.r { background: #ff8c82; }
.tl.y { background: #ffc94d; }
.tl.g { background: #8fd9a8; }
.win-body { display: flex; height: 384px; }
.dm-side {
  width: 152px;
  flex: 0 0 auto;
  background: var(--surface, #fff);
  border-right: 1px solid var(--hair, rgba(120,100,70,.1));
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
}
.dm-logo { padding: 0 4px 16px; }
.dm-nav { display: flex; flex-direction: column; gap: 2px; }
.dm-item {
  display: flex; align-items: center; gap: 9px; padding: 8px 10px; border-radius: 10px;
  color: var(--text-sub); font-weight: 600; font-size: 12.5px;
}
.dm-item.on { background: var(--brand-100); color: var(--text); font-weight: 800; }
.dm-teacher {
  margin-top: auto; display: flex; align-items: center; gap: 8px; padding: 8px; border-radius: 12px;
  background: var(--surface-soft);
}
.dm-t-info { line-height: 1.3; min-width: 0; }
.dm-t-name { font-size: 11px; font-weight: 700; white-space: nowrap; }
.dm-t-cls { font-size: 10px; color: var(--text-sub); white-space: nowrap; }
.dm-main { flex: 1; min-width: 0; padding: 18px 20px; background: var(--bg); overflow: hidden; text-align: left; }
</style>
