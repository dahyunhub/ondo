<script setup>
// 인앱 피드백 — 어느 화면에서든 의견을 보낼 수 있는 플로팅 버튼 + 시트.
// 백엔드: POST /api/v1/feedback (데모 계정도 허용). 저장은 durable(feedback 테이블) + 관리자 메일 알림.
import { ref, reactive, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { notice } from '../stores/notice'
import AppIcon from './AppIcon.vue'

const route = useRoute()

const open = ref(false)
const sending = ref(false)
const error = ref('')
const textarea = ref(null)
const form = reactive({ category: '제안', message: '' })

const CATEGORIES = ['제안', '버그', '기타']

async function show() {
  error.value = ''
  open.value = true
  await nextTick()
  textarea.value?.focus()
}

function close() {
  open.value = false
}

async function submit() {
  error.value = ''
  const message = form.message.trim()
  if (!message) { error.value = '의견을 입력해 주세요.'; return }

  sending.value = true
  try {
    await api.sendFeedback({
      message,
      category: form.category,
      page: route.name ? String(route.name) : route.path,
    })
    open.value = false
    form.message = ''
    form.category = '제안'
    notice.show('소중한 의견 고마워요! 잘 전달됐어요 🙌')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '전송 중 문제가 생겼어요. 잠시 후 다시 시도해 주세요.'
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <!-- 플로팅 트리거 -->
  <button class="fb-fab" type="button" aria-label="의견 보내기" @click="show">
    <AppIcon name="chat" :size="22" :stroke="2.2" />
    <span class="fb-fab-label">의견</span>
  </button>

  <!-- 입력 시트 -->
  <Transition name="fb">
    <div v-if="open" class="fb-overlay" @click.self="close">
      <div class="fb-sheet jr-card">
        <div class="fb-top">
          <span class="jr-h2">의견 보내기</span>
          <button class="fb-close" type="button" aria-label="닫기" @click="close">
            <AppIcon name="x" :size="18" />
          </button>
        </div>
        <p class="fb-sub">불편했던 점, 바라는 점 무엇이든 좋아요. 짧아도 큰 도움이 돼요.</p>

        <div class="fb-cats">
          <button v-for="c in CATEGORIES" :key="c" type="button" class="jr-toggle"
                  :class="{ 'is-on': form.category === c }" @click="form.category = c">{{ c }}</button>
        </div>

        <textarea ref="textarea" v-model="form.message" class="jr-textarea fb-text"
                  rows="5" maxlength="2000" placeholder="예: 일지 저장 후 화면이 잠깐 멈춰요 / 반별 통계도 보고 싶어요"></textarea>

        <p v-if="error" class="fb-err">{{ error }}</p>

        <button class="jr-btn jr-btn--primary jr-btn--block" type="button" :disabled="sending" @click="submit">
          <template v-if="!sending"><AppIcon name="chat" :size="20" :stroke="2.4" /> 보내기</template>
          <template v-else>보내는 중…</template>
        </button>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.fb-fab {
  position: fixed; right: 16px; bottom: 92px; z-index: 55;
  display: inline-flex; align-items: center; gap: 7px;
  padding: 12px 16px; border: none; cursor: pointer; font-family: inherit;
  font-size: 14px; font-weight: 700; color: var(--text);
  background: var(--brand-500); border-radius: var(--r-pill);
  box-shadow: 0 6px 18px rgba(245, 185, 64, .42);
  transition: transform .12s cubic-bezier(.2,.8,.2,1.2), box-shadow .15s, background .15s;
}
.fb-fab:hover { background: var(--brand-700); transform: translateY(-1px); }
.fb-fab:active { transform: translateY(0) scale(.97); }
/* 데스크톱엔 하단탭이 없으니 더 아래로. */
@media (min-width: 900px) { .fb-fab { bottom: 24px; right: 24px; } }

.fb-overlay {
  position: fixed; inset: 0; z-index: 80; background: rgba(40, 30, 20, .36);
  display: flex; align-items: flex-end; justify-content: center;
}
@media (min-width: 520px) { .fb-overlay { align-items: center; padding: 24px; } }

.fb-sheet {
  width: 100%; max-width: 460px; max-height: 90vh; overflow-y: auto;
  border-radius: 26px 26px 0 0; padding: 22px 22px 26px; box-shadow: var(--shadow-lg);
}
@media (min-width: 520px) { .fb-sheet { border-radius: 24px; padding: 24px 26px; } }

.fb-top { display: flex; align-items: center; margin-bottom: 6px; }
.fb-close {
  margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%;
  width: 34px; height: 34px; display: flex; align-items: center; justify-content: center;
  color: var(--text-sub); cursor: pointer;
}
.fb-sub { font-size: 13.5px; color: var(--text-sub); line-height: 1.5; margin: 0 0 14px; }
.fb-cats { display: flex; gap: 7px; margin-bottom: 12px; }
.fb-cats .jr-toggle { cursor: pointer; }
.fb-text { min-height: 120px; }
.fb-err { color: var(--warn); font-weight: 600; font-size: 13.5px; margin: 12px 0 0; }
.fb-sheet .jr-btn { margin-top: 16px; }

/* 시트 트랜지션 */
.fb-enter-active, .fb-leave-active { transition: opacity .2s; }
.fb-enter-active .fb-sheet, .fb-leave-active .fb-sheet { transition: transform .24s cubic-bezier(.2,.8,.2,1.05); }
.fb-enter-from, .fb-leave-to { opacity: 0; }
.fb-enter-from .fb-sheet, .fb-leave-to .fb-sheet { transform: translateY(16px); }
</style>
