<script setup>
// 일지·분석 허브 — 목업 JournalHub 비주얼. AI 분석(Epic 3·4)은 백엔드 구현 후 연결.
import { ref } from 'vue'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import AppIcon from '../components/AppIcon.vue'

const { isDesktop } = useViewport()
const today = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())
const toast = ref('')
function soon() { toast.value = 'AI 분석은 곧 제공돼요 (Epic 3·4 준비 중)'; setTimeout(() => (toast.value = ''), 1800) }
</script>

<template>
  <div :class="isDesktop ? 'dt-page' : 'jh-m'">
    <template v-if="isDesktop">
      <div class="jr-display" style="margin-bottom:6px">일지·분석</div>
      <div class="dt-sub" style="margin-bottom:26px">{{ today }} · {{ session.classroom?.name }}</div>
    </template>
    <header v-else class="m-head screen"><span class="jr-h1">일지·분석</span></header>

    <div :class="isDesktop ? '' : 'screen body'">
      <!-- 오늘 분석 CTA -->
      <div class="cta" :class="{ dt: isDesktop }" @click="soon">
        <span class="cta-ic"><AppIcon name="sparkle" :size="26" /></span>
        <div class="cta-tx"><div class="t">오늘 메모로 일지 쓰기</div><div class="d">하루 일지를 바로 만들어요</div></div>
        <AppIcon name="chevR" :size="22" />
      </div>

      <div class="ask">어떤 걸 분석할까요?</div>
      <div class="analysis" :class="{ dt: isDesktop }">
        <button class="abig" @click="soon">
          <div class="ab-top"><span class="ab-ic" style="background:rgba(201,168,232,.3)"><AppIcon name="me" :size="24" /></span>
            <div><div class="ab-t"><span class="ab-id">A</span>한 아이 분석</div><div class="ab-sub">개인 관찰 평가</div></div></div>
          <div class="ab-desc">한 아이의 기록을 모아 상담·발달평가용 관찰 평가를 만들어요.</div>
          <div class="ab-go">곧 제공 <span class="ab-badge">Epic 4</span></div>
        </button>
        <button class="abig" @click="soon">
          <div class="ab-top"><span class="ab-ic" style="background:var(--brand-300)"><AppIcon name="journal" :size="24" /></span>
            <div><div class="ab-t"><span class="ab-id">B</span>오늘 전체 분석</div><div class="ab-sub">하루 일지</div></div></div>
          <div class="ab-desc">오늘 반 전체 메모로 매일 제출하는 일지를 만들어요.</div>
          <div class="ab-go">곧 제공 <span class="ab-badge">Epic 3</span></div>
        </button>
      </div>

      <div class="recent-h"><span class="jr-h2" :style="isDesktop ? '' : 'font-size:18px'">최근 만든 일지</span></div>
      <div class="recent-empty">
        <AppIcon name="journal" :size="26" style="color:var(--text-faint)" />
        <div class="re-t">아직 만든 일지가 없어요</div>
        <div class="re-d">메모가 쌓이면 AI가 일지를 만들어 여기에 모아드릴게요.</div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="toast" class="toast-wrap" :class="{ dt: isDesktop }"><div class="jr-toast">{{ toast }}</div></div>
    </Transition>
  </div>
</template>

<style scoped>
.jh-m { display: flex; flex-direction: column; }
.m-head { padding-top: 6px; padding-bottom: 12px; }
.body { padding-bottom: 28px; }
.dt-sub { font-size: 15px; color: var(--text-sub); }

.cta { display: flex; align-items: center; gap: 14px; padding: 16px 18px; border-radius: 20px; cursor: pointer; background: linear-gradient(135deg, #FFD45E, #F5B940); box-shadow: 0 8px 22px rgba(245, 185, 64, .4); color: var(--text); margin-bottom: 18px; }
.cta.dt { max-width: 520px; }
.cta-ic { width: 46px; height: 46px; border-radius: 14px; flex: 0 0 auto; background: rgba(255, 255, 255, .45); display: flex; align-items: center; justify-content: center; }
.cta-tx { flex: 1; }
.cta-tx .t { font-size: 16px; font-weight: 800; }
.cta-tx .d { font-size: 13px; color: #7a5e22; font-weight: 600; margin-top: 2px; }

.ask { font-size: 13px; font-weight: 800; color: var(--text-sub); margin-bottom: 10px; }
.analysis { display: flex; flex-direction: column; gap: 10px; margin-bottom: 24px; }
.analysis.dt { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; max-width: 760px; }
.abig {
  display: flex; flex-direction: column; gap: 12px; padding: 20px; border-radius: var(--r-card); cursor: pointer;
  background: var(--surface); border: 1.5px solid var(--hair); box-shadow: var(--shadow); font-family: inherit; text-align: left;
}
.ab-top { display: flex; align-items: center; gap: 12px; }
.ab-ic { width: 48px; height: 48px; border-radius: 14px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; color: var(--text); }
.ab-t { font-size: 16.5px; font-weight: 800; }
.ab-id { color: var(--text-faint); margin-right: 5px; }
.ab-sub { font-size: 12.5px; color: var(--text-sub); font-weight: 600; }
.ab-desc { font-size: 13.5px; color: var(--text-sub); line-height: 1.55; }
.ab-go { display: flex; align-items: center; gap: 8px; font-size: 13.5px; font-weight: 800; color: var(--text-faint); }
.ab-badge { font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }

.recent-h { margin-bottom: 14px; }
.recent-empty {
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px;
  padding: 32px 20px; border-radius: var(--r-card); background: var(--surface); box-shadow: var(--shadow-sm); max-width: 760px;
}
.re-t { font-size: 15px; font-weight: 800; color: var(--text-sub); }
.re-d { font-size: 13px; color: var(--text-faint); line-height: 1.5; }

.toast-wrap { position: fixed; left: 20px; right: 20px; bottom: 110px; z-index: 40; display: flex; justify-content: center; }
.toast-wrap.dt { left: 50%; right: auto; transform: translateX(-50%); bottom: 48px; }
</style>
