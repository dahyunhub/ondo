<script setup>
// FAQ 아코디언(spec-help-faq). 한 번에 하나만 펼친다 — 여러 개가 열려 스크롤이 길어지면
// 오히려 찾기 어렵다. 기본은 전부 접힘(질문 목록이 한눈에 보여야 훑을 수 있다).
import { ref, useId } from 'vue'
import { FAQ } from '../lib/faq'
import AppIcon from './AppIcon.vue'

const open = ref(-1) // 열린 항목 인덱스. -1 = 전부 접힘
const uid = useId()  // aria-controls 연결용 고유 id 접두어

function toggle(i) {
  open.value = open.value === i ? -1 : i
}
</script>

<template>
  <div class="faq">
    <div v-for="(item, i) in FAQ" :key="i" class="faq-item">
      <!-- 질문: 버튼이라 키보드로 열고 닫힌다. aria-expanded/controls 로 스크린리더에 상태 전달. -->
      <button
        class="faq-q"
        :aria-expanded="open === i"
        :aria-controls="`${uid}-panel-${i}`"
        @click="toggle(i)"
      >
        <span class="faq-q-tx">{{ item.q }}</span>
        <AppIcon name="chevD" :size="18" class="faq-chev" :class="{ up: open === i }" />
      </button>
      <div v-show="open === i" :id="`${uid}-panel-${i}`" class="faq-a" role="region">
        {{ item.a }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.faq { display: flex; flex-direction: column; }
.faq-item + .faq-item { border-top: 1px solid var(--hair); }
.faq-q {
  display: flex; align-items: center; gap: 12px; width: 100%; padding: 15px 4px;
  background: transparent; border: none; font-family: inherit; text-align: left; cursor: pointer;
  color: var(--text); font-size: 14.5px; font-weight: 700; line-height: 1.4;
}
.faq-q-tx { flex: 1; min-width: 0; }
.faq-chev { flex: 0 0 auto; color: var(--text-faint); transition: transform .18s ease; }
.faq-chev.up { transform: rotate(180deg); }
.faq-a {
  padding: 0 4px 16px; font-size: 13.5px; line-height: 1.6; color: var(--text-sub);
  white-space: pre-line;
}
</style>
