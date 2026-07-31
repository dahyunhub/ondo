<script setup>
// 생년월일 입력 — 연/월/일 셀렉트 3개(spec-classroom-age-birthdate).
//
// 네이티브 <input type="date"> 를 쓰지 않는 이유: 값이 비어 있을 때 달력이 열리는 위치를
// 지정하는 속성이 없다. min/max 로 위치를 잡으면 그 밖 연도가 막히고(혼합연령반 등록 불가),
// value 를 미리 채우면 교사가 못 보고 저장했을 때 틀린 생년월일이 조용히 들어간다.
// 셀렉트는 '기본 표시 연도'와 '선택 가능 범위'를 분리할 수 있어 둘 다 피한다.
//
// 상태를 modelValue 에서만 읽으면 안 된다 — modelValue 는 셋이 다 정해져야 채워지므로
// 중간 선택(예: 월만 고른 상태)이 유실돼 날짜가 영원히 완성되지 않는다. 그래서 내부 상태를 둔다.
import { ref, computed, watch } from 'vue'
import { birthYearOptions } from '../lib/birthYear'

const props = defineProps({
  /** YYYY-MM-DD 또는 '' */
  modelValue: { type: String, default: '' },
  /** 아직 고르지 않았을 때 연도 셀렉트에 보일 연도(반 연령으로 계산) */
  defaultYear: { type: [Number, String], default: null },
  /** 연도 목록의 기준 학년도 */
  classroomYear: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:modelValue'])

const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1)

const y = ref(null)
const m = ref(null)
const d = ref(null)

function parse(value) {
  return /^(\d{4})-(\d{2})-(\d{2})$/.exec(value || '')
}

// 바깥에서 값이 들어오면(수정 화면 초기값 등) 내부 상태에 반영한다.
watch(() => props.modelValue, (value) => {
  const hit = parse(value)
  if (!hit) return // 빈 값은 내부 선택을 지우지 않는다 — 아직 고르는 중일 수 있다
  const [, yy, mm, dd] = hit
  y.value = +yy; m.value = +mm; d.value = +dd
}, { immediate: true })

/** 연도는 아직 안 골랐어도 기본값을 '보여준다'. 이게 이 컴포넌트의 존재 이유. */
const shownYear = computed(() => y.value ?? (props.defaultYear ? Number(props.defaultYear) : null))

const years = computed(() => {
  const list = birthYearOptions(props.classroomYear)
  // 저장된 값이 목록 밖이면(유예·전학 등) 그 연도도 함께 보여준다 — 수정 화면에서 값이 사라지면 안 된다.
  return y.value && !list.includes(y.value) ? [...list, y.value].sort((a, b) => b - a) : list
})

function daysIn(year, month) {
  if (!year || !month) return 31
  return new Date(year, month, 0).getDate() // month 는 1-based, day 0 = 이전 달 말일
}
const days = computed(() => Array.from({ length: daysIn(shownYear.value, m.value) }, (_, i) => i + 1))

const pad = (n) => String(n).padStart(2, '0')

/** 셋이 다 정해졌을 때만 확정한다. 연도만 보인 상태로 저장되면 안 된다(미리 채우기 금지). */
function sync() {
  const year = shownYear.value
  if (!year || !m.value || !d.value) {
    emit('update:modelValue', '')
    return
  }
  // 월·연이 바뀌어 일이 범위를 넘으면 그 달 마지막 날로 줄인다(2월 30일·2021-02-29 방지).
  d.value = Math.min(d.value, daysIn(year, m.value))
  emit('update:modelValue', `${year}-${pad(m.value)}-${pad(d.value)}`)
}

function onYear(e) { y.value = Number(e.target.value) || null; sync() }
function onMonth(e) { m.value = Number(e.target.value) || null; sync() }
function onDay(e) { d.value = Number(e.target.value) || null; sync() }
</script>

<template>
  <div class="bd">
    <select class="jr-input bd-sel" :value="shownYear ?? ''" @change="onYear">
      <option value="" disabled>연도</option>
      <option v-for="year in years" :key="year" :value="year">{{ year }}년</option>
    </select>
    <select class="jr-input bd-sel" :value="m ?? ''" @change="onMonth">
      <option value="" disabled>월</option>
      <option v-for="month in MONTHS" :key="month" :value="month">{{ month }}월</option>
    </select>
    <select class="jr-input bd-sel" :value="d ?? ''" @change="onDay">
      <option value="" disabled>일</option>
      <option v-for="day in days" :key="day" :value="day">{{ day }}일</option>
    </select>
  </div>
</template>

<style scoped>
.bd { display: flex; gap: 8px; }
.bd-sel {
  flex: 1; min-width: 0; cursor: pointer; font-family: inherit;
  /* 네이티브 화살표를 남겨 셀렉트임을 드러낸다 — appearance 를 지우면 탭 가능한지 안 보인다. */
  padding-right: 8px;
}
</style>
