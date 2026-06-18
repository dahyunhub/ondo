<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Logo from '../components/Logo.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const { isDesktop } = useViewport()

const tab = ref('login') // login | signup(준비 중)
const email = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function submit() {
  error.value = ''
  if (!email.value || !password.value) {
    error.value = '이메일과 비밀번호를 입력해 주세요.'
    return
  }
  loading.value = true
  try {
    const res = await api.post('/auth/login', { email: email.value, password: password.value }, { auth: false })
    auth.setSession({ accessToken: res.accessToken, teacher: res.teacher })
    router.replace({ name: session.classroom ? 'home' : 'classrooms' })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '로그인 중 문제가 발생했어요.'
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
          <div class="jr-h1 head">다시 오셨네요, 반가워요</div>
          <div class="jr-body head-sub">메모와 기록이 그대로 기다리고 있어요.</div>
        </template>

        <!-- 탭 -->
        <div class="tabs" :class="{ big: isDesktop }">
          <button class="tab" :class="{ on: tab === 'login' }" @click="tab = 'login'">
            로그인<span class="ink" />
          </button>
          <button class="tab" :class="{ on: tab === 'signup' }" @click="tab = 'signup'">
            회원가입<span class="ink" />
          </button>
        </div>

        <!-- 로그인 폼 -->
        <form v-if="tab === 'login'" class="form" @submit.prevent="submit">
          <div>
            <label class="jr-field-label">이메일</label>
            <input v-model="email" class="jr-input" type="email" placeholder="teacher@jaram.dev" autocomplete="username" />
          </div>
          <div>
            <label class="jr-field-label">비밀번호</label>
            <input v-model="password" class="jr-input" type="password" placeholder="비밀번호를 입력해주세요" autocomplete="current-password" />
            <div class="keep-row">
              <span class="keep-box"><AppIcon name="check" :size="12" :stroke="3" /></span>
              <span class="keep-lab">로그인 상태 유지</span>
              <span class="find">비밀번호 찾기</span>
            </div>
          </div>

          <p v-if="error" class="err">{{ error }}</p>

          <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" type="submit" :disabled="loading">
            <template v-if="!loading">로그인 <AppIcon name="chevR" :size="20" /></template>
            <template v-else>로그인 중…</template>
          </button>
        </form>

        <!-- 회원가입(준비 중) -->
        <div v-else class="soon">
          <div class="soon-ic"><AppIcon name="sparkle" :size="28" /></div>
          <div class="soon-t">회원가입은 곧 제공돼요</div>
          <div class="soon-d">지금은 시드 계정(teacher@jaram.dev)으로 로그인해 둘러볼 수 있어요.</div>
          <button class="jr-btn jr-btn--secondary" @click="tab = 'login'">로그인으로 돌아가기</button>
        </div>
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
.keep-row { display: flex; align-items: center; gap: 7px; margin-top: 12px; }
.keep-box {
  width: 18px; height: 18px; border-radius: 6px; background: var(--brand-100); border: 1.5px solid var(--brand-500);
  display: flex; align-items: center; justify-content: center; color: var(--brand-700); flex: 0 0 auto;
}
.keep-lab { font-size: 13.5px; color: var(--text-sub); font-weight: 600; }
.find { margin-left: auto; font-size: 13.5px; color: var(--text-sub); font-weight: 600; cursor: pointer; }
.err { color: var(--warn); font-size: 13.5px; font-weight: 600; margin: -4px 2px 0; }

.soon { text-align: center; padding: 20px 0; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.soon-ic { width: 56px; height: 56px; border-radius: 18px; background: var(--brand-100); color: var(--brand-700); display: flex; align-items: center; justify-content: center; }
.soon-t { font-size: 17px; font-weight: 800; }
.soon-d { font-size: 13.5px; color: var(--text-sub); line-height: 1.6; max-width: 300px; }
.soon .jr-btn { margin-top: 8px; }
</style>
