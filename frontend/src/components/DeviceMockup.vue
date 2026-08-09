<script setup>
// 소개 페이지 등에서 재사용하는 아이폰형 디바이스 목업 프레임.
// 화면 내용은 slot 으로 받는다(부모 스코프 스타일이 그대로 적용됨).
</script>

<template>
  <div class="dm">
    <div class="dm-notch" aria-hidden="true"></div>
    <div class="dm-screen"><slot /></div>
  </div>
</template>

<style scoped>
.dm {
  width: min(86vw, 300px);
  aspect-ratio: 300 / 620;
  background: #221d15;
  border-radius: 42px;
  padding: 11px;
  box-shadow: 0 24px 50px rgba(120, 90, 40, 0.28), inset 0 0 0 2px rgba(255, 255, 255, 0.05);
  position: relative;
  flex: 0 0 auto;
}
.dm-notch {
  position: absolute;
  top: 16px;
  left: 50%;
  transform: translateX(-50%);
  width: 92px;
  height: 20px;
  background: #221d15;
  border-radius: 999px;
  z-index: 3;
}
.dm-screen {
  width: 100%;
  height: 100%;
  /* --bg 는 상위 .jr 토큰에서 상속받는다 */
  background: var(--bg, #fffcf5);
  border-radius: 32px;
  overflow: hidden;
  padding: 30px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: left;
}
</style>
