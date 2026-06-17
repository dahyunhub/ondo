<script setup>
// jaram-ui.jsx SproutLoader 포팅 — 자라는 새싹(빈 상태/로딩 일러스트)
defineProps({ size: { type: [Number, String], default: 92 } })
</script>

<template>
  <div class="jr-sprout" :style="{ width: size + 'px', height: size + 'px', position: 'relative' }">
    <svg :width="size" :height="size" viewBox="0 0 96 96" fill="none">
      <g class="jr-sun" style="transform-origin:48px 18px">
        <line v-for="i in 8" :key="i" x1="48" y1="2" x2="48" y2="8" stroke="#FFD45E" stroke-width="3"
              stroke-linecap="round" :transform="`rotate(${(i - 1) * 45} 48 18)`" />
      </g>
      <circle cx="48" cy="18" r="8" fill="#FFD45E" />
      <path d="M30 72 h36 l-3 14 a4 4 0 0 1-4 3 h-22 a4 4 0 0 1-4-3 z" fill="#F0D9B8" />
      <rect x="27" y="68" width="42" height="7" rx="3.5" fill="#E7C79B" />
      <g class="jr-stem" style="transform-origin:48px 72px">
        <path d="M48 72 V52" stroke="#7FBF8E" stroke-width="4" stroke-linecap="round" />
        <path class="jr-leaf jr-leaf-l" d="M48 60 C40 58 35 52 36 46 C44 46 49 51 48 60 Z" fill="#8FD9A8" />
        <path class="jr-leaf jr-leaf-r" d="M48 56 C56 54 61 48 60 42 C52 42 47 47 48 56 Z" fill="#A8E0BB" />
      </g>
    </svg>
  </div>
</template>
