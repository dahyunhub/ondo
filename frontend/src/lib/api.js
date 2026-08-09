// 온도 API 클라이언트 — fetch 래퍼.
// 개발 중엔 Vite 프록시(/api → :8090)를 통해 호출한다.
import { auth } from '../stores/auth'
import { notice } from '../stores/notice'

const BASE = '/api/v1'

// 읽기 전용 데모 계정이 변경을 시도하면 서버가 이 코드로 막는다 → 화면 어디서든 부드럽게 안내.
function noticeIfDemoReadOnly(code, message) {
  if (code === 'DEMO_READ_ONLY') notice.show(message || '읽기 전용 데모 계정이에요.')
}

/** 표준 에러 응답({code,message,...})을 담는 에러 타입. */
export class ApiError extends Error {
  constructor(status, code, message, body) {
    super(message || code || `HTTP ${status}`)
    this.status = status
    this.code = code
    this.body = body
  }
}

async function request(method, path, { body, auth: needAuth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' }
  if (needAuth && auth.token) headers.Authorization = `Bearer ${auth.token}`

  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 204) return null

  let data = null
  const text = await res.text()
  if (text) {
    try { data = JSON.parse(text) } catch { data = text }
  }

  // 401: 보호 요청의 토큰 만료/무효 → 로그아웃하고 로그인으로.
  // 예외 둘: 인증 불필요 요청(로그인 등)과 AUTH_INVALID_CREDENTIALS(비밀번호 확인 실패 등
  // 자격 검증 실패)는 세션 문제가 아니므로 가로채지 않고 아래 표준 에러 처리로
  // 흘려보내 서버의 실제 메시지를 그대로 노출한다.
  if (res.status === 401 && needAuth && (data && data.code) !== 'AUTH_INVALID_CREDENTIALS') {
    auth.logout()
    if (location.hash !== '#/login' && location.pathname !== '/login') {
      window.location.assign('/login')
    }
    throw new ApiError(401, 'AUTH_UNAUTHENTICATED', '로그인이 필요해요.')
  }

  if (!res.ok) {
    const code = data && data.code
    const message = (data && data.message) || `요청 실패 (${res.status})`
    noticeIfDemoReadOnly(code, message)
    throw new ApiError(res.status, code, message, data)
  }
  return data
}

// 인증 GET → Blob(이미지 등). 404 는 null(사진 없음), 401 은 로그아웃 처리.
async function getBlob(path) {
  const headers = {}
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`
  const res = await fetch(BASE + path, { headers })
  if (res.status === 401) {
    auth.logout()
    throw new ApiError(401, 'AUTH_UNAUTHENTICATED', '로그인이 필요해요.')
  }
  if (res.status === 404) return null
  if (!res.ok) throw new ApiError(res.status, null, `요청 실패 (${res.status})`)
  return res.blob()
}

// 바이너리(이미지) PUT — Content-Type 을 직접 지정해 raw 바이트 전송. 응답 JSON 반환.
async function putBinary(path, blob, contentType) {
  const headers = { 'Content-Type': contentType }
  if (auth.token) headers.Authorization = `Bearer ${auth.token}`
  const res = await fetch(BASE + path, { method: 'PUT', headers, body: blob })
  if (res.status === 401) {
    auth.logout()
    throw new ApiError(401, 'AUTH_UNAUTHENTICATED', '로그인이 필요해요.')
  }
  const text = await res.text()
  let data = null
  if (text) { try { data = JSON.parse(text) } catch { data = text } }
  if (!res.ok) {
    noticeIfDemoReadOnly(data && data.code, data && data.message)
    throw new ApiError(res.status, data && data.code, (data && data.message) || `요청 실패 (${res.status})`, data)
  }
  return data
}

export const api = {
  get: (p, opts) => request('GET', p, opts),
  post: (p, body, opts) => request('POST', p, { ...opts, body }),
  put: (p, body, opts) => request('PUT', p, { ...opts, body }),
  patch: (p, body, opts) => request('PATCH', p, { ...opts, body }),
  del: (p, opts) => request('DELETE', p, opts),
  getBlob,
  putBinary,
}
