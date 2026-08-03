<script setup>
// 자라는 새싹 로더 — 화분에서 새싹이 돋고 해가 떠오르는 등장 애니메이션 뒤
// 흔들림·잎 나부낌·해 맥동·꽃가루 상승이 이어지는 로딩/빈 상태 일러스트.
// (카피 문구는 이 컴포넌트를 쓰는 화면에서 각자 붙인다 — 여기선 그림만.)
import { computed, useId } from 'vue'

const props = defineProps({ size: { type: [Number, String], default: 92 } })

// size = 세로 높이. 새싹 원본 비율(2:3)을 지켜 가로를 맞춘다.
const h = computed(() => Number(props.size))
const w = computed(() => Math.round(h.value * 2 / 3))

// 한 페이지에 여러 로더가 떠도 그라데이션 id 가 충돌하지 않도록 인스턴스마다 유니크 접미사.
const uid = useId()
const id = (n) => `${n}-${uid}`
const url = (n) => `url(#${n}-${uid})`
</script>

<template>
  <div class="jr-sprout" :style="{ width: w + 'px', height: h + 'px' }">
    <svg class="sprout" :width="w" :height="h" viewBox="0 0 200 300" fill="none" role="img"
         aria-label="새싹이 자라는 로딩 애니메이션">
      <defs>
        <radialGradient :id="id('gSun')" cx="42%" cy="38%" r="70%">
          <stop offset="0%" stop-color="#FFE79B" /><stop offset="55%" stop-color="#FFD45E" /><stop offset="100%" stop-color="#F5B62E" />
        </radialGradient>
        <linearGradient :id="id('gLeafA')" x1="0" y1="1" x2="1" y2="0">
          <stop offset="0%" stop-color="#7FCB92" /><stop offset="100%" stop-color="#9EDCA8" />
        </linearGradient>
        <linearGradient :id="id('gLeafB')" x1="0" y1="1" x2="1" y2="0">
          <stop offset="0%" stop-color="#6CBE86" /><stop offset="100%" stop-color="#86D19A" />
        </linearGradient>
        <linearGradient :id="id('gPot')" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stop-color="#D4A06A" /><stop offset="100%" stop-color="#C6905A" />
        </linearGradient>
        <filter :id="id('soft')" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="3.4" /></filter>
      </defs>

      <!-- 바닥 그림자 -->
      <ellipse class="shadow" cx="100" cy="233" rx="52" ry="10" fill="#C9A05E" opacity=".22" />

      <!-- 해 -->
      <circle class="sun-glow" cx="100" cy="54" r="30" fill="#FFE59A" :filter="url('soft')" />
      <g class="sun-rays">
        <g class="sun-rays-in">
          <g fill="#FBC63C">
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(0 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(45 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(90 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(135 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(180 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(225 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(270 100 54)" />
            <rect x="97.5" y="19" width="5" height="10" rx="2.5" transform="rotate(315 100 54)" />
          </g>
        </g>
      </g>
      <circle class="sun-core" cx="100" cy="54" r="19" :fill="url('gSun')" />

      <!-- 새싹 (화분 뒤) -->
      <g class="plant">
        <path class="stem" pathLength="1" d="M100 181 C99 170 101 158 100 148 C99.4 140 100 134 100 128" stroke="#63B486" stroke-width="6" stroke-linecap="round" />
        <!-- 오른쪽 잎 -->
        <g class="leaf-r">
          <path d="M100 130 C120 130 134 118 131 101 C112 99 101 110 100 130 Z" :fill="url('gLeafB')" />
          <path class="rib" pathLength="1" d="M100 128 C114 124 124 116 129 103" stroke="#57A47C" stroke-width="1.6" stroke-linecap="round" opacity=".5" fill="none" />
        </g>
        <!-- 왼쪽 잎 (대칭) -->
        <g class="leaf-l">
          <path d="M100 130 C80 130 66 118 69 101 C88 99 99 110 100 130 Z" :fill="url('gLeafA')" />
          <path class="rib" pathLength="1" d="M100 128 C86 124 76 116 71 103" stroke="#57A47C" stroke-width="1.6" stroke-linecap="round" opacity=".5" fill="none" />
        </g>
      </g>

      <!-- 화분 (새싹 앞) -->
      <g class="pot">
        <ellipse cx="100" cy="176" rx="40" ry="7" fill="#A9744A" />
        <path d="M60 182 L140 182 L131 222 Q130 226 125 226 L75 226 Q70 226 69 222 Z" :fill="url('gPot')" />
        <path d="M131 222 Q130 226 125 226 L75 226 Q70 226 69 222 L68 216 Q100 222 132 216 Z" fill="#000" opacity=".05" />
        <rect x="53" y="167" width="94" height="16" rx="7" fill="#E2B37F" />
        <rect x="53" y="167" width="94" height="6" rx="3" fill="#fff" opacity=".22" />
      </g>

      <!-- 반짝이는 꽃가루 -->
      <circle class="pollen p1" style="--dx:-8px" cx="132" cy="150" r="2.4" fill="#FFD45E" />
      <circle class="pollen p2" style="--dx:10px" cx="70" cy="158" r="2" fill="#7FCB92" />
      <circle class="pollen p3" style="--dx:4px" cx="118" cy="168" r="1.8" fill="#F5B62E" />
    </svg>
  </div>
</template>

<style scoped>
.jr-sprout { position: relative; display: inline-block; }
.sprout { display: block; overflow: visible; }
.sprout * { transform-box: view-box; }

/* 각 파트 pivot */
.plant { transform-origin: 100px 182px; }
.leaf-l { transform-origin: 100px 130px; }
.leaf-r { transform-origin: 100px 130px; }
.pot { transform-origin: 100px 224px; }
.sun-rays { transform-origin: 100px 54px; }
.sun-rays-in { transform-origin: 100px 54px; }
.sun-core, .sun-glow { transform-origin: 100px 54px; }
.shadow { transform-origin: 100px 233px; }

/* stem draw-up */
.stem { stroke-dasharray: 1; stroke-dashoffset: 1; animation: stemDraw .85s cubic-bezier(.45, .05, .2, 1) .3s both; }
.rib { stroke-dasharray: 1; stroke-dashoffset: 1; animation: stemDraw .5s ease-out .9s both; }

/* 등장(팝) + 이후 생명감 모션 */
.sun-glow { opacity: 0; animation: glowIn .7s ease .1s both, glowPulse 3s ease-in-out 1.4s infinite; }
.sun-core { transform: scale(0); animation: pop .7s cubic-bezier(.34, 1.56, .64, 1) .25s both, beat 2.8s ease-in-out 1.4s infinite; }
.sun-rays { animation: spin 5s linear .4s infinite; }
.sun-rays-in { opacity: 0; animation: raysIn .8s cubic-bezier(.34, 1.56, .64, 1) .35s both; }
.pot { transform: translateY(10px) scale(.9); opacity: 0; animation: potIn .7s cubic-bezier(.34, 1.56, .64, 1) both; }
.leaf-l { transform: scale(0); animation: pop .65s cubic-bezier(.34, 1.56, .64, 1) .78s both, flutterL 3.1s ease-in-out 1.5s infinite; }
.leaf-r { transform: scale(0); animation: pop .65s cubic-bezier(.34, 1.56, .64, 1) .92s both, flutterR 3.6s ease-in-out 1.5s infinite; }
.plant { animation: sway 3.8s ease-in-out 1.5s infinite; }
.shadow { animation: shadowIn .6s ease both, shadowBreathe 3.8s ease-in-out 1.5s infinite; }
.pollen { opacity: 0; }
.pollen.p1 { animation: rise 4.2s ease-in-out 2s infinite; }
.pollen.p2 { animation: rise 5s ease-in-out 3.1s infinite; }
.pollen.p3 { animation: rise 4.6s ease-in-out 4s infinite; }

@keyframes stemDraw { to { stroke-dashoffset: 0; } }
@keyframes pop { 0% { transform: scale(0); } 62% { transform: scale(1.1); } 100% { transform: scale(1); } }
@keyframes potIn { 0% { transform: translateY(10px) scale(.9); opacity: 0; } 60% { transform: translateY(0) scale(1.03); } 100% { transform: translateY(0) scale(1); opacity: 1; } }
@keyframes glowIn { to { opacity: 1; } }
@keyframes raysIn { 0% { opacity: 0; transform: scale(.4) rotate(-25deg); } 100% { opacity: 1; transform: scale(1) rotate(0); } }
@keyframes shadowIn { from { opacity: 0; } to { opacity: 1; } }

/* 흔들림 — 시작=끝(0deg) 이라 이음매 없이 반복 */
@keyframes sway { 0% { transform: rotate(0); } 25% { transform: rotate(2.4deg); } 50% { transform: rotate(0); } 75% { transform: rotate(-2.4deg); } 100% { transform: rotate(0); } }
@keyframes flutterL { 0% { transform: rotate(0); } 25% { transform: rotate(-5deg); } 50% { transform: rotate(0); } 75% { transform: rotate(4deg); } 100% { transform: rotate(0); } }
@keyframes flutterR { 0% { transform: rotate(0); } 25% { transform: rotate(5deg); } 50% { transform: rotate(0); } 75% { transform: rotate(-4deg); } 100% { transform: rotate(0); } }
@keyframes beat { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.06); } }
@keyframes glowPulse { 0%, 100% { opacity: .5; transform: scale(1); } 50% { opacity: .8; transform: scale(1.14); } }
@keyframes spin { to { transform: rotate(360deg); } }
@keyframes shadowBreathe { 0%, 100% { transform: scaleX(1); opacity: 1; } 50% { transform: scaleX(.9); opacity: .82; } }
@keyframes rise { 0% { opacity: 0; transform: translate(0, 0) scale(.6); } 20% { opacity: .7; } 80% { opacity: .5; } 100% { opacity: 0; transform: translate(var(--dx, 6px), -46px) scale(1); } }

@media (prefers-reduced-motion: reduce) {
  .sprout * { animation: none !important; }
  .stem, .rib { stroke-dashoffset: 0; }
  .sun-core, .leaf-l, .leaf-r { transform: scale(1); }
  .sun-glow { opacity: .55; }
  .sun-rays, .shadow { opacity: 1; }
  .pot { opacity: 1; transform: none; }
  .pollen { opacity: 0; }
}
</style>
