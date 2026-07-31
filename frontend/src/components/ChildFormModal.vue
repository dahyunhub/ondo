<script setup>
import { reactive, ref } from 'vue'
import { api, ApiError } from '../lib/api'
import AppIcon from './AppIcon.vue'
import Avatar from './Avatar.vue'
import ImageCropper from './ImageCropper.vue'
import BirthDatePicker from './BirthDatePicker.vue'
import { session } from '../stores/session'
import { birthYearFor } from '../lib/birthYear'

const props = defineProps({
  mode: { type: String, required: true },      // 'add' | 'edit'
  classroomId: { type: [Number, String], default: null }, // add 시 필요
  child: { type: Object, default: null },       // edit 시 { id, name, birthDate, gender, photoUpdatedAt }
})
const emit = defineEmits(['close', 'saved', 'deleted'])

const form = reactive({
  name: props.child?.name ?? '',
  birthDate: props.child?.birthDate ?? '',
  gender: props.child?.gender ?? 'MALE',
})
const saving = ref(false)
const deleting = ref(false)

// 생년월일 기본 표시 연도 — 반 연령으로 계산한다(오늘 날짜 아님).
// 수정 모드에서는 저장된 값이 우선이라 이 값이 쓰이지 않는다.
const classroomYear = session.classroom?.year ?? null
const defaultBirthYear = birthYearFor(classroomYear, session.classroom?.ageClass ?? null)
const error = ref('')

// 프로필 사진(크롭) — 신규 크롭 blob 은 저장 시 업로드.
const fileInput = ref(null)
const cropFile = ref(null)
const pendingBlob = ref(null)
const pendingPreview = ref('')
function pickFile(e) {
  const f = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (f) cropFile.value = f
}
function onCropped(blob) {
  pendingBlob.value = blob
  if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value)
  pendingPreview.value = URL.createObjectURL(blob)
  cropFile.value = null
}

async function save() {
  error.value = ''
  if (!form.name.trim()) { error.value = '이름을 입력해 주세요.'; return }
  if (!form.birthDate) { error.value = '생년월일을 입력해 주세요.'; return }
  saving.value = true
  try {
    const body = { name: form.name.trim(), birthDate: form.birthDate, gender: form.gender }
    let childId
    if (props.mode === 'add') {
      const created = await api.post(`/classrooms/${props.classroomId}/children`, body)
      childId = created.id
    } else {
      await api.put(`/children/${props.child.id}`, body)
      childId = props.child.id
    }
    if (pendingBlob.value) {
      await api.putBinary(`/children/${childId}/photo`, pendingBlob.value, 'image/jpeg')
    }
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
    await api.del(`/children/${props.child.id}`)
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
      <!-- 보존형 삭제 확인 -->
      <template v-if="deleting">
        <div class="del-ic"><AppIcon name="heart" :size="28" /></div>
        <div class="jr-h2 del-title">{{ form.name }} 아이를 명단에서 숨길까요?</div>
        <div class="jr-banner del-banner">
          <AppIcon name="check" :size="22" :stroke="2.4" style="color:var(--brand-700);flex:0 0 auto" />
          <span>기록은 그대로 보존되며 명단에서만 숨겨져요. 완전히 삭제되지 않아요.</span>
        </div>
        <p v-if="error" class="err mt">{{ error }}</p>
        <div class="btn-row">
          <button class="jr-btn jr-btn--secondary f1" @click="deleting = false" :disabled="saving">취소</button>
          <button class="jr-btn jr-btn--warn f1" @click="confirmDelete" :disabled="saving">
            {{ saving ? '처리 중…' : '명단에서 숨기기' }}
          </button>
        </div>
      </template>

      <!-- 등록/수정 폼 -->
      <template v-else>
        <div class="sheet-top">
          <span class="jr-h2">{{ mode === 'add' ? '새 친구 등록' : '아이 정보 수정' }}</span>
          <button class="close" @click="emit('close')"><AppIcon name="x" :size="18" /></button>
        </div>
        <div class="fields">
          <!-- 프로필 사진 -->
          <div class="photo-row">
            <img v-if="pendingPreview" class="jr-avatar jr-avatar--lg photo-prev" :src="pendingPreview" alt="미리보기" />
            <Avatar v-else :name="form.name"
                    :photo-url="mode === 'edit' && child?.id ? `/children/${child.id}/photo` : ''"
                    :photo-key="mode === 'edit' ? (child?.photoUpdatedAt || '') : ''" size="lg" />
            <button type="button" class="jr-btn jr-btn--secondary jr-btn--sm" @click="fileInput?.click()">
              <AppIcon name="plus" :size="16" :stroke="2.6" /> {{ pendingPreview ? '사진 변경' : '사진 추가' }}
            </button>
            <input ref="fileInput" type="file" accept="image/*" class="file-hidden" @change="pickFile" />
          </div>
          <div>
            <label class="jr-field-label">이름</label>
            <input v-model="form.name" class="jr-input" placeholder="아이 이름을 입력해주세요" />
          </div>
          <div>
            <label class="jr-field-label">생년월일</label>
            <BirthDatePicker v-model="form.birthDate"
                             :default-year="defaultBirthYear" :classroom-year="classroomYear" />
          </div>
          <div>
            <label class="jr-field-label">성별</label>
            <div class="sex">
              <button type="button" class="jr-toggle" :class="{ 'is-on': form.gender === 'MALE' }" @click="form.gender = 'MALE'">
                <AppIcon v-if="form.gender === 'MALE'" name="check" :size="14" :stroke="2.6" /> 남자
              </button>
              <button type="button" class="jr-toggle" :class="{ 'is-on': form.gender === 'FEMALE' }" @click="form.gender = 'FEMALE'">
                <AppIcon v-if="form.gender === 'FEMALE'" name="check" :size="14" :stroke="2.6" /> 여자
              </button>
            </div>
          </div>
        </div>
        <p v-if="error" class="err mt">{{ error }}</p>
        <button v-if="mode === 'edit'" class="hide-link" @click="deleting = true">
          <AppIcon name="x" :size="18" /> 명단에서 숨기기 (기록 보존)
        </button>
        <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg save" @click="save" :disabled="saving">
          <template v-if="!saving"><AppIcon name="check" :size="22" :stroke="2.6" /> 저장하기</template>
          <template v-else>저장 중…</template>
        </button>
      </template>
    </div>

    <ImageCropper v-if="cropFile" :file="cropFile" @cropped="onCropped" @close="cropFile = null" />
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 30; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .overlay { align-items: center; padding: 24px; } }
.sheet { background: var(--surface); width: 100%; max-width: 440px; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
@media (min-width: 520px) { .sheet { border-radius: 24px; padding: 26px 28px; } }
.sheet-top { display: flex; align-items: center; margin-bottom: 16px; }
.photo-row { display: flex; align-items: center; gap: 14px; }
.photo-prev { object-fit: cover; }
.file-hidden { display: none; }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }
.fields { display: flex; flex-direction: column; gap: 14px; }
.sex { display: flex; gap: 8px; }
.sex .jr-toggle { flex: 1; justify-content: center; cursor: pointer; }
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
