<script setup>
// 자람 로고 — 배경 제거(투명) + trim 된 원본을 public/logos 에서 사용.
// 모든 변형을 높이 기준으로 표시(가로폭은 비율 유지). 콘텐츠가 꽉 차게 잘려 있어 height 만으로 정확히 맞는다.
// variant: mark(마크) | lockup(가로) | vertical(세로) | title(자람 글자)
const props = defineProps({
  variant: { type: String, default: 'mark' },
  height: { type: [Number, String], default: 40 },
})

const SRC = {
  mark: '/logos/mark.png',
  lockup: '/logos/lockup.png',
  vertical: '/logos/vertical.png',
  title: '/logos/title.png',
}
</script>

<template>
  <img
    :src="SRC[variant] || SRC.mark" alt="자람"
    :style="{ height: height + 'px', width: 'auto', objectFit: 'contain', display: 'block', flex: '0 0 auto' }"
  />
</template>
