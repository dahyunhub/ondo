<script setup>
import { reactive, ref, computed } from 'vue'
import { api, ApiError } from '../lib/api'
import AppIcon from './AppIcon.vue'

// memo: 타임라인 항목 { id, content, playActivity, interaction, attitude }
const props = defineProps({
  memo: { type: Object, required: true },
})
const emit = defineEmits(['close', 'saved', 'deleted'])

const FIELDS = [
  { key: 'playActivity', label: '놀이' },
  { key: 'interaction', label: '의사소통·상호작용' },
  { key: 'attitude', label: '수업태도' },
]

const form = reactive({
  content: props.memo.content ?? '',
  playActivity: props.memo.playActivity ?? '',
  interaction: props.memo.interaction ?? '',
  attitude: props.memo.attitude ?? '',
})
const saving = ref(false)
const deleting = ref(false)
const error = ref('')

const allBlank = computed(() =>
  !form.content.trim() && !form.playActivity.trim() && !form.interaction.trim() && !form.attitude.trim())

async function save() {
  error.value = ''
  if (allBlank.value) { error.value = '내용 또는 항목 중 하나 이상을 입력해 주세요.'; return }
  saving.value = true
  try {
    await api.patch(`/memos/${props.memo.id}`, {
      content: form.content.trim() || null,
      playActivity: form.playActivity.trim() || null,
      interaction: form.interaction.trim() || null,
      attitude: form.attitude.trim() || null,
    })
    emit('saved')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '저장 중 문제가 발생했어요.'
  } finally {
    saving.value = false
  }
}

async function confirmDelete() {
  saving.value = true
  error.value = ''
  try {
    await api.del(`/memos/${props.memo.id}`)
    emit('deleted')
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '삭제 중 문제가 발생했어요.'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <!-- 삭제 확인 -->
      <template v-if="deleting">
        <div class="del-ic"><AppIcon name="x" :size="26" /></div>
        <div class="jr-h2 del-title">이 메모를 삭제할까요?</div>
        <div class="jr-banner del-banner">
          <AppIcon name="check" :size="22" :stroke="2.4" style="color:var(--brand-700);flex:0 0 auto" />
          <span>삭제한 메모는 타임라인과 분석에서 제외돼요. 이 동작은 되돌릴 수 없어요.</span>
        </div>
        <p v-if="error" class="err mt">{{ error }}</p>
        <div class="btn-row">
          <button class="jr-btn jr-btn--secondary f1" @click="deleting = false" :disabled="saving">취소</button>
          <button class="jr-btn jr-btn--warn f1" @click="confirmDelete" :disabled="saving">
            {{ saving ? '처리 중…' : '삭제하기' }}
          </button>
        </div>
      </template>

      <!-- 수정 폼 -->
      <template v-else>
        <div class="sheet-top">
          <span class="jr-h2">메모 수정</span>
          <button class="close" @click="emit('close')"><AppIcon name="x" :size="18" /></button>
        </div>
        <div class="fields">
          <div>
            <label class="jr-field-label">자유 메모</label>
            <textarea v-model="form.content" class="jr-textarea" rows="4"
                      placeholder="관찰한 모습을 자유롭게 적어요" style="font-size:16px;line-height:1.6"></textarea>
          </div>
          <div v-for="f in FIELDS" :key="f.key">
            <label class="jr-field-label">{{ f.label }}</label>
            <input v-model="form[f.key]" class="jr-input" :placeholder="`${f.label}에서 관찰한 모습`" />
          </div>
        </div>
        <p v-if="error" class="err mt">{{ error }}</p>
        <button class="hide-link" @click="deleting = true">
          <AppIcon name="x" :size="18" /> 이 메모 삭제
        </button>
        <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg save" @click="save" :disabled="saving">
          <template v-if="!saving"><AppIcon name="check" :size="22" :stroke="2.6" /> 저장하기</template>
          <template v-else>저장 중…</template>
        </button>
      </template>
    </div>
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 30; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .overlay { align-items: center; padding: 24px; } }
.sheet { background: var(--surface); width: 100%; max-width: 440px; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
@media (min-width: 520px) { .sheet { border-radius: 24px; padding: 26px 28px; } }
.sheet-top { display: flex; align-items: center; margin-bottom: 16px; }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }
.fields { display: flex; flex-direction: column; gap: 14px; }
.hide-link { display: flex; align-items: center; justify-content: center; gap: 7px; width: 100%; margin-top: 16px; padding: 12px; border: none; background: transparent; color: var(--warn); cursor: pointer; font-family: inherit; font-size: 14.5px; font-weight: 700; }
.save { margin-top: 12px; }
.err { color: var(--warn); font-weight: 600; font-size: 13.5px; }
.mt { margin-top: 4px; }
.del-ic { width: 56px; height: 56px; border-radius: 50%; margin: 0 auto 16px; background: rgba(240, 140, 125, .14); color: var(--warn); display: flex; align-items: center; justify-content: center; }
.del-title { text-align: center; }
.del-banner { margin-top: 16px; text-align: left; font-size: 13.5px; font-weight: 700; }
.btn-row { display: flex; gap: 10px; margin-top: 22px; }
.f1 { flex: 1; }
</style>
