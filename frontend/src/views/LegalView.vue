<script setup>
// 개인정보 처리방침·이용약관 공용 뷰. /privacy·/terms 두 라우트가 모두 여기로 온다.
// 문서 본문은 lib/legal.js 가 단일 출처(faq.js 와 같은 방식) — 여기선 렌더링만 한다.
// 공개 페이지(로그인 전에도 접근)라 회원가입 동의 링크·소개 화면에서 바로 열 수 있어야 한다.
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useViewport } from '../lib/useViewport'
import AppIcon from '../components/AppIcon.vue'
import { LEGAL, LEGAL_EFFECTIVE } from '../lib/legal'
import { SITE } from '../lib/site'

const route = useRoute()
const router = useRouter()
const { isDesktop } = useViewport()

// 라우트 meta.doc('privacy' | 'terms')로 어느 문서인지 결정한다.
const doc = computed(() => LEGAL[route.meta.doc] || LEGAL.privacy)

function goBack() {
  if (window.history.length > 1) router.back()
  else router.replace({ name: 'login' })
}
</script>

<template>
  <div :class="isDesktop ? 'dt-page legal-dt' : 'legal-m'">
    <!-- 데스크톱 -->
    <template v-if="isDesktop">
      <button class="dt-back" @click="goBack"><AppIcon name="back" :size="18" /> 돌아가기</button>
      <div class="jr-display" style="margin-bottom:6px">{{ doc.title }}</div>
      <div class="dt-sub">{{ SITE.serviceName }} · 시행일 {{ LEGAL_EFFECTIVE }}</div>
    </template>

    <!-- 모바일 -->
    <header v-else class="m-head screen">
      <button class="back" @click="goBack" aria-label="뒤로"><AppIcon name="back" :size="22" /></button>
      <span class="jr-h1">{{ doc.title }}</span>
    </header>

    <div :class="isDesktop ? '' : 'screen body'">
      <div class="jr-card doc">
        <p v-if="!isDesktop" class="eff">{{ SITE.serviceName }} · 시행일 {{ LEGAL_EFFECTIVE }}</p>
        <p class="lead">{{ doc.lead }}</p>

        <section v-for="(sec, i) in doc.sections" :key="i" class="sec">
          <h2 class="sec-h">{{ sec.h }}</h2>
          <template v-for="(blk, j) in sec.body" :key="j">
            <p v-if="blk.p" class="para">{{ blk.p }}</p>
            <ul v-else-if="blk.ul" class="list">
              <li v-for="(item, k) in blk.ul" :key="k">{{ item }}</li>
            </ul>
            <p v-else-if="blk.note" class="note">{{ blk.note }}</p>
            <div v-else-if="blk.contact" class="contact">
              <div class="c-row"><span class="c-lab">운영자</span><span>{{ SITE.operator }}</span></div>
              <div class="c-row"><span class="c-lab">개인정보 보호책임자</span><span>{{ SITE.privacyOfficer }}</span></div>
              <div class="c-row"><span class="c-lab">문의</span><span>{{ SITE.contactEmail }}</span></div>
              <div v-if="SITE.businessAddress" class="c-row"><span class="c-lab">주소</span><span>{{ SITE.businessAddress }}</span></div>
            </div>
          </template>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.legal-dt { max-width: 760px; }
.legal-m { display: flex; flex-direction: column; }
.dt-back { display: inline-flex; align-items: center; gap: 4px; border: none; background: transparent; font-family: inherit;
  font-size: 13.5px; font-weight: 700; color: var(--text-sub); cursor: pointer; padding: 0 0 16px; margin-left: -2px; }
.dt-back:hover { color: var(--text); }
.dt-sub { font-size: 14px; color: var(--text-sub); margin-bottom: 22px; }
.m-head { display: flex; align-items: center; gap: 8px; padding-top: 6px; padding-bottom: 12px; }
.back { border: none; background: transparent; color: var(--text); cursor: pointer; padding: 4px; display: flex; margin-left: -4px; }
.body { padding-bottom: 40px; }
.doc { padding: 22px 22px 26px; }

.eff { font-size: 12.5px; color: var(--text-faint); font-weight: 600; margin: 0 0 14px; }
.lead { font-size: 14.5px; color: var(--text-sub); line-height: 1.7; margin: 0 0 24px; }
.sec { margin-bottom: 24px; }
.sec:last-child { margin-bottom: 0; }
.sec-h { font-size: 16px; font-weight: 800; color: var(--text); margin: 0 0 10px; letter-spacing: -0.01em; }
.para { font-size: 14px; color: var(--text); line-height: 1.72; margin: 0 0 10px; }
.para:last-child { margin-bottom: 0; }
.list { margin: 0 0 10px; padding-left: 4px; list-style: none; }
.list li { position: relative; font-size: 14px; color: var(--text); line-height: 1.68; padding-left: 16px; margin-bottom: 6px; }
.list li::before { content: ''; position: absolute; left: 3px; top: 10px; width: 4px; height: 4px; border-radius: 50%; background: var(--brand-500); }
.note {
  font-size: 13.5px; color: var(--text); line-height: 1.68; font-weight: 600;
  background: var(--brand-100); border-radius: 14px; padding: 13px 16px; margin: 0 0 10px;
}
.contact { background: var(--surface-soft); border: 1px solid var(--hair); border-radius: 14px; padding: 6px 16px; margin-top: 4px; }
.c-row { display: flex; gap: 12px; padding: 9px 0; border-bottom: 1px solid var(--hair); font-size: 13.5px; line-height: 1.5; }
.c-row:last-child { border-bottom: none; }
.c-lab { flex: 0 0 116px; color: var(--text-sub); font-weight: 700; }
/* 이메일·긴 값이 좁은 화면에서 가로 넘침을 만들지 않도록 줄바꿈 허용 */
.c-row > span:last-child { min-width: 0; overflow-wrap: anywhere; }
</style>
