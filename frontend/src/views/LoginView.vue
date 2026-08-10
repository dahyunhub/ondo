<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Logo from '../components/Logo.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const route = useRoute()
const { isDesktop } = useViewport()

// 소개 페이지의 '시작하기' 등에서 ?tab=signup 으로 넘어오면 가입 탭을 먼저 연다.
const tab = ref(route.query.tab === 'signup' ? 'signup' : 'login') // login | signup
const email = ref('')
const password = ref('')
// 로그인 상태 유지 — ON(기본)은 localStorage 영속, OFF는 sessionStorage(브라우저 닫으면 로그아웃).
const keepLoggedIn = ref(true)
// 회원가입 폼 — 로그인과 입력을 분리해 탭 전환 시 섞이지 않게 한다.
const name = ref('')
const suEmail = ref('')
const suPassword = ref('')
const suPassword2 = ref('')
// 필수 동의 — 아동 개인정보를 다루므로 처리방침·약관 동의 없이는 가입시키지 않는다.
const agree = ref(false)
const loading = ref(false)
const error = ref('')

function switchTab(t) {
  tab.value = t
  error.value = ''
  forgot.value = false
  forgotSent.value = false
}

// 비밀번호 찾기 — 재설정 링크 요청.
// 응답은 가입 여부와 무관하게 항상 같으므로(계정 열거 방지), 화면 문구도 하나뿐이다.
const forgot = ref(false)
const forgotEmail = ref('')
const forgotSent = ref(false)

function openForgot() {
  forgot.value = true
  forgotSent.value = false
  error.value = ''
  forgotEmail.value = email.value // 로그인 칸에 쓰던 주소를 그대로 가져온다
}

function closeForgot() {
  forgot.value = false
  error.value = ''
}

async function submitForgot() {
  error.value = ''
  if (!forgotEmail.value) {
    error.value = '이메일을 입력해 주세요.'
    return
  }
  loading.value = true
  try {
    await api.post('/auth/password-reset/request', { email: forgotEmail.value }, { auth: false })
    forgotSent.value = true
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '요청 중 문제가 발생했어요.'
  } finally {
    loading.value = false
  }
}

// 카카오 인가 페이지로 리다이렉트. redirect_uri 는 콜백 뷰와 동일하게 origin 기준으로 계산한다
// (카카오 토큰 교환 시 값 일치 검증). VITE_KAKAO_CLIENT_ID 는 카카오 REST API 키.
function kakaoLogin() {
  const clientId = import.meta.env.VITE_KAKAO_CLIENT_ID
  if (!clientId) {
    error.value = '카카오 로그인이 아직 설정되지 않았어요.'
    return
  }
  // CSRF 방지용 state — 이 브라우저가 시작한 흐름임을 콜백에서 대조한다(로그인 CSRF/코드 주입 차단).
  const state = crypto.randomUUID()
  sessionStorage.setItem('ondo.kakao.state', state)
  const redirectUri = window.location.origin + '/oauth/kakao/callback'
  const url = 'https://kauth.kakao.com/oauth/authorize'
    + `?client_id=${encodeURIComponent(clientId)}`
    + `&redirect_uri=${encodeURIComponent(redirectUri)}`
    + '&response_type=code'
    + `&state=${encodeURIComponent(state)}`
  window.location.assign(url)
}

function gotoNext() {
  router.replace({ name: session.classroom ? 'home' : 'classrooms' })
}

async function submit() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = '이메일과 비밀번호를 입력해 주세요.'
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', { email: email.value, password: password.value }, { auth: false })
    auth.setSession({ accessToken: res.accessToken, teacher: res.teacher, persist: keepLoggedIn.value })
    gotoNext()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '로그인 중 문제가 발생했어요.'
  } finally {
    loading.value = false
  }
}

async function submitSignup() {
  error.value = ''
  if (!name.value || !suEmail.value || !suPassword.value || !suPassword2.value) {
    error.value = '모든 항목을 입력해 주세요.'
    return
  }
  if (suPassword.value.length < 8) {
    error.value = '비밀번호는 8자 이상이어야 해요.'
    return
  }
  if (suPassword.value !== suPassword2.value) {
    error.value = '비밀번호가 일치하지 않아요.'
    return
  }
  if (!agree.value) {
    error.value = '개인정보 처리방침과 이용약관에 동의해 주세요.'
    return
  }
  loading.value = true
  try {
    // 가입 성공 시 로그인과 동일한 토큰이 발급된다(자동 로그인) — api-spec [1b].
    const res = await api.post('/auth/register',
      { name: name.value, email: suEmail.value, password: suPassword.value }, { auth: false })
    auth.setSession({ accessToken: res.accessToken, teacher: res.teacher })
    gotoNext()
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '회원가입 중 문제가 발생했어요.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth fullscreen" :class="{ desk: isDesktop }">
    <!-- 데스크톱 좌측 브랜드 패널 -->
    <section v-if="isDesktop" class="brand-panel">
      <Logo variant="vertical" :height="200" />
      <div class="bp-title">오늘도 우리 함께해요 ☀️</div>
      <div class="bp-desc">짧은 메모만 남기면 AI가 일지를 만들고,<br />기록은 아이별로 차곡차곡 쌓여요.</div>
    </section>

    <!-- 폼 영역 -->
    <section class="form-area">
      <div class="form-inner">
        <!-- 모바일 상단 로고 -->
        <div v-if="!isDesktop" class="m-logo"><Logo variant="vertical" :height="120" /></div>

        <template v-if="isDesktop">
          <div class="jr-h1 head">{{ tab === 'signup' ? '온도에 오신 걸 환영해요' : '다시 오셨네요, 반가워요' }}</div>
          <div class="jr-body head-sub">{{ tab === 'signup' ? '메모 세 줄이면 AI가 하루 일지를 대신 써요.' : '메모와 기록이 그대로 기다리고 있어요.' }}</div>
        </template>

        <!-- 탭 -->
        <div class="tabs" :class="{ big: isDesktop }">
          <button class="tab" :class="{ on: tab === 'login' }" @click="switchTab('login')">
            로그인<span class="ink" />
          </button>
          <button class="tab" :class="{ on: tab === 'signup' }" @click="switchTab('signup')">
            회원가입<span class="ink" />
          </button>
        </div>

        <!-- 로그인 폼 -->
        <form v-if="tab === 'login' && !forgot" class="form" @submit.prevent="submit">
          <div>
            <label class="jr-field-label">이메일</label>
            <input v-model="email" class="jr-input" type="email" placeholder="teacher@ondo.dev" autocomplete="username" />
          </div>
          <div>
            <label class="jr-field-label">비밀번호</label>
            <input v-model="password" class="jr-input" type="password" placeholder="비밀번호를 입력해주세요" autocomplete="current-password" />
            <div class="keep-row">
              <button type="button" class="keep-toggle" role="checkbox" :aria-checked="keepLoggedIn"
                      @click="keepLoggedIn = !keepLoggedIn">
                <span class="keep-box" :class="{ on: keepLoggedIn }">
                  <AppIcon v-if="keepLoggedIn" name="check" :size="12" :stroke="3" />
                </span>
                <span class="keep-lab">로그인 상태 유지</span>
              </button>
              <button type="button" class="find" @click="openForgot">비밀번호 찾기</button>
            </div>
          </div>

          <p v-if="error" class="err">{{ error }}</p>

          <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" type="submit" :disabled="loading">
            <template v-if="!loading">로그인 <AppIcon name="chevR" :size="20" /></template>
            <template v-else>로그인 중…</template>
          </button>
        </form>

        <!-- 비밀번호 찾기 -->
        <form v-else-if="tab === 'login' && forgot" class="form" @submit.prevent="submitForgot">
          <template v-if="!forgotSent">
            <p class="forgot-lead">가입하신 이메일로 재설정 링크를 보내 드릴게요.</p>
            <div>
              <label class="jr-field-label">이메일</label>
              <input v-model="forgotEmail" class="jr-input" type="email" placeholder="teacher@ondo.dev" autocomplete="username" />
            </div>
            <p v-if="error" class="err">{{ error }}</p>
            <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" type="submit" :disabled="loading">
              <template v-if="!loading">재설정 링크 받기 <AppIcon name="chevR" :size="20" /></template>
              <template v-else>보내는 중…</template>
            </button>
          </template>
          <!-- 가입 여부를 알려주지 않는다 — 문구가 갈리면 그 자체로 계정 조회기가 된다. -->
          <div v-else class="jr-banner forgot-done">
            <AppIcon name="check" :size="22" :stroke="2.4" style="color:var(--brand-700);flex:0 0 auto" />
            <span>입력하신 주소로 가입된 계정이 있다면 재설정 링크를 보내 드렸어요.
              메일함을 확인해 주세요. 링크는 30분 동안만 쓸 수 있어요.</span>
          </div>
          <button type="button" class="back-login" @click="closeForgot">로그인으로 돌아가기</button>
        </form>

        <!-- 회원가입 폼 -->
        <form v-else class="form" @submit.prevent="submitSignup">
          <div>
            <label class="jr-field-label">이름</label>
            <input v-model="name" class="jr-input" type="text" placeholder="선생님 성함을 입력해주세요" autocomplete="name" />
          </div>
          <div>
            <label class="jr-field-label">이메일</label>
            <input v-model="suEmail" class="jr-input" type="email" placeholder="teacher@ondo.dev" autocomplete="username" />
          </div>
          <div>
            <label class="jr-field-label">비밀번호</label>
            <input v-model="suPassword" class="jr-input" type="password" placeholder="8자 이상 입력해주세요" autocomplete="new-password" />
          </div>
          <div>
            <label class="jr-field-label">비밀번호 확인</label>
            <input v-model="suPassword2" class="jr-input" type="password" placeholder="비밀번호를 한 번 더 입력해주세요" autocomplete="new-password" />
          </div>

          <!-- 필수 동의 — 링크는 새 탭으로 열어 입력 중인 가입 폼을 잃지 않게 한다. -->
          <button type="button" class="agree" role="checkbox" :aria-checked="agree" @click="agree = !agree">
            <span class="keep-box" :class="{ on: agree }">
              <AppIcon v-if="agree" name="check" :size="12" :stroke="3" />
            </span>
            <span class="agree-lab">
              <RouterLink to="/privacy" target="_blank" @click.stop>개인정보 처리방침</RouterLink>과
              <RouterLink to="/terms" target="_blank" @click.stop>이용약관</RouterLink>에 동의합니다. <span class="req">(필수)</span>
            </span>
          </button>

          <p v-if="error" class="err">{{ error }}</p>

          <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" type="submit" :disabled="loading">
            <template v-if="!loading">회원가입 <AppIcon name="chevR" :size="20" /></template>
            <template v-else>가입 중…</template>
          </button>
        </form>

        <!-- 소셜 로그인 — 로그인/회원가입 공통. 카카오 검증 후 기존과 동일한 세션이 발급된다. -->
        <div class="social-sep"><span>또는</span></div>
        <button type="button" class="kakao-btn" @click="kakaoLogin" :disabled="loading">
          <svg class="kakao-ico" viewBox="0 0 24 24" width="20" height="20" aria-hidden="true">
            <path fill="#000" d="M12 3C6.9 3 2.75 6.2 2.75 10.15c0 2.55 1.72 4.79 4.3 6.05-.19.68-.68 2.47-.78 2.85-.13.48.17.47.37.34.15-.1 2.4-1.63 3.38-2.29.64.09 1.3.14 1.98.14 5.1 0 9.25-3.2 9.25-7.15S17.1 3 12 3Z"/>
          </svg>
          카카오로 시작하기
        </button>

        <!-- 온도가 처음인 분을 위한 소개 페이지 안내 -->
        <p class="intro-link">
          온도가 처음이신가요?
          <button type="button" @click="router.push({ name: 'intro' })">서비스 소개 보기</button>
        </p>

        <!-- 법적 고지 링크 — 로그인·가입 어느 탭에서도 접근 가능 -->
        <nav class="legal-links">
          <RouterLink to="/privacy">개인정보 처리방침</RouterLink>
          <span class="dot">·</span>
          <RouterLink to="/terms">이용약관</RouterLink>
        </nav>
      </div>
    </section>
  </div>
</template>

<style scoped>
.auth { display: flex; flex-direction: column; }
.auth.desk { flex-direction: row; height: 100vh; }

/* 브랜드 패널 (데스크톱) */
.brand-panel {
  flex: 0 0 44%; background: linear-gradient(165deg, #FFF6DC, #FFFBF2);
  border-right: 1px solid var(--hair);
  display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; padding: 40px;
}
.bp-title { font-size: 18px; font-weight: 700; color: #7a5e22; margin-top: 18px; }
.bp-desc { font-size: 14px; color: #8a6c2c; margin-top: 24px; line-height: 1.6; max-width: 300px; }

/* 폼 영역 */
.form-area { flex: 1; display: flex; flex-direction: column; justify-content: center; padding: 24px; }
.auth.desk .form-area { padding: 0 56px; }
.form-inner { width: 100%; max-width: 380px; margin: 0 auto; }
.m-logo { text-align: center; display: flex; justify-content: center; padding: 16px 0 12px; }
.head { margin-bottom: 4px; }
.head-sub { color: var(--text-sub); margin-bottom: 26px; }

/* 탭 */
.tabs { display: flex; gap: 22px; border-bottom: 2px solid var(--hair); margin-bottom: 18px; }
.tabs.big { gap: 28px; margin-bottom: 30px; }
.tab {
  position: relative; border: none; background: transparent; cursor: pointer; font-family: inherit; white-space: nowrap;
  font-size: 17px; font-weight: 600; color: var(--text-faint); padding: 0 2px 12px;
}
.tabs.big .tab { font-size: 19px; padding: 0 2px 14px; }
.tab.on { font-weight: 800; color: var(--text); }
.tab .ink { position: absolute; left: 0; right: 0; bottom: -2px; height: 3px; border-radius: 3px; background: transparent; }
.tab.on .ink { background: var(--brand-500); }

.form { display: flex; flex-direction: column; gap: 16px; }
/* 제출 버튼 라운드를 입력창과 맞춘다(기본 pill은 이 화면에선 과하게 둥글다). */
.form .jr-btn { border-radius: var(--r-input); }
.keep-row { display: flex; align-items: center; gap: 7px; margin-top: 12px; }
.keep-toggle {
  display: flex; align-items: center; gap: 7px; border: none; background: transparent;
  font-family: inherit; padding: 0; cursor: pointer;
}
.keep-box {
  width: 18px; height: 18px; border-radius: 6px; background: var(--surface); border: 1.5px solid var(--hair-strong);
  display: flex; align-items: center; justify-content: center; color: var(--brand-700); flex: 0 0 auto;
  transition: background .12s ease, border-color .12s ease;
}
.keep-box.on { background: var(--brand-100); border-color: var(--brand-500); }
.keep-lab { font-size: 13.5px; color: var(--text-sub); font-weight: 600; }
.find { margin-left: auto; font-size: 13.5px; color: var(--text-sub); font-weight: 600; cursor: pointer;
  border: none; background: transparent; font-family: inherit; padding: 0; text-decoration: underline; text-underline-offset: 3px; }
.forgot-lead { font-size: 14px; color: var(--text-sub); margin: 0; line-height: 1.55; }
.forgot-done { font-size: 13.5px; font-weight: 600; line-height: 1.55; text-align: left; }
.back-login { border: none; background: transparent; font-family: inherit; font-size: 13.5px; font-weight: 700;
  color: var(--text-sub); cursor: pointer; padding: 10px; }
.err { color: var(--warn); font-size: 13.5px; font-weight: 600; margin: -4px 2px 0; }

/* 소셜 로그인 */
.social-sep {
  display: flex; align-items: center; gap: 12px; margin: 22px 0 16px; color: var(--text-faint); font-size: 13px;
}
.social-sep::before, .social-sep::after {
  content: ''; flex: 1; height: 1px; background: var(--hair);
}
.kakao-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px; width: 100%;
  height: 52px; border: none; border-radius: 12px; background: #FEE500; color: #191600;
  font-family: inherit; font-size: 16px; font-weight: 700; cursor: pointer;
  transition: filter .15s ease;
}
.kakao-btn:hover { filter: brightness(0.97); }
.kakao-btn:disabled { opacity: .6; cursor: default; }
.kakao-ico { flex: 0 0 auto; }

/* 필수 동의 체크박스 */
.agree { display: flex; align-items: flex-start; gap: 9px; border: none; background: transparent;
  font-family: inherit; padding: 2px 0; cursor: pointer; text-align: left; }
.agree .keep-box { margin-top: 1px; }
.agree-lab { font-size: 13px; color: var(--text-sub); font-weight: 600; line-height: 1.55; }
.agree-lab a { color: var(--brand-700); font-weight: 700; text-decoration: underline; text-underline-offset: 3px; }
.agree-lab a:hover { color: #d89f28; }
.agree-lab .req { color: var(--warn); font-weight: 700; }

/* 소개 페이지 안내 링크 */
.intro-link { margin: 22px 0 0; text-align: center; font-size: 13.5px; font-weight: 500; color: var(--text-faint); }
.intro-link button {
  border: none; background: transparent; font-family: inherit; font-size: 13.5px; font-weight: 700;
  color: var(--brand-700); cursor: pointer; padding: 0 2px; text-decoration: underline; text-underline-offset: 3px;
}
.intro-link button:hover { color: #d89f28; }

/* 법적 고지 링크(푸터) */
.legal-links { margin: 18px 0 4px; text-align: center; font-size: 12.5px; color: var(--text-faint); }
.legal-links a { color: var(--text-faint); font-weight: 600; text-decoration: none; }
.legal-links a:hover { color: var(--text-sub); text-decoration: underline; text-underline-offset: 3px; }
.legal-links .dot { margin: 0 8px; color: var(--text-faint); }
</style>
