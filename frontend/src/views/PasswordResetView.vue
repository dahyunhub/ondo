<script setup>
// 비밀번호 재설정 확정 화면(spec-password-reset).
// 메일 링크(`/reset-password?token=...`)로 들어온다. 성공해도 자동 로그인하지 않고
// 로그인 화면으로 보낸다 — 메일 접근권만으로 세션이 열리면 안 되고, 새 비밀번호를 한 번
// 직접 입력해봐야 교사도 기억에 남는다.
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import Logo from '../components/Logo.vue'
import AppIcon from '../components/AppIcon.vue'

const route = useRoute()
const router = useRouter()

const token = ref('')
const password = ref('')
const password2 = ref('')
const loading = ref(false)
const error = ref('')
const done = ref(false)

const missingToken = computed(() => !token.value)

onMounted(() => {
  const raw = route.query.token
  token.value = typeof raw === 'string' ? raw : ''
})

async function submit() {
  error.value = ''
  if (password.value.length < 8) {
    error.value = '비밀번호는 8자 이상이어야 해요.'
    return
  }
  if (password.value !== password2.value) {
    error.value = '비밀번호가 일치하지 않아요.'
    return
  }
  loading.value = true
  try {
    await api.post('/auth/password-reset/confirm',
      { token: token.value, newPassword: password.value }, { auth: false })
    done.value = true
  } catch (e) {
    // 만료·재사용·위조를 서버가 구분하지 않으므로 화면도 서버 문구를 그대로 보여준다.
    error.value = e instanceof ApiError ? e.message : '재설정 중 문제가 발생했어요.'
  } finally {
    loading.value = false
  }
}

function gotoLogin() { router.replace({ name: 'login' }) }
</script>

<template>
  <div class="wrap fullscreen">
    <div class="card">
      <Logo :size="72" />
      <div class="jr-h2 title">비밀번호 재설정</div>

      <!-- 링크에 토큰이 없다 — 주소를 직접 치고 들어왔거나 메일 앱이 링크를 잘랐을 때 -->
      <template v-if="missingToken">
        <p class="lead">재설정 링크가 올바르지 않아요. 메일에 있는 링크를 다시 눌러 주세요.</p>
        <button class="jr-btn jr-btn--secondary jr-btn--block" @click="gotoLogin">로그인으로 가기</button>
      </template>

      <!-- 완료 -->
      <template v-else-if="done">
        <div class="jr-banner done">
          <AppIcon name="check" :size="22" :stroke="2.4" style="color:var(--brand-700);flex:0 0 auto" />
          <span>비밀번호를 바꿨어요. 새 비밀번호로 로그인해 주세요.</span>
        </div>
        <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" @click="gotoLogin">
          로그인하기 <AppIcon name="chevR" :size="20" />
        </button>
      </template>

      <!-- 입력 -->
      <form v-else class="form" @submit.prevent="submit">
        <p class="lead">새로 사용할 비밀번호를 입력해 주세요.</p>
        <div>
          <label class="jr-field-label">새 비밀번호</label>
          <input v-model="password" class="jr-input" type="password"
                 placeholder="8자 이상" autocomplete="new-password" />
        </div>
        <div>
          <label class="jr-field-label">새 비밀번호 확인</label>
          <input v-model="password2" class="jr-input" type="password"
                 placeholder="다시 한 번 입력해주세요" autocomplete="new-password" />
        </div>
        <p v-if="error" class="err">{{ error }}</p>
        <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg" type="submit" :disabled="loading">
          <template v-if="!loading">비밀번호 바꾸기 <AppIcon name="chevR" :size="20" /></template>
          <template v-else>바꾸는 중…</template>
        </button>
        <button type="button" class="back-login" @click="gotoLogin">로그인으로 돌아가기</button>
      </form>
    </div>
  </div>
</template>

<style scoped>
/* 배경·최소 높이는 전역 .fullscreen 유틸이 담당한다(base.css). */
.wrap { display: flex; align-items: center; justify-content: center; padding: 24px; }
.card {
  width: 100%; max-width: 400px; background: var(--surface); border-radius: var(--r-card);
  box-shadow: var(--shadow); padding: 32px 26px 28px; display: flex; flex-direction: column;
  align-items: center; text-align: center; gap: 14px;
}
.title { margin-top: 4px; }
.lead { font-size: 14px; color: var(--text-sub); line-height: 1.55; margin: 0; }
.form { width: 100%; display: flex; flex-direction: column; gap: 14px; text-align: left; }
.done { font-size: 13.5px; font-weight: 600; line-height: 1.55; text-align: left; }
.err { color: var(--warn); font-weight: 600; font-size: 13.5px; margin: 0; }
.back-login {
  border: none; background: transparent; font-family: inherit; font-size: 13.5px; font-weight: 700;
  color: var(--text-sub); cursor: pointer; padding: 6px;
}
</style>
