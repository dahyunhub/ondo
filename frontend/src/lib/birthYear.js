// 반 연령 → 아이 출생연도 계산의 단일 출처(spec-classroom-age-birthdate).
//
// 공식: 출생연도 = 학년도 − (만 나이 + 1)
//   2026학년도 만 4세반 → 2026 − 5 = 2021년생
//   검산: 2024학년도 만 5세반 = 2018년생(2024−6) — 현장 기준과 일치
//
// 오늘 날짜가 아니라 '반의 학년도'로 계산한다. 오늘로 계산하면 3월 경계에서 답이 흔들리고,
// 지난 학년도 반을 열었을 때 틀린 연도가 나온다.

/** 반 연령이 없을 때 쓸 기본 만 나이. 유치원에서 가장 흔한 반이라 헛짚어도 손해가 적다. */
const FALLBACK_AGE_CLASS = 4

/** 연도 목록 하한 오프셋(학년도 − N). 유예 아동까지 덮는다. */
const YEAR_SPAN_BACK = 8

/**
 * 생년월일 입력에서 처음 보여줄 연도.
 * @param {number} classroomYear 반의 학년도
 * @param {number|null} ageClass 만 나이(0~5) 또는 null(미지정·혼합반)
 */
export function birthYearFor(classroomYear, ageClass) {
  const year = Number(classroomYear)
  if (!Number.isInteger(year)) return new Date().getFullYear()
  // null/undefined/'' 를 먼저 걸러낸다 — Number(null) 은 0 이라 그냥 변환하면 '만 0세'로 잡힌다.
  const hasAge = ageClass !== null && ageClass !== undefined && ageClass !== ''
    && Number.isInteger(Number(ageClass))
  const age = hasAge ? Number(ageClass) : FALLBACK_AGE_CLASS
  return year - (age + 1)
}

/**
 * 연도 셀렉트에 넣을 목록(내림차순). 기본 연도만 바꿀 뿐 선택을 제약하지 않는다 —
 * 혼합연령반·조기입학·유예 아동이 흔해서 범위를 닫으면 그 아이를 등록할 수 없다.
 * 상한이 학년도인 이유는 아이가 학년도보다 미래에 태어날 수 없기 때문.
 */
export function birthYearOptions(classroomYear) {
  const year = Number.isInteger(Number(classroomYear))
    ? Number(classroomYear)
    : new Date().getFullYear()
  const years = []
  for (let y = year; y >= year - YEAR_SPAN_BACK; y--) years.push(y)
  return years
}

/** 만 나이 선택지. 어린이집 영아반(만 0세)부터 유치원 만 5세까지. */
export const AGE_CLASS_OPTIONS = [0, 1, 2, 3, 4, 5]
