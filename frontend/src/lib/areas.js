// 누리과정 5영역 — 백엔드 enum ↔ 디자인 코드/색/한글 매핑.
// 백엔드는 enum 값(SOCIAL 등)을 주고받고, 한글·색은 프론트가 매핑(design-api-alignment).

export const AREA_META = {
  PHYSICAL_HEALTH: { ko: '신체운동·건강', code: 'body', color: '#FF9E80' },
  COMMUNICATION: { ko: '의사소통', code: 'comm', color: '#7EC4E8' },
  SOCIAL: { ko: '사회관계', code: 'social', color: '#FFC94D' },
  ART: { ko: '예술경험', code: 'art', color: '#C9A8E8' },
  NATURE: { ko: '자연탐구', code: 'nature', color: '#8FD9A8' },
}

export const UNCAT = { ko: '미분류', code: 'uncat', color: '#B7AFA2' }

/** enum 값(또는 null) → 메타. null/미분류는 UNCAT. */
export function areaMeta(value) {
  return value ? (AREA_META[value] || UNCAT) : UNCAT
}

// 영역 지정(수정) 메뉴 순서 — 미분류 제외 5영역
export const AREA_ORDER = ['PHYSICAL_HEALTH', 'COMMUNICATION', 'SOCIAL', 'ART', 'NATURE']

// 타임라인 필터 칩 순서 — 전체는 별도, 여기에 미분류(UNCLASSIFIED) 포함
export const FILTER_ORDER = [...AREA_ORDER, 'UNCLASSIFIED']
