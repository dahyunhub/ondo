<script setup>
// 분석 허브 — '한 아이 분석'(개인 관찰평가)과 '오늘 전체 분석'(하루 일지)의 진입점만 담당.
// 실제 분석 실행 로직은 일지 페이지(JournalView)에 단일 구현으로 두고 여기서는 위임만 한다.
//  · A 한 아이 분석 → 아이 목록(타임라인에서 개인평가)
//  · B 오늘 전체 분석 → 일지 페이지로 analyze=1 위임(없으면 즉시 생성, 있으면 열기)
import { useRouter } from 'vue-router'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import AppIcon from '../components/AppIcon.vue'

const { isDesktop } = useViewport()
const router = useRouter()

function goChildAnalysis() { router.push({ name: 'children' }) }
function goDailyAnalysis() { router.push({ name: 'journal', query: { analyze: '1' } }) }
</script>

<template>
  <div :class="isDesktop ? 'dt-page an-dt' : 'an-m'">
    <template v-if="isDesktop">
      <div class="jr-display" style="margin-bottom:6px">분석</div>
      <div class="dt-sub" style="margin-bottom:26px">어떤 걸 분석할까요? · {{ session.classroom?.name }}</div>
    </template>
    <header v-else class="m-head screen"><span class="jr-h1">분석</span></header>

    <div :class="isDesktop ? '' : 'screen body'">
      <div v-if="!isDesktop" class="ask">어떤 걸 분석할까요?</div>
      <div class="analysis" :class="{ dt: isDesktop }">
        <button class="abig" @click="goChildAnalysis">
          <div class="ab-top"><span class="ab-ic" style="background:rgba(201,168,232,.3)"><AppIcon name="me" :size="24" /></span>
            <div><div class="ab-t"><span class="ab-id">A</span>한 아이 분석</div><div class="ab-sub">개인 관찰 평가</div></div></div>
          <div class="ab-desc">한 아이의 기록을 모아 상담·발달평가용 관찰 평가를 만들어요.</div>
          <div class="ab-go">아이 선택하기 <AppIcon name="chevR" :size="16" /></div>
        </button>
        <button class="abig" @click="goDailyAnalysis">
          <div class="ab-top"><span class="ab-ic" style="background:var(--brand-300)"><AppIcon name="journal" :size="24" /></span>
            <div><div class="ab-t"><span class="ab-id">B</span>오늘 전체 분석</div><div class="ab-sub">하루 일지</div></div></div>
          <div class="ab-desc">오늘 반 전체 메모로 매일 제출하는 일지를 만들어요.</div>
          <div class="ab-go">바로 시작 <AppIcon name="chevR" :size="16" /></div>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.an-m { display: flex; flex-direction: column; }
.an-dt { max-width: 760px; }
.m-head { padding-top: 6px; padding-bottom: 12px; }
.body { padding-bottom: 28px; }
.dt-sub { font-size: 15px; color: var(--text-sub); }

.ask { font-size: 13px; font-weight: 800; color: var(--text-sub); margin-bottom: 10px; }
.analysis { display: flex; flex-direction: column; gap: 10px; }
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
</style>
