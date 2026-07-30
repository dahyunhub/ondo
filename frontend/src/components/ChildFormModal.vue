<script setup>
import { reactive, ref, computed } from 'vue'
import { api, ApiError } from '../lib/api'
import AppIcon from './AppIcon.vue'
import Avatar from './Avatar.vue'
import ImageCropper from './ImageCropper.vue'

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
const error = ref('')

// 프로필 사진(크롭) — 신규 크롭 blob 은 저장 시 업로드, 삭제도 저장 시 반영한다.
// 즉시 반영하지 않는 이유: 이름·생년월일과 함께 '저장하기'로 확정되는 폼이라,
// 사진만 먼저 사라지면 취소해도 되돌릴 수 없어 폼의 약속이 깨진다.
const fileInput = ref(null)
const cropFile = ref(null)
const pendingBlob = ref(null)
const pendingPreview = ref('')
const photoRemoved = ref(false)

/** 저장된 사진이 있나(수정 모드에서만 의미 있음). */
const hasSavedPhoto = computed(() => props.mode === 'edit' && !!props.child?.photoUpdatedAt)
/** 아바타에 저장된 사진을 띄울지 — 삭제 예약 중이면 이니셜로 미리 보여준다. */
const showSavedPhoto = computed(() => hasSavedPhoto.value && !photoRemoved.value)
/** 지울 사진이 있나(새로 고른 것이든 이미 저장된 것이든). */
const canRemovePhoto = computed(() => !!pendingPreview.value || showSavedPhoto.value)

function pickFile(e) {
  const f = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (f) cropFile.value = f
}
function onCropped(blob) {
  pendingBlob.value = blob
  if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value)
  pendingPreview.value = URL.createObjectURL(blob)
  photoRemoved.value = false // 지웠다가 다시 고르면 삭제 예약 취소
  cropFile.value = null
}

/** 새로 고른 사진이면 그것만 취소하고, 저장된 사진이면 삭제를 예약한다. */
function removePhoto() {
  if (pendingPreview.value) {
    URL.revokeObjectURL(pendingPreview.value)
    pendingPreview.value = ''
    pendingBlob.value = null
    return
  }
  photoRemoved.value = true
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
    } else if (photoRemoved.value && props.mode === 'edit') {
      await api.del(`/children/${childId}/photo`)
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
                    :photo-url="showSavedPhoto && child?.id ? `/children/${child.id}/photo` : ''"
                    :photo-key="showSavedPhoto ? (child?.photoUpdatedAt || '') : ''" size="lg" />
            <div class="photo-actions">
              <button type="button" class="jr-btn jr-btn--secondary jr-btn--sm" @click="fileInput?.click()">
                <AppIcon name="plus" :size="16" :stroke="2.6" />
                {{ pendingPreview || showSavedPhoto ? '사진 변경' : '사진 추가' }}
              </button>
              <!-- 사진 삭제는 되돌리기 쉬운 동작(다시 올리면 됨)이라 확인 단계를 두지 않는다.
                   실제 반영은 '저장하기' 시점 — 취소하면 사진도 그대로 남는다. -->
              <button v-if="canRemovePhoto" type="button" class="photo-del" @click="removePhoto">
                사진 삭제
              </button>
              <span v-else-if="photoRemoved" class="photo-note">저장하면 사진이 지워져요</span>
            </div>
            <input ref="fileInput" type="file" accept="image/*" class="file-hidden" @change="pickFile" />
          </div>
          <div>
            <label class="jr-field-label">이름</label>
            <input v-model="form.name" class="jr-input" placeholder="아이 이름을 입력해주세요" />
          </div>
          <div>
            <label class="jr-field-label">생년월일</label>
            <input v-model="form.birthDate" class="jr-input" type="date" />
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
.photo-actions { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; min-width: 0; }
/* 사진 삭제는 파괴적 동작이 아니라(다시 올리면 됨) 조용한 보조 액션으로 둔다. */
.photo-del {
  border: none; background: transparent; padding: 2px 4px; font-family: inherit;
  font-size: 12.5px; font-weight: 700; color: var(--text-faint); cursor: pointer;
  text-decoration: underline; text-underline-offset: 3px;
}
.photo-del:hover { color: var(--warn); }
.photo-note { font-size: 12px; font-weight: 700; color: var(--text-faint); }
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
