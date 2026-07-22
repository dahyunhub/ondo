<script setup>
// 카카오 인가 리다이렉트 콜백. URL 의 ?code= 를 백엔드로 넘겨 로그인/가입을 마친 뒤 홈으로 이동한다.
// redirect_uri 는 인가 요청 때와 동일해야 카카오 토큰 교환이 통과하므로 origin 으로 동일하게 계산한다.
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import Logo from '../components/Logo.vue'

const router = useRouter()
const error = ref('')

function goLoginWithError(message) {
  error.value = message
  // 잠깐 안내 후 로그인 화면으로. (사용자가 바로 다시 시도할 수 있게)
  setTimeout(() => router.replace({ name: 'login' }), 1600)
}

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  const returnedState = params.get('state')
  const kakaoError = params.get('error')

  // 시작 시 저장한 state 를 꺼내고 즉시 폐기(일회성).
  const savedState = sessionStorage.getItem('ondo.kakao.state')
  sessionStorage.removeItem('ondo.kakao.state')

  if (kakaoError) {
    // 사용자가 동의를 취소했거나 카카오가 오류를 반환한 경우.
    goLoginWithError('카카오 로그인이 취소되었어요.')
    return
  }
  if (!code) {
    goLoginWithError('카카오 인증 정보를 받지 못했어요.')
    return
  }
  // state 불일치/부재 = 이 브라우저가 시작하지 않은 콜백(위조) → 코드 전송하지 않고 차단.
  if (!returnedState || returnedState !== savedState) {
    goLoginWithError('카카오 로그인 요청이 유효하지 않아요. 다시 시도해 주세요.')
    return
  }

  try {
    const redirectUri = window.location.origin + '/oauth/kakao/callback'
    const res = await api.post('/auth/kakao', { code, redirectUri }, { auth: false })
    auth.setSession({ accessToken: res.accessToken, teacher: res.teacher })
    router.replace({ name: session.classroom ? 'home' : 'classrooms' })
  } catch (e) {
    goLoginWithError(e instanceof ApiError ? e.message : '카카오 로그인 중 문제가 발생했어요.')
  }
})
</script>

<template>
  <div class="cb fullscreen">
    <Logo variant="vertical" :height="120" />
    <p v-if="!error" class="msg">카카오 로그인 중이에요…</p>
    <p v-else class="err">{{ error }}</p>
  </div>
</template>

<style scoped>
.cb {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 18px; min-height: 100vh; padding: 24px; text-align: center;
}
.msg { font-size: 15px; color: var(--text-sub); font-weight: 600; }
.err { font-size: 14.5px; color: var(--warn); font-weight: 700; }
</style>
