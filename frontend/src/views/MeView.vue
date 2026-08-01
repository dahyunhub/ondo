<script setup>
// 마이 — 프로필(이름·비밀번호 수정) · 반 전환 · 설정 · 도움말(FAQ). (알림/내보내기는 추후)
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api, ApiError } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import ImageCropper from '../components/ImageCropper.vue'

const router = useRouter()
const { isDesktop } = useViewport()

const teacher = computed(() => auth.teacher || {})
const teacherName = computed(() => teacher.value.name || '선생님')
const teacherEmail = computed(() => teacher.value.email || '')

const classrooms = ref([])
const switchOpen = ref(false)
const toast = ref('')

const nowYear = new Date().getFullYear()
function tagOf(year) {
  if (year === nowYear) return '올해'
  if (year === nowYear - 1) return '작년'
  return `${year}학년도`
}
const isNow = (c) => tagOf(c.year) === '올해'
const current = computed(() => session.classroom)
const isPast = computed(() => current.value && !isNow(current.value))

async function openSwitch() {
  switchOpen.value = true
  if (!classrooms.value.length) {
    try { classrooms.value = await api.get('/classrooms') } catch { /* 무시 */ }
  }
}
function pickClass(c) {
  session.select(c)
  switchOpen.value = false
  // 현재(올해) 반으로 전환하면 홈으로, 지난 반이면 그대로 보기
  if (isNow(c)) router.replace({ name: 'home' })
}
function notReady(label) { toast.value = `${label}은 곧 제공돼요`; setTimeout(() => (toast.value = ''), 1600) }

function showToast(msg) { toast.value = msg; setTimeout(() => (toast.value = ''), 1600) }

// 프로필 사진 — '프로필 수정' 모달 안에서 편집한다(카드의 즉석 + 버튼 제거).
// 변경·삭제 모두 '저장하기' 시점에 이름과 함께 반영 — 취소하면 사진도 그대로다.
const fileInput = ref(null)
const cropFile = ref(null)
const pendingBlob = ref(null)
const pendingPreview = ref('')
const photoRemoved = ref(false)
const hasSavedPhoto = computed(() => !!teacher.value.photoUpdatedAt)
const showSavedPhoto = computed(() => hasSavedPhoto.value && !photoRemoved.value)
const canRemovePhoto = computed(() => !!pendingPreview.value || showSavedPhoto.value)
function pickPhoto(e) {
  const f = e.target.files?.[0]
  e.target.value = ''
  if (f) cropFile.value = f
}
function onCropped(blob) {
  pendingBlob.value = blob
  if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value)
  pendingPreview.value = URL.createObjectURL(blob)
  photoRemoved.value = false
  cropFile.value = null
}
function removePhoto() {
  if (pendingPreview.value) {
    URL.revokeObjectURL(pendingPreview.value)
    pendingPreview.value = ''
    pendingBlob.value = null
    return
  }
  photoRemoved.value = true
}
function resetPhotoStaging() {
  if (pendingPreview.value) URL.revokeObjectURL(pendingPreview.value)
  pendingPreview.value = ''
  pendingBlob.value = null
  photoRemoved.value = false
}

// 이름 수정 시트 — 성공 시 auth.updateTeacher 로 아바타·사이드바 즉시 반영.
const editOpen = ref(false)
const editName = ref('')
const editErr = ref('')
const editBusy = ref(false)
function openEdit() {
  editName.value = teacher.value.name || ''
  editErr.value = ''
  resetPhotoStaging()
  editOpen.value = true
}
// 요청 진행 중엔 닫기 무시 — "취소했는데 변경됨" 방지
function closeEdit() { if (!editBusy.value) { resetPhotoStaging(); editOpen.value = false } }
async function saveName() {
  if (editBusy.value) return
  const name = editName.value.trim()
  if (!name) { editErr.value = '이름을 입력해 주세요.'; return }
  editBusy.value = true
  try {
    const res = await api.patch('/teachers/me', { name })
    const patch = { name: res.name }
    if (pendingBlob.value) {
      const up = await api.putBinary('/teachers/me/photo', pendingBlob.value, 'image/jpeg')
      patch.photoUpdatedAt = up.photoUpdatedAt
    } else if (photoRemoved.value) {
      await api.del('/teachers/me/photo')
      patch.photoUpdatedAt = null
    }
    auth.updateTeacher(patch)
    resetPhotoStaging()
    editOpen.value = false
    showToast('프로필을 저장했어요')
  } catch (e) {
    editErr.value = e instanceof ApiError ? e.message : '저장 중 문제가 발생했어요.'
  } finally { editBusy.value = false }
}

// 비밀번호 변경 시트 — 현재 비밀번호 불일치(401)는 인라인 에러, 시트 유지.
const pwOpen = ref(false)
const pw = ref({ current: '', next: '', confirm: '' })
const pwErr = ref('')
const pwBusy = ref(false)
function openPw() {
  pw.value = { current: '', next: '', confirm: '' }
  pwErr.value = ''
  pwOpen.value = true
}
function closePw() { if (!pwBusy.value) pwOpen.value = false }
async function savePassword() {
  if (pwBusy.value) return
  pwErr.value = ''
  if (!pw.value.current) { pwErr.value = '현재 비밀번호를 입력해 주세요.'; return }
  if (pw.value.next.length < 8) { pwErr.value = '새 비밀번호는 8자 이상이어야 해요.'; return }
  // 서버 @MaxBytes(72)(BCrypt 한계)를 사전 검증 — 서버 400 의 범용 메시지보다 구체적으로 안내
  if (new TextEncoder().encode(pw.value.next).length > 72) { pwErr.value = '새 비밀번호가 너무 길어요. 조금 줄여 주세요.'; return }
  if (pw.value.next !== pw.value.confirm) { pwErr.value = '새 비밀번호가 서로 달라요.'; return }
  pwBusy.value = true
  try {
    await api.post('/teachers/me/password', { currentPassword: pw.value.current, newPassword: pw.value.next })
    pwOpen.value = false
    pw.value = { current: '', next: '', confirm: '' }
    showToast('비밀번호를 변경했어요')
  } catch (e) {
    pwErr.value = e instanceof ApiError ? e.message : '변경 중 문제가 발생했어요.'
  } finally { pwBusy.value = false }
}

function logout() {
  auth.logout()
  session.clear()
  router.replace({ name: 'login' })
}

onMounted(() => {})
</script>

<template>
  <div :class="isDesktop ? 'dt-page me-dt' : 'me-m'">
    <template v-if="isDesktop"><div class="jr-display" style="margin-bottom:24px">마이</div></template>
    <header v-else class="m-head screen"><span class="jr-h1">마이</span></header>

    <div :class="isDesktop ? 'me-col' : 'screen body'">
      <!-- 프로필 -->
      <div class="jr-card profile">
        <Avatar :name="teacherName" size="lg"
                photo-url="/teachers/me/photo" :photo-key="teacher.photoUpdatedAt || ''" />
        <div class="p-info">
          <div class="p-name" :class="{ big: isDesktop }">{{ teacherName }} 선생님</div>
          <div class="p-email">{{ teacherEmail || '—' }}</div>
        </div>
        <button class="jr-btn jr-btn--secondary jr-btn--sm" @click="openEdit">
          <AppIcon name="pencil" :size="15" :stroke="2.4" /> {{ isDesktop ? '프로필 수정' : '수정' }}
        </button>
      </div>

      <!-- 지난 반 보는 중 배너 -->
      <div v-if="isPast" class="jr-banner viewing">
        <AppIcon name="clock" :size="20" style="color:var(--brand-700);flex:0 0 auto" />
        <span>{{ current.name }} {{ current.year }}학년도 기록을 보는 중이에요. 지난 반 아이들 기록을 확인할 수 있어요.</span>
      </div>

      <!-- 설정 -->
      <div v-if="isDesktop" class="set-label">설정</div>
      <div class="jr-card settings">
        <button class="row" @click="openSwitch">
          <AppIcon name="swap" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">반 전환하기</div><div class="r-sub">현재 {{ current?.name }} · {{ current?.year }}학년도</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="openPw">
          <AppIcon name="lock" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">비밀번호 변경</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="notReady('알림 설정')">
          <AppIcon name="bell" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">알림 설정</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="notReady('기록 내보내기')">
          <AppIcon name="download" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">기록 내보내기</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row" @click="router.push({ name: 'help' })">
          <AppIcon name="help" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">도움말</div><div class="r-sub">자주 묻는 질문</div></div>
          <AppIcon name="chevR" :size="18" class="r-chev" />
        </button>
        <button class="row danger" @click="logout">
          <AppIcon name="logout" :size="21" class="r-ic" />
          <div class="r-tx"><div class="r-label">로그아웃</div></div>
        </button>
      </div>
    </div>

    <!-- 반 전환 모달 -->
    <div v-if="switchOpen" class="overlay" :class="{ dt: isDesktop }" @click.self="switchOpen = false">
      <div class="sheet" :class="{ dt: isDesktop }">
        <div class="sheet-top">
          <div>
            <div class="s-title">반 전환하기</div>
            <div class="s-sub">지금까지 맡았던 반 중에서 선택하세요</div>
          </div>
          <button class="close" @click="switchOpen = false"><AppIcon name="x" :size="18" /></button>
        </div>
        <p v-if="!classrooms.length" class="muted" style="padding:14px 0">불러오는 중…</p>
        <div v-else class="switch-list">
          <button
            v-for="c in classrooms" :key="c.id" class="sw-card" :class="{ on: current && c.id === current.id }"
            @click="pickClass(c)"
          >
            <span class="sw-ic" :class="{ on: current && c.id === current.id }"><AppIcon :name="isNow(c) ? 'sun' : 'leaf'" :size="22" /></span>
            <span class="sw-info">
              <span class="sw-row"><span class="sw-name">{{ c.name }}</span><span class="sw-tag" :class="{ now: isNow(c) }">{{ tagOf(c.year) }}</span></span>
              <span class="sw-sub">{{ c.year }}학년도 · 원아 {{ c.childCount }}명</span>
            </span>
            <AppIcon v-if="current && c.id === current.id" name="check" :size="20" :stroke="2.6" style="color:var(--brand-700);flex:0 0 auto" />
            <span v-else class="sw-view">보기</span>
          </button>
        </div>
      </div>
    </div>

    <!-- 이름(프로필) 수정 시트 -->
    <div v-if="editOpen" class="overlay" :class="{ dt: isDesktop }" @click.self="closeEdit">
      <div class="sheet" :class="{ dt: isDesktop }">
        <div class="sheet-top">
          <div>
            <div class="s-title">프로필 수정</div>
            <div class="s-sub">이메일은 로그인 아이디라 바꿀 수 없어요</div>
          </div>
          <button class="close" @click="closeEdit"><AppIcon name="x" :size="18" /></button>
        </div>
        <div class="form">
          <!-- 프로필 사진 — 변경·삭제 모두 저장 시점에 반영(취소하면 그대로) -->
          <div class="photo-row">
            <img v-if="pendingPreview" class="jr-avatar jr-avatar--lg photo-prev" :src="pendingPreview" alt="미리보기" />
            <Avatar v-else :name="teacherName" size="lg"
                    :photo-url="showSavedPhoto ? '/teachers/me/photo' : ''"
                    :photo-key="showSavedPhoto ? (teacher.photoUpdatedAt || '') : ''" />
            <div class="photo-actions">
              <button type="button" class="jr-btn jr-btn--secondary jr-btn--sm" @click="fileInput?.click()">
                <AppIcon name="plus" :size="16" :stroke="2.6" />
                {{ pendingPreview || showSavedPhoto ? '사진 변경' : '사진 추가' }}
              </button>
              <button v-if="canRemovePhoto" type="button" class="photo-del" @click="removePhoto">사진 삭제</button>
              <span v-else-if="photoRemoved" class="photo-note">저장하면 사진이 지워져요</span>
            </div>
            <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="pickPhoto" />
          </div>
          <div>
            <label class="jr-field-label">이름</label>
            <input v-model="editName" class="jr-input" maxlength="100" placeholder="이름을 입력해 주세요" @keyup.enter="saveName" />
          </div>
          <div>
            <label class="jr-field-label">이메일</label>
            <input :value="teacherEmail" class="jr-input" disabled />
          </div>
          <p v-if="editErr" class="form-err">{{ editErr }}</p>
          <button class="jr-btn jr-btn--primary" :disabled="editBusy" @click="saveName">
            {{ editBusy ? '저장 중…' : '저장하기' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 비밀번호 변경 시트 -->
    <div v-if="pwOpen" class="overlay" :class="{ dt: isDesktop }" @click.self="closePw">
      <div class="sheet" :class="{ dt: isDesktop }">
        <div class="sheet-top">
          <div>
            <div class="s-title">비밀번호 변경</div>
            <div class="s-sub">새 비밀번호는 8자 이상이어야 해요</div>
          </div>
          <button class="close" @click="closePw"><AppIcon name="x" :size="18" /></button>
        </div>
        <div class="form">
          <div>
            <label class="jr-field-label">현재 비밀번호</label>
            <input v-model="pw.current" class="jr-input" type="password" autocomplete="current-password" />
          </div>
          <div>
            <label class="jr-field-label">새 비밀번호</label>
            <input v-model="pw.next" class="jr-input" type="password" autocomplete="new-password" />
          </div>
          <div>
            <label class="jr-field-label">새 비밀번호 확인</label>
            <input v-model="pw.confirm" class="jr-input" type="password" autocomplete="new-password" @keyup.enter="savePassword" />
          </div>
          <p v-if="pwErr" class="form-err">{{ pwErr }}</p>
          <button class="jr-btn jr-btn--primary" :disabled="pwBusy" @click="savePassword">
            {{ pwBusy ? '변경 중…' : '변경하기' }}
          </button>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="toast" class="toast-wrap" :class="{ dt: isDesktop }">
        <div class="jr-toast">{{ toast }}</div>
      </div>
    </Transition>

    <ImageCropper v-if="cropFile" :file="cropFile" @cropped="onCropped" @close="cropFile = null" />
  </div>
</template>

<style scoped>
.photo-row { display: flex; align-items: center; gap: 14px; }
.photo-prev { object-fit: cover; }
.photo-actions { display: flex; flex-direction: column; align-items: flex-start; gap: 6px; min-width: 0; }
.photo-del { border: none; background: transparent; padding: 2px 4px; font-family: inherit; font-size: 12.5px; font-weight: 700; color: var(--text-faint); cursor: pointer; text-decoration: underline; text-underline-offset: 3px; }
.photo-del:hover { color: var(--warn); }
.photo-note { font-size: 12px; font-weight: 700; color: var(--text-faint); }
.me-m { display: flex; flex-direction: column; }
/* 데스크톱: 제목+내용을 콘텐츠 실폭(560px) 한 컬럼으로 묶어 통째로 가운데 정렬 */
.me-dt { max-width: 560px; }
.m-head { padding-top: 6px; padding-bottom: 12px; }
.body { padding-bottom: 28px; }
.me-col { max-width: 560px; display: flex; flex-direction: column; gap: 20px; }

.profile { display: flex; align-items: center; gap: 14px; margin-bottom: 20px; }
.me-col .profile { margin-bottom: 0; }
.p-info { min-width: 0; flex: 1; }
.p-name { font-size: 19px; font-weight: 700; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.p-name.big { font-size: 22px; }
.p-email { font-size: 13.5px; color: var(--text-sub); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; margin-top: 2px; }

.viewing { margin-bottom: 18px; font-size: 13px; font-weight: 700; line-height: 1.45; }
.me-col .viewing { margin-bottom: 0; }

.set-label { font-size: 14px; font-weight: 800; color: var(--text-sub); }
.settings { padding: 4px 16px; }
.me-col .settings { padding: 4px 18px; }
.row {
  display: flex; align-items: center; gap: 13px; padding: 14px 4px; cursor: pointer; width: 100%;
  border: none; background: transparent; font-family: inherit; text-align: left; border-bottom: 1px solid var(--hair);
}
.row:last-child { border-bottom: none; }
.r-ic { color: var(--text-sub); flex: 0 0 auto; }
.r-tx { min-width: 0; }
.r-label { font-size: 15px; font-weight: 600; white-space: nowrap; }
.r-sub { font-size: 12.5px; color: var(--text-sub); font-weight: 600; white-space: nowrap; margin-top: 2px; }
.r-chev { margin-left: auto; color: var(--text-faint); flex: 0 0 auto; }
.row.danger .r-ic, .row.danger .r-label { color: var(--warn); }

/* 모달 */
.overlay { position: fixed; inset: 0; z-index: 30; background: rgba(40, 30, 20, .34); display: flex; align-items: flex-end; justify-content: center; }
.overlay.dt { align-items: center; padding: 40px; }
.sheet { background: var(--surface); width: 100%; max-width: 440px; box-shadow: var(--shadow-lg); border-radius: 26px 26px 0 0; padding: 22px 22px 28px; }
.sheet.dt { border-radius: 24px; padding: 26px 28px; }
.sheet-top { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 6px; }
.s-title { font-size: 18px; font-weight: 800; }
.s-sub { font-size: 13px; color: var(--text-sub); margin-top: 2px; }
.close { margin-left: auto; border: none; background: var(--surface-soft); border-radius: 50%; width: 34px; height: 34px; display: flex; align-items: center; justify-content: center; color: var(--text-sub); cursor: pointer; flex: 0 0 auto; }
.switch-list { display: flex; flex-direction: column; gap: 10px; margin-top: 16px; }
.sw-card {
  display: flex; align-items: center; gap: 12px; padding: 14px 16px; border-radius: 16px; cursor: pointer; width: 100%;
  background: var(--surface); border: 2px solid var(--hair-strong); font-family: inherit; text-align: left;
}
.sw-card.on { background: var(--brand-100); border-color: var(--brand-500); }
.sw-ic { width: 44px; height: 44px; border-radius: 13px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; background: var(--surface-soft); color: var(--text-sub); }
.sw-ic.on { background: var(--brand-300); color: #9A6B12; }
.sw-info { flex: 1; min-width: 0; }
.sw-row { display: flex; align-items: center; gap: 7px; }
.sw-name { font-size: 16px; font-weight: 800; }
.sw-tag { font-size: 10.5px; font-weight: 700; padding: 4px 9px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }
.sw-tag.now { background: rgba(127, 209, 174, .22); color: #3C8F62; }
.sw-sub { display: block; font-size: 12.5px; color: var(--text-sub); font-weight: 600; margin-top: 2px; white-space: nowrap; }
.sw-view { font-size: 12.5px; font-weight: 800; color: var(--brand-700); flex: 0 0 auto; }
.muted { color: var(--text-sub); }

/* 프로필/비밀번호 수정 폼 */
.form { display: flex; flex-direction: column; gap: 14px; margin-top: 16px; }
.form .jr-input:disabled { color: var(--text-faint); cursor: not-allowed; }
.form-err { font-size: 13px; font-weight: 700; color: var(--warn); }

.toast-wrap { position: fixed; left: 20px; right: 20px; bottom: 110px; z-index: 40; display: flex; justify-content: center; }
.toast-wrap.dt { left: 50%; right: auto; transform: translateX(-50%); bottom: 48px; }
</style>
