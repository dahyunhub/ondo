<script setup>
// 새 반 만들기 — 온보딩형 플로우. 반 이름·학년도 + 아이 한 명씩 입력(사진 포함).
// 백엔드: POST /classrooms → POST /classrooms/{id}/children (아이마다) → 사진 있으면 PUT /children/{id}/photo.
import { reactive, ref, onBeforeUnmount } from 'vue'
import { api, ApiError } from '../lib/api'
import AppIcon from './AppIcon.vue'
import Avatar from './Avatar.vue'
import BirthDatePicker from './BirthDatePicker.vue'
import { birthYearFor, AGE_CLASS_OPTIONS } from '../lib/birthYear'
import ImageCropper from './ImageCropper.vue'

const emit = defineEmits(['close', 'created'])

const nowYear = new Date().getFullYear()
const form = reactive({ name: '', year: nowYear, ageClass: null })

// uid: v-for 안정 키 + 크롭 대상 추적용. 행 삭제/재정렬 시 인덱스가 밀려도 사진이 엉뚱한 아이에 붙지 않도록.
let uidSeq = 0
function newChild() { return { uid: ++uidSeq, name: '', birthDate: '', gender: 'MALE', photoBlob: null, photoPreview: '' } }

const children = ref([newChild()])
const saving = ref(false)
const error = ref('')

function addChild() { children.value.push(newChild()) }
function removeChild(i) {
  if (children.value[i].photoPreview) URL.revokeObjectURL(children.value[i].photoPreview)
  children.value.splice(i, 1)
}

// 프로필 사진(크롭) — 대상 아이를 인덱스가 아닌 객체 참조로 추적(ImageCropper 단일 인스턴스 공유).
const cropFile = ref(null)
const cropTarget = ref(null)
function pickFile(child, e) {
  const f = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (f) { cropTarget.value = child; cropFile.value = f }
}
function onCropped(blob) {
  const c = cropTarget.value
  if (c) {
    if (c.photoPreview) URL.revokeObjectURL(c.photoPreview)
    c.photoBlob = blob
    c.photoPreview = URL.createObjectURL(blob)
  }
  cropFile.value = null
  cropTarget.value = null
}

onBeforeUnmount(() => {
  for (const c of children.value) if (c.photoPreview) URL.revokeObjectURL(c.photoPreview)
})

async function submit() {
  error.value = ''
  if (!form.name.trim()) { error.value = '반 이름을 입력해 주세요.'; return }
  if (!form.year || form.year < 2000 || form.year > 2100) { error.value = '학년도를 확인해 주세요.'; return }

  // 입력된 아이만(이름 있는 행). 이름은 있는데 생년월일이 빠진 행은 막는다.
  const filled = children.value.filter((c) => c.name.trim() || c.birthDate)
  for (const c of filled) {
    if (!c.name.trim()) { error.value = '아이 이름을 입력해 주세요.'; return }
    if (!c.birthDate) { error.value = `${c.name.trim()} 아이의 생년월일을 입력해 주세요.`; return }
  }

  saving.value = true
  try {
    const classroom = await api.post('/classrooms',
      { name: form.name.trim(), year: form.year, ageClass: form.ageClass })
    // 아이들 순차 등록(반 생성 후). 일부 실패해도 반은 이미 생성됨.
    let failed = 0
    for (const c of filled) {
      try {
        const created = await api.post(`/classrooms/${classroom.id}/children`,
          { name: c.name.trim(), birthDate: c.birthDate, gender: c.gender })
        // 사진은 아이 생성 성공 후에만, 비치명적으로 업로드(실패해도 아이는 등록됨).
        if (c.photoBlob) {
          try { await api.putBinary(`/children/${created.id}/photo`, c.photoBlob, 'image/jpeg') }
          catch (e) { console.warn(`아이 사진 업로드 실패(childId=${created.id})`, e) } // 비치명적: 아이는 등록됨
        }
      } catch { failed += 1 }
    }
    emit('created', { classroom, total: filled.length, failed })
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '반 생성 중 문제가 발생했어요.'
    saving.value = false
  }
}
</script>

<template>
  <div class="overlay" @click.self="emit('close')">
    <div class="sheet">
      <div class="sheet-top">
        <span class="jr-h2">새 반 만들기</span>
        <button class="close" @click="emit('close')"><AppIcon name="x" :size="18" /></button>
      </div>

      <div class="fields">
        <div class="row2">
          <div class="grow">
            <label class="jr-field-label">반 이름</label>
            <input v-model="form.name" class="jr-input" placeholder="예: 햇살반" />
          </div>
          <div class="yearbox">
            <label class="jr-field-label">학년도</label>
            <input v-model.number="form.year" class="jr-input" type="number" inputmode="numeric" />
          </div>
        </div>

        <div>
          <label class="jr-field-label">만 나이 <span class="opt">선택</span></label>
          <div class="ages">
            <button v-for="a in AGE_CLASS_OPTIONS" :key="a" type="button" class="jr-toggle age"
                    :class="{ 'is-on': form.ageClass === a }" @click="form.ageClass = a">만 {{ a }}세</button>
            <button type="button" class="jr-toggle age" :class="{ 'is-on': form.ageClass === null }"
                    @click="form.ageClass = null">혼합·미정</button>
          </div>
          <!-- 고르면 아래 아이들의 생년월일이 그 연령에 맞는 연도부터 보인다. 다른 연도도 그대로 고를 수 있다. -->
          <p class="age-hint">고르면 아이 생년월일이 <b>{{ birthYearFor(form.year, form.ageClass) }}년</b>부터 보여요. 다른 연도도 선택할 수 있어요.</p>
        </div>

        <div class="kids-head">
          <span class="kids-t">아이 명단</span>
          <span class="kids-note">나중에 추가해도 돼요</span>
        </div>
        <div class="kids">
          <div v-for="(c, i) in children" :key="c.uid" class="kid">
            <div class="kid-line">
              <label class="kid-photo" aria-label="아이 사진 추가">
                <img v-if="c.photoPreview" class="kid-photo-img" :src="c.photoPreview" alt="미리보기" />
                <Avatar v-else :name="c.name" size="sm" />
                <span class="kid-cam"><AppIcon name="plus" :size="11" :stroke="2.8" /></span>
                <input type="file" accept="image/*" class="file-hidden" @change="pickFile(c, $event)" />
              </label>
              <input v-model="c.name" class="jr-input" placeholder="이름" />
              <button class="kid-x" @click="removeChild(i)" aria-label="삭제"><AppIcon name="x" :size="16" /></button>
            </div>
            <!-- 생년월일은 셀렉트 3개라 성별과 한 줄에 두면 375px 에서 연도가 잘린다. -->
            <BirthDatePicker v-model="c.birthDate" class="kid-birth"
                             :default-year="birthYearFor(form.year, form.ageClass)" :classroom-year="form.year" />
            <div class="kid-line">
              <div class="sex grow">
                <button type="button" class="jr-toggle" :class="{ 'is-on': c.gender === 'MALE' }" @click="c.gender = 'MALE'">남</button>
                <button type="button" class="jr-toggle" :class="{ 'is-on': c.gender === 'FEMALE' }" @click="c.gender = 'FEMALE'">여</button>
              </div>
            </div>
          </div>
        </div>
        <button class="add-kid" @click="addChild"><AppIcon name="plus" :size="16" :stroke="2.6" /> 아이 추가</button>
      </div>

      <p v-if="error" class="err">{{ error }}</p>
      <button class="jr-btn jr-btn--primary jr-btn--block jr-btn--lg save" @click="submit" :disabled="saving">
        <template v-if="!saving"><AppIcon name="check" :size="22" :stroke="2.6" /> 반 만들기</template>
        <template v-else>만드는 중…</template>
      </button>
    </div>

    <ImageCropper v-if="cropFile" :file="cropFile" @cropped="onCropped" @close="cropFile = null; cropTarget = null" />
  </div>
</template>

<style scoped>
.overlay { position: fixed; inset: 0; z-index: 40; background: rgba(40, 30, 20, .36); display: flex; align-items: flex-end; justify-content: center; }
@media (min-width: 520px) { .overlay { align-items: center; padding: 24px; } }
.sheet { background: var(--surface); width: 100%; max-width: 460px; max-height: 90vh; overflow-y: auto; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
@media (min-width: 520px) { .sheet { border-radius: 24px; padding: 26px 28px; } }
.sheet-top { display: flex; align-items: center; margin-bottom: 16px; }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; }
.fields { display: flex; flex-direction: column; gap: 14px; }
.row2 { display: flex; gap: 12px; }
.grow { flex: 1; }
.yearbox { width: 110px; flex: 0 0 auto; }
.opt { font-weight: 600; color: var(--text-faint); font-size: 12px; }
.ages { display: flex; flex-wrap: wrap; gap: 6px; }
.age { cursor: pointer; font-size: 13px; padding: 7px 12px; }
.age-hint { font-size: 12px; color: var(--text-sub); margin: 8px 0 0; line-height: 1.5; }
.kid-birth { margin-top: 8px; }
.kids-head { display: flex; align-items: baseline; gap: 8px; margin-top: 4px; }
.kids-t { font-size: 13px; font-weight: 800; color: var(--text-sub); }
.kids-note { font-size: 11.5px; color: var(--text-faint); }
.kids { display: flex; flex-direction: column; gap: 12px; }
.kid { background: var(--surface-soft); border-radius: 14px; padding: 12px; display: flex; flex-direction: column; gap: 8px; }
.kid-line { display: flex; gap: 8px; align-items: center; }
.kid-line .jr-input { flex: 1; min-width: 0; }
.kid-x { border: none; background: transparent; color: var(--text-faint); cursor: pointer; flex: 0 0 auto; padding: 6px; }
.file-hidden { display: none; }
.kid-photo { position: relative; flex: 0 0 auto; border: none; background: transparent; padding: 0; cursor: pointer; line-height: 0; }
.kid-photo-img { width: 40px; height: 40px; border-radius: 50%; object-fit: cover; display: block; }
/* 이니셜 아바타(span)는 base .jr-avatar 의 inline-flex 중앙정렬을 유지해야 이름이 원 안에 가운데로 온다.
   여기서 display 를 건드리면 글자가 위로 붙어 잘린다 — 크기·글꼴크기만 덮어쓴다. */
.kid-photo :deep(.jr-avatar) { width: 40px; height: 40px; font-size: 15px; }
.kid-cam { position: absolute; right: -3px; bottom: -3px; width: 17px; height: 17px; border-radius: 50%; background: var(--brand-500); color: #fff; display: flex; align-items: center; justify-content: center; box-shadow: 0 0 0 2px var(--surface-soft); }
.sex { display: flex; gap: 6px; flex: 0 0 auto; }
.sex .jr-toggle { cursor: pointer; padding: 8px 12px; }
.add-kid { display: flex; align-items: center; justify-content: center; gap: 6px; width: 100%; padding: 11px; border: 1.5px dashed var(--hair-strong); border-radius: 12px; background: transparent; color: var(--text-sub); cursor: pointer; font-family: inherit; font-size: 13.5px; font-weight: 700; }
.err { color: var(--warn); font-weight: 600; font-size: 13.5px; margin-top: 12px; }
.save { margin-top: 16px; }
</style>
