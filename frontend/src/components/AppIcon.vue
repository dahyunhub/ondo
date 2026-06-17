<script setup>
import { computed } from 'vue'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: 22 },
  stroke: { type: [Number, String], default: 2 },
})

// jaram-ui.jsx Icon 의 둥근 라인 아이콘 포팅(필요분). 모두 currentColor stroke.
const PATHS = {
  chevR: '<path d="M9 5l7 7-7 7"/>',
  chevL: '<path d="M15 5l-7 7 7 7"/>',
  chevD: '<path d="M6 9l6 6 6-6"/>',
  back: '<path d="M11 5l-7 7 7 7M4 12h16"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  check: '<path d="M4.5 12.5l4.5 4.5L19.5 6.5"/>',
  x: '<path d="M6 6l12 12M18 6L6 18"/>',
  cal: '<rect x="3.5" y="5" width="17" height="16" rx="3"/><path d="M3.5 9.5h17M8 3v4M16 3v4"/>',
  sun: '<circle cx="12" cy="12" r="4"/><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.2 5.2l1.8 1.8M17 17l1.8 1.8M18.8 5.2L17 7M7 17l-1.8 1.8"/>',
  leaf: '<path d="M5 19c0-8 6-13 14-13 0 8-5 14-13 14-1 0-1-1-1-1z"/><path d="M5 19c3-4 6-6 10-7.5"/>',
  home: '<path d="M3 10.5L12 3l9 7.5"/><path d="M5 9.5V20h14V9.5"/><path d="M9.5 20v-5h5v5"/>',
  children: '<circle cx="8" cy="8" r="3"/><circle cx="16.5" cy="9.5" r="2.4"/><path d="M2.5 19c0-3 2.5-5 5.5-5s5.5 2 5.5 5"/><path d="M14.5 19c.2-2.4 1.8-4 4-4 1.6 0 3 .9 3.5 2.3"/>',
  journal: '<rect x="4" y="3" width="16" height="18" rx="2.5"/><path d="M8 8h8M8 12h8M8 16h5"/>',
  me: '<circle cx="12" cy="8" r="3.4"/><path d="M4.5 20c.5-3.8 3.6-6 7.5-6s7 2.2 7.5 6"/>',
  swap: '<path d="M7 4L3 8l4 4"/><path d="M3 8h13a5 5 0 0 1 0 10h-2"/>',
  logout: '<path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3"/><path d="M10 8l-4 4 4 4M6 12h10"/>',
  sparkle: '<path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8L12 3z"/><path d="M19 15l.7 2 2 .7-2 .7-.7 2-.7-2-2-.7 2-.7.7-2z"/>',
  heart: '<path d="M12 20s-7-4.5-7-9.5A3.8 3.8 0 0 1 12 7a3.8 3.8 0 0 1 7 3c0 5-7 9.5-7 9.5z"/>',
  search: '<circle cx="11" cy="11" r="6.5"/><path d="M16 16l4 4"/>',
  expand: '<path d="M9 4H4v5M15 4h5v5M9 20H4v-5M15 20h5v-5"/>',
  clock: '<circle cx="12" cy="12" r="8.5"/><path d="M12 7v5l3.5 2"/>',
  pencil: '<path d="M4 20l4-1 11-11-3-3L5 16l-1 4z"/><path d="M14.5 5.5l3 3"/>',
  dots: '<circle cx="5" cy="12" r="1.6" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.6" fill="currentColor" stroke="none"/><circle cx="19" cy="12" r="1.6" fill="currentColor" stroke="none"/>',
  help: '<circle cx="12" cy="12" r="8.5"/><path d="M9.5 9.5a2.5 2.5 0 0 1 4.5 1.5c0 1.5-2 2-2 3.5"/><path d="M12 17.5v0"/>',
}

const inner = computed(() => PATHS[props.name] || '')
</script>

<template>
  <svg
    :width="size" :height="size" viewBox="0 0 24 24"
    fill="none" stroke="currentColor" :stroke-width="stroke"
    stroke-linecap="round" stroke-linejoin="round"
    style="display:block; flex:0 0 auto"
    v-html="inner"
  />
</template>
