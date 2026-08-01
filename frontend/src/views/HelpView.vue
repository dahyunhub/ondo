<script setup>
// 도움말 페이지(spec-help-faq). 마이페이지 '도움말' 행에서 진입한다.
// 모달이 아니라 페이지로 둔 이유: FAQ는 읽고 훑는 콘텐츠라 포커스 갇힘·모바일 스크롤 답답함이 없고,
// 나중에 교사에게 주소로 안내할 수도 있으며 항목이 늘어도 편하다.
import { useRouter } from 'vue-router'
import { useViewport } from '../lib/useViewport'
import AppIcon from '../components/AppIcon.vue'
import FaqAccordion from '../components/FaqAccordion.vue'

const router = useRouter()
const { isDesktop } = useViewport()

function goBack() {
  // 마이에서 들어오는 게 일반 경로라 마이로 되돌린다(직접 URL 진입 대비 fallback).
  if (window.history.length > 1) router.back()
  else router.replace({ name: 'me' })
}
</script>

<template>
  <div :class="isDesktop ? 'dt-page help-dt' : 'help-m'">
    <!-- 데스크톱 -->
    <template v-if="isDesktop">
      <div class="jr-display" style="margin-bottom:6px">도움말</div>
      <div class="dt-sub" style="margin-bottom:22px">자주 묻는 질문</div>
    </template>

    <!-- 모바일 -->
    <header v-else class="m-head screen">
      <button class="back" @click="goBack" aria-label="뒤로"><AppIcon name="back" :size="22" /></button>
      <span class="jr-h1">도움말</span>
    </header>

    <div :class="isDesktop ? '' : 'screen body'">
      <p class="lead">궁금한 점을 눌러 보세요. 자주 묻는 질문을 모았어요.</p>
      <div class="jr-card faq-card">
        <FaqAccordion />
      </div>
    </div>
  </div>
</template>

<style scoped>
.help-dt { max-width: 720px; }
.help-m { display: flex; flex-direction: column; }
.dt-sub { font-size: 15px; color: var(--text-sub); }
.m-head { display: flex; align-items: center; gap: 8px; padding-top: 6px; padding-bottom: 12px; }
.back { border: none; background: transparent; color: var(--text); cursor: pointer; padding: 4px; display: flex; margin-left: -4px; }
.body { padding-bottom: 28px; }
.lead { font-size: 14px; color: var(--text-sub); line-height: 1.55; margin: 0 0 14px; }
.faq-card { padding: 4px 16px; }
</style>
