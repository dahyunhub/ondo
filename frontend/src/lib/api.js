// 자람 API 클라이언트 — fetch 래퍼.
// 개발 중엔 Vite 프록시(/api → :8080)를 통해 호출한다.
import { auth } from '../stores/auth'

const BASE = '/api/v1'

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

  // 401: 토큰 만료/무효 → 로그아웃하고 로그인으로
  if (res.status === 401) {
    auth.logout()
    if (location.hash !== '#/login' && location.pathname !== '/login') {
      window.location.assign('/login')
    }
    throw new ApiError(401, 'AUTH_UNAUTHENTICATED', '로그인이 필요해요.')
  }

  if (res.status === 204) return null

  let data = null
  const text = await res.text()
  if (text) {
    try { data = JSON.parse(text) } catch { data = text }
  }

  if (!res.ok) {
    const code = data && data.code
    const message = (data && data.message) || `요청 실패 (${res.status})`
    throw new ApiError(res.status, code, message, data)
  }
  return data
}

export const api = {
  get: (p, opts) => request('GET', p, opts),
  post: (p, body, opts) => request('POST', p, { ...opts, body }),
  put: (p, body, opts) => request('PUT', p, { ...opts, body }),
  patch: (p, body, opts) => request('PATCH', p, { ...opts, body }),
  del: (p, opts) => request('DELETE', p, opts),
}
