<script setup>
import { ref, computed, watch } from 'vue'
import { api } from '../lib/api'

const props = defineProps({
  name: { type: String, default: '' },
  size: { type: String, default: '' }, // '', 'sm', 'lg'
  photoUrl: { type: String, default: '' },           // 사진 바이트 API 경로 (예: /children/1/photo)
  photoKey: { type: [String, Number], default: '' },  // photoUpdatedAt — 캐시 키 + 사진 유무 판단
  // 관찰 온도 링(spec-child-warmth). '' | 'WARM' | 'LOW'.
  // 경고가 아니라 '진함 ↔ 옅음'으로만 표현한다 — 빈 값이면 링 없음(교사 아바타 등 기본).
  warmth: { type: String, default: '' },
})

const AVA_COLORS = ['#EF9D5E', '#62AdD0', '#E0AE3C', '#B07FD6', '#5FBA86', '#E8897C', '#8089D2', '#D483AC']

const color = computed(() => {
  // 이름 입력 전(온보딩 자리표시자 등) 기본색 — 온도 브랜드 노랑
  const name = props.name
  if (!name) return 'var(--brand-500)'
  let h = 0
  for (let i = 0; i < name.length; i++) h = (h * 31 + name.charCodeAt(i)) >>> 0
  return AVA_COLORS[h % AVA_COLORS.length]
})

// 성 제외 이름(가독)
const initial = computed(() => {
  const n = props.name
  if (!n) return '' // 이름 입력 전엔 깔끔한 브랜드색 원(자리표시자) — 사진 추가 배지가 안내 역할
  return n.slice(n.length > 2 ? 1 : 0)
})

const cls = computed(() => 'jr-avatar'
  + (props.size ? ' jr-avatar--' + props.size : '')
  + (props.warmth === 'WARM' ? ' ring-warm' : props.warmth === 'LOW' ? ' ring-low' : ''))

// 사진: 인증 fetch → objectURL. 세션 내 모듈 캐시로 같은 사진(경로+갱신시각)은 한 번만 받는다.
const photoCache = (window.__ondoPhotoCache ||= new Map())
const src = ref('')
const failed = ref(false)
const cacheKey = computed(() => (props.photoUrl && props.photoKey ? `${props.photoUrl}?${props.photoKey}` : ''))
const showImg = computed(() => !!src.value && !failed.value)

async function loadPhoto() {
  const key = cacheKey.value
  src.value = ''
  failed.value = false
  if (!key) return
  if (photoCache.has(key)) { src.value = photoCache.get(key); return }
  try {
    const blob = await api.getBlob(props.photoUrl)
    if (!blob) { failed.value = true; return } // 사진 없음 → 이니셜 폴백
    const url = URL.createObjectURL(blob)
    photoCache.set(key, url)
    if (cacheKey.value === key) src.value = url // 그새 prop 이 바뀌지 않았을 때만 반영
  } catch {
    failed.value = true
  }
}

watch(cacheKey, loadPhoto, { immediate: true })
</script>

<template>
  <img v-if="showImg" :class="cls" :src="src" :alt="name" />
  <span v-else :class="cls" :style="{ background: color }">{{ initial }}</span>
</template>

<style scoped>
img.jr-avatar { object-fit: cover; display: inline-block; }

/* 관찰 온도 링 — box-shadow 라 레이아웃을 밀지 않는다.
   옅은 쪽도 '표식'이 아니라 '덜 칠해진' 느낌이어야 해서 경고색 없이 같은 노랑 계열의 농도 차만 준다. */
.jr-avatar.ring-warm { box-shadow: 0 0 0 3px var(--brand-500); }
.jr-avatar.ring-low { box-shadow: 0 0 0 3px var(--brand-100); }
</style>
