<script setup>
// 온도 마스코트 — 생성 이미지 3종을 좌표로 옮긴 벡터본.
// 세 변형이 같은 마스터 실루엣(몸통 path)을 공유하고 채움 높이·햇살·표정만 갈아끼운다.
//  · warm  최근 기록이 쌓인 상태(따뜻해요)
//  · low   최근 기록이 옅은 상태(자는 중 — 슬픈 얼굴 금지)
//  · intro 기능 소개·온보딩 히어로(상태 표시 아님)
import { computed, useId } from 'vue'

const props = defineProps({
  level: { type: String, default: 'warm' }, // warm | low | intro
  size: { type: [Number, String], default: 128 }, // 높이(px). 너비는 viewBox 비율(0.632배)로 따라온다
  leaf: { type: Boolean, default: null }, // 잎 표시. null이면 변형 기본값(현재 셋 다 표시)
})

const uid = useId() // 한 화면에 여러 개 놓여도 clipPath id가 안 겹치게

// 몸통 실루엣 — 튜브(폭 30, 상단 y=42) + 전구(r 26, 중심 48,118). 세 변형이 이 한 줄을 공유한다.
// 이 path 하나만 고치면 세 변형의 실루엣이 함께 움직인다(형제 유지 장치).
const BODY = 'M33 96.8V42a15 15 0 0 1 30 0v54.8a26 26 0 1 1-30 0z'

const VARIANTS = {
  warm:  { liquidY: 53, rays: [-50, -30, -10, 10, 30, 50], sleeping: false, leaf: true },
  low:   { liquidY: null, rays: [], sleeping: true, leaf: true },
  intro: { liquidY: 69, rays: [-40, 0, 40], sleeping: false, leaf: true },
}

const v = computed(() => VARIANTS[props.level] || VARIANTS.warm)
const showLeaf = computed(() => (props.leaf === null ? v.value.leaf : props.leaf))
const h = computed(() => Number(props.size))
const w = computed(() => Math.round(h.value * 0.632))

// 액체 윗면 — 완만한 물결(수은이 아니라 꿀·햇살처럼 보이게 하는 장치).
// 파장 26(반주기 13) · 진폭 ±2. 튜브 안폭이 26이라 물결이 딱 한 번, 마루→골로 지나간다.
// q로 첫 마루를 만들고 t로 제어점을 반사시키면 골·마루가 자동으로 번갈아 나온다.
// 시작 x=9 — 마디가 튜브 안쪽 벽(35, 61)과 정중앙(48)에 떨어져야 벽에서 잘려 뿔처럼 안 보인다.
const WAVE = `q6.5 -4 13 0${'t13 0'.repeat(5)}`
const liquid = computed(() => {
  const y = v.value.liquidY
  return y ? `M9 ${y}${WAVE}V170H9Z` : null
})
</script>

<template>
  <svg :width="w" :height="h" viewBox="0 0 96 152" fill="none" style="display:block;flex:0 0 auto"
       role="img" :aria-label="level === 'low' ? '기록이 옅어요' : '기록이 따뜻해요'">
    <defs>
      <clipPath :id="`ondo-warmth-${uid}`"><path :d="BODY" /></clipPath>
    </defs>

    <!-- 잎(몸통 뒤에서 살짝 비침) -->
    <path v-if="showLeaf" d="M48 56C56 54 61 48 60 42C52 42 47 47 48 56Z"
          fill="#8FD9A8" transform="translate(18 52)" />

    <!-- 햇살 — 캡 중심(48,57) 기준 r40~50. 몸통에 걸치지 않고 머리 위에서만 퍼진다. -->
    <g stroke="var(--brand-500, #FFD45E)" stroke-width="4.5" stroke-linecap="round">
      <line v-for="a in v.rays" :key="a" x1="48" y1="17" x2="48" y2="7" :transform="`rotate(${a} 48 57)`" />
    </g>

    <!-- 몸통 안쪽(빈 부분) → 액체 → 테두리 순서. 테두리를 마지막에 그려야 띠가 깨끗하다. -->
    <path :d="BODY" fill="var(--brand-100, #FFF6DC)" />
    <g v-if="liquid" :clip-path="`url(#ondo-warmth-${uid})`">
      <path :d="liquid" fill="var(--brand-500, #FFD45E)" />
    </g>
    <path :d="BODY" fill="none" stroke="var(--brand-300, #FFE08A)" stroke-width="4" stroke-linejoin="round" />

    <!-- 얼굴 -->
    <g fill="none" stroke="var(--text, #43392E)" stroke-width="3.2" stroke-linecap="round">
      <template v-if="v.sleeping">
        <!-- 자는 눈은 ‿(아래로 볼록). 입만 옅은 미소로 남겨 '시무룩'이 아니라 '자는 중'으로 읽히게 한다. -->
        <path d="M35 112q4 5 8 0" />
        <path d="M53 112q4 5 8 0" />
      </template>
      <template v-else>
        <circle cx="39" cy="114" r="3.4" fill="var(--text, #43392E)" stroke="none" />
        <circle cx="57" cy="114" r="3.4" fill="var(--text, #43392E)" stroke="none" />
      </template>
      <path d="M42 123q6 6 12 0" />
    </g>

    <!-- zzz -->
    <g v-if="v.sleeping" stroke="var(--brand-500, #FFD45E)" stroke-linecap="round" stroke-linejoin="round" fill="none">
      <path d="M0 0h8l-8 9h8" stroke-width="4.6" transform="translate(72 86) scale(0.6)" />
      <path d="M0 0h8l-8 9h8" stroke-width="3.4" transform="translate(77 70) scale(0.9)" />
      <path d="M0 0h8l-8 9h8" stroke-width="2.7" transform="translate(82 52) scale(1.25)" />
    </g>
  </svg>
</template>
