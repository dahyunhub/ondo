// 인증 상태 — 경량 reactive 스토어(Pinia 미사용). localStorage 영속.
import { reactive } from 'vue'
import { session } from './session'

const TOKEN_KEY = 'ondo.token'
const TEACHER_KEY = 'ondo.teacher'

// '로그인 상태 유지'에 따라 저장소가 갈린다:
//   유지 ON  → localStorage   (브라우저를 닫았다 열어도 로그인 유지)
//   유지 OFF → sessionStorage (탭/브라우저를 닫으면 로그아웃 — 공용 PC 대비)
// 읽을 때는 둘 다 살펴, 이전에 어느 쪽에 저장했든 세션을 복구한다.
function readStored(key) {
  return localStorage.getItem(key) ?? sessionStorage.getItem(key)
}

function loadTeacher() {
  try { return JSON.parse(readStored(TEACHER_KEY)) } catch { return null }
}

export const auth = reactive({
  token: readStored(TOKEN_KEY) || null,
  teacher: loadTeacher(),

  get isAuthenticated() {
    return !!this.token
  },

  setSession({ accessToken, teacher, persist = true }) {
    // 다른 교사로 인증되면(가입·로그인 공통) 이전 교사의 반 선택을 비운다 —
    // 같은 브라우저에 남은 ondo.classroom 을 신규/타 교사가 재사용하는 것을 막는다.
    if (teacher?.id !== this.teacher?.id) {
      session.clear()
    }
    this.token = accessToken
    this.teacher = teacher
    const store = persist ? localStorage : sessionStorage
    const other = persist ? sessionStorage : localStorage
    store.setItem(TOKEN_KEY, accessToken)
    store.setItem(TEACHER_KEY, JSON.stringify(teacher))
    // 반대편 저장소에 남은 값 제거 — 유지 ON↔OFF 전환 시 옛 토큰이 되살아나지 않게.
    other.removeItem(TOKEN_KEY)
    other.removeItem(TEACHER_KEY)
  },

  // 비밀번호를 바꾸면 서버가 이전 토큰을 전부 무효화하고 새 토큰을 내려준다.
  // 지금 기기가 곧바로 로그아웃되지 않도록 토큰만 갈아끼운다(교사 정보·반 선택은 유지).
  replaceToken(accessToken) {
    this.token = accessToken
    // 현재 토큰이 들어있는 저장소를 그대로 따라간다('로그인 상태 유지' 설정을 뒤집지 않도록).
    const store = localStorage.getItem(TOKEN_KEY) ? localStorage : sessionStorage
    store.setItem(TOKEN_KEY, accessToken)
  },

  // 교사 정보 부분 갱신(프로필 사진 등) — 세션(반 선택)은 건드리지 않는다.
  updateTeacher(patch) {
    this.teacher = { ...(this.teacher || {}), ...patch }
    // 현재 토큰이 들어있는 저장소에 맞춰 갱신한다.
    const store = localStorage.getItem(TOKEN_KEY) ? localStorage : sessionStorage
    store.setItem(TEACHER_KEY, JSON.stringify(this.teacher))
  },

  logout() {
    this.token = null
    this.teacher = null
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(TEACHER_KEY)
    sessionStorage.removeItem(TOKEN_KEY)
    sessionStorage.removeItem(TEACHER_KEY)
  },
})
