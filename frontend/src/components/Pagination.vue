<script setup>
// 공통 페이지네이션 — 서비스 전 페이지에서 같은 규칙으로 쓴다.
// 페이지가 아무리 많아도 한 번에 최대 5개 번호만 보여준다(블록 단위).
// 다음 묶음이 있으면 뒤에 ···, 이전 묶음이 있으면 앞에 ··· 를 두고,
// ··· 를 누르면 그 묶음(예: 6~10)으로 넘어간다. v-model:page 로 현재 페이지를 주고받는다.
import { computed } from 'vue'
import AppIcon from './AppIcon.vue'

const props = defineProps({
  page: { type: Number, required: true },      // 현재 페이지(1부터)
  totalPages: { type: Number, required: true },
  block: { type: Number, default: 5 },         // 한 번에 보여줄 번호 개수
})
const emit = defineEmits(['update:page'])

// 현재 페이지가 속한 묶음의 시작·끝 번호.
const blockStart = computed(() => Math.floor((props.page - 1) / props.block) * props.block + 1)
const blockEnd = computed(() => Math.min(blockStart.value + props.block - 1, props.totalPages))
const pages = computed(() => {
  const arr = []
  for (let p = blockStart.value; p <= blockEnd.value; p++) arr.push(p)
  return arr
})
const hasPrev = computed(() => blockStart.value > 1)              // 이전 묶음 존재
const hasNext = computed(() => blockEnd.value < props.totalPages) // 다음 묶음 존재

function go(p) {
  const t = Math.min(Math.max(1, p), props.totalPages)
  if (t !== props.page) emit('update:page', t)
}
</script>

<template>
  <nav v-if="totalPages > 1" class="pager">
    <button class="pg-arrow" :disabled="page === 1" aria-label="이전 페이지" @click="go(page - 1)">
      <AppIcon name="chevL" :size="18" />
    </button>
    <!-- 앞 ··· : 이전 묶음 마지막 페이지로 이동 → 이전 묶음이 보인다 -->
    <button v-if="hasPrev" class="pg-ellipsis" aria-label="이전 페이지 묶음" @click="go(blockStart - 1)">···</button>
    <button v-for="p in pages" :key="p" class="pg-num" :class="{ on: p === page }" @click="go(p)">{{ p }}</button>
    <!-- 뒤 ··· : 다음 묶음 첫 페이지로 이동 → 다음 묶음(예: 6~10)이 보인다 -->
    <button v-if="hasNext" class="pg-ellipsis" aria-label="다음 페이지 묶음" @click="go(blockEnd + 1)">···</button>
    <button class="pg-arrow" :disabled="page === totalPages" aria-label="다음 페이지" @click="go(page + 1)">
      <AppIcon name="chevR" :size="18" />
    </button>
  </nav>
</template>

<style scoped>
.pager { display: flex; align-items: center; justify-content: center; gap: 5px; margin-top: 6px; flex-wrap: wrap; }
.pg-arrow, .pg-num, .pg-ellipsis {
  min-width: 32px; height: 34px; padding: 0 4px; border-radius: 10px; border: 1.5px solid var(--hair);
  background: var(--surface); color: var(--text-sub); font-family: inherit; font-size: 14px; font-weight: 700;
  cursor: pointer; display: inline-flex; align-items: center; justify-content: center;
  transition: border-color .12s, background .12s, color .12s;
}
.pg-arrow:hover:not(:disabled), .pg-num:hover, .pg-ellipsis:hover { border-color: var(--brand-500); }
.pg-arrow:disabled { opacity: .4; cursor: not-allowed; }
.pg-num.on { background: var(--brand-500); border-color: var(--brand-500); color: var(--text); }
/* ··· 는 클릭 대상이지만 번호처럼 강조하지 않는다 — 묶음 이동 힌트 */
.pg-ellipsis { letter-spacing: 1px; color: var(--text-faint); }
</style>
