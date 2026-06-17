<script setup>
import { computed } from 'vue'
import { areaMeta } from '../lib/areas'

// area: 백엔드 enum 값(PHYSICAL_HEALTH 등) 또는 null(미분류)
const props = defineProps({
  area: { type: String, default: null },
  dot: { type: Boolean, default: true },
})
const meta = computed(() => areaMeta(props.area))
</script>

<template>
  <span class="jr-chip" :class="'jr-area-' + meta.code">
    <span v-if="dot" class="dot" />{{ meta.ko }}
  </span>
</template>
