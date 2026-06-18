// 반응형 분기 — 데스크톱(사이드바) ↔ 모바일(하단 탭) 전환 기준.
// 목업이 화면마다 Mobile/Desktop 두 레이아웃을 정의하므로, 폭으로 분기한다.
import { ref, onMounted, onBeforeUnmount } from 'vue'

const DESKTOP_QUERY = '(min-width: 900px)'

// 모듈 전역 단일 상태 — 여러 컴포넌트가 같은 값을 공유.
const isDesktop = ref(
  typeof window !== 'undefined' && window.matchMedia
    ? window.matchMedia(DESKTOP_QUERY).matches
    : false,
)

let mql = null
let listeners = 0

function onChange(e) { isDesktop.value = e.matches }

export function useViewport() {
  onMounted(() => {
    if (typeof window === 'undefined' || !window.matchMedia) return
    if (!mql) {
      mql = window.matchMedia(DESKTOP_QUERY)
      isDesktop.value = mql.matches
    }
    if (listeners === 0) mql.addEventListener('change', onChange)
    listeners += 1
  })
  onBeforeUnmount(() => {
    listeners = Math.max(0, listeners - 1)
    if (listeners === 0 && mql) mql.removeEventListener('change', onChange)
  })
  return { isDesktop }
}
