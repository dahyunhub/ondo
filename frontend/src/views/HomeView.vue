<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../lib/api'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'
import MeetSoonCard from '../components/MeetSoonCard.vue'

const router = useRouter()
const { isDesktop } = useViewport()
const teacherName = auth.teacher?.name || '선생님'
const shortTeacher = teacherName.length > 2 ? teacherName.slice(1) : teacherName

const today = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

function go(name) { router.push({ name }) }

// 지금까지 만든 일지(최신순) — 클릭 시 일지 페이지에서 해당 일지 열기.
const journals = ref([])
function fmtDate(iso) { return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric' }).format(new Date(iso)) }
function openJournal(id) { router.push({ name: 'journal', query: { journalId: id } }) }
// 이번 주 만나볼 아이 — 최근 기록이 옅은 아이.
// 인원 상한은 서버(WarmthService.LOW_CAP)가 무조건 3명으로 강제하므로 여기서 다시 자르지 않는다.
// 온도 API 는 childId·level 만 주므로 이름·사진은 명단에서 가져와 붙인다.
const meetSoon = ref([])
async function loadMeetSoon(cid) {
  try {
    const [warmth, children] = await Promise.all([
      api.get(`/classrooms/${cid}/warmth`),
      api.get(`/classrooms/${cid}/children`),
    ])
    if (!warmth?.enabled) return
    const lowIds = new Set(warmth.items.filter((i) => i.level === 'LOW').map((i) => i.childId))
    meetSoon.value = children.filter((c) => lowIds.has(c.id))
  } catch { /* 부가 정보 — 홈은 그대로 뜬다 */ }
}

// 잠시 접어두기 — 낙관적으로 목록에서 먼저 빼고, 실패하면 되돌린다.
async function snoozeChild(child) {
  const before = meetSoon.value
  meetSoon.value = before.filter((c) => c.id !== child.id)
  try {
    await api.post(`/children/${child.id}/warmth-snooze`)
  } catch {
    meetSoon.value = before
  }
}

onMounted(async () => {
  const cid = session.classroom?.id
  if (!cid) return
  loadMeetSoon(cid)
  try { journals.value = await api.get(`/journals/list?classroomId=${cid}`) } catch (e) { /* 비치명적 */ }
})
</script>

<template>
  <!-- ============ 데스크톱 ============ -->
  <div v-if="isDesktop" class="dt-page home-dt">
    <header class="dt-head">
      <div>
        <div class="jr-display">{{ shortTeacher }} 선생님, 좋은 하루예요 ☀️</div>
        <div class="dt-sub">{{ today }} · {{ session.classroom?.name }}</div>
      </div>
      <button class="jr-btn jr-btn--primary" @click="go('memo')"><AppIcon name="plus" :size="22" :stroke="2.6" /> 빠른 메모</button>
    </header>

    <div class="grid">
      <div class="col-main">
        <div class="sec-title"><span class="jr-h2">바로 가기</span></div>
        <button class="action" @click="go('memo')">
          <span class="action-ic brand"><AppIcon name="plus" :size="24" :stroke="2.4" /></span>
          <span class="action-tx"><span class="t">빠른 메모</span><span class="d">아이 고르고 한 줄 · 10초면 충분해요</span></span>
          <AppIcon name="chevR" :size="22" />
        </button>
        <button class="action" @click="go('children')">
          <span class="action-ic soft"><AppIcon name="children" :size="24" /></span>
          <span class="action-tx"><span class="t">아이들 관리</span><span class="d">명단 · 등록 · 수정 · 타임라인</span></span>
          <AppIcon name="chevR" :size="22" />
        </button>

        <div class="sec-title" style="margin-top:28px"><span class="jr-h2">지금까지 만든 일지</span></div>
        <div v-if="journals.length" class="jlist">
          <button v-for="j in journals" :key="j.id" class="jrow" @click="openJournal(j.id)">
            <span class="jrow-ic"><AppIcon name="journal" :size="20" /></span>
            <span class="jrow-body">
              <span class="jrow-t">{{ fmtDate(j.journalDate) }}</span>
              <span class="jrow-d">{{ j.summary || '요약 없음' }}</span>
            </span>
            <span class="jrow-st" :class="j.status === 'CONFIRMED' ? 'on' : ''">{{ j.status === 'CONFIRMED' ? '확정' : '초안' }}</span>
            <AppIcon name="chevR" :size="18" />
          </button>
        </div>
        <div v-else class="jempty">아직 만든 일지가 없어요. 오늘 메모로 첫 일지를 만들어 보세요.</div>
      </div>

      <div class="col-side">
        <div class="cta" @click="go('journal')">
          <span class="cta-ic"><AppIcon name="sparkle" :size="30" /></span>
          <div class="cta-tx"><div class="t">AI 일지 쓰기</div><div class="d">오늘 메모로 하루 일지를 만들어요</div></div>
          <AppIcon name="chevR" :size="24" />
        </div>
        <MeetSoonCard v-if="meetSoon.length" :children="meetSoon" @snooze="snoozeChild" />
        <div class="jr-card soon-card">
          <div class="soon-h">AI 분석</div>
          <button class="soon-row act" @click="go('journal')">
            <span class="soon-ic"><AppIcon name="sparkle" :size="18" /></span> AI 일지
            <AppIcon name="chevR" :size="16" class="soon-arrow" />
          </button>
          <button class="soon-row act" @click="go('children')">
            <span class="soon-ic"><AppIcon name="me" :size="18" /></span> 개인 관찰평가
            <AppIcon name="chevR" :size="16" class="soon-arrow" />
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- ============ 모바일 ============ -->
  <div v-else class="home-m">
    <header class="m-head">
      <button class="cls-btn" @click="go('classrooms')">
        {{ session.classroom?.name }} <AppIcon name="chevD" :size="16" :stroke="2.4" />
      </button>
      <span class="m-date">{{ today }}</span>
      <Avatar :name="teacherName" size="sm" style="margin-left:auto"
              photo-url="/teachers/me/photo" :photo-key="auth.teacher?.photoUpdatedAt || ''" />
    </header>

    <div class="screen m-body">
      <h1 class="jr-h1 greet">{{ shortTeacher }} 선생님,<br />좋은 하루예요 ☀️</h1>
      <p class="greet-sub">빠른 메모로 오늘을 기록해 보세요.</p>

      <div class="cta" @click="go('journal')">
        <span class="cta-ic"><AppIcon name="sparkle" :size="26" /></span>
        <div class="cta-tx"><div class="t">AI 일지 쓰기</div><div class="d">오늘 메모로 하루 일지를 만들어요</div></div>
        <AppIcon name="chevR" :size="24" />
      </div>

      <div class="sec-title" style="margin-top:24px"><span class="jr-h2" style="font-size:18px">바로 가기</span></div>
      <button class="action" @click="go('memo')">
        <span class="action-ic brand"><AppIcon name="plus" :size="24" :stroke="2.4" /></span>
        <span class="action-tx"><span class="t">빠른 메모</span><span class="d">아이 고르고 한 줄 · 10초면 충분해요</span></span>
        <AppIcon name="chevR" :size="22" />
      </button>
      <button class="action" @click="go('children')">
        <span class="action-ic soft"><AppIcon name="children" :size="24" /></span>
        <span class="action-tx"><span class="t">아이들 관리</span><span class="d">명단 · 등록 · 수정 · 타임라인</span></span>
        <AppIcon name="chevR" :size="22" />
      </button>

      <MeetSoonCard v-if="meetSoon.length" :children="meetSoon" style="margin-top:24px" @snooze="snoozeChild" />

      <div class="soon-label">지금까지 만든 일지</div>
      <div v-if="journals.length" class="jlist">
        <button v-for="j in journals" :key="j.id" class="jrow" @click="openJournal(j.id)">
          <span class="jrow-ic"><AppIcon name="journal" :size="20" /></span>
          <span class="jrow-body">
            <span class="jrow-t">{{ fmtDate(j.journalDate) }}</span>
            <span class="jrow-d">{{ j.summary || '요약 없음' }}</span>
          </span>
          <span class="jrow-st" :class="j.status === 'CONFIRMED' ? 'on' : ''">{{ j.status === 'CONFIRMED' ? '확정' : '초안' }}</span>
          <AppIcon name="chevR" :size="18" />
        </button>
      </div>
      <div v-else class="jempty">아직 만든 일지가 없어요.<br>오늘 메모로 첫 일지를 만들어 보세요.</div>

      <div class="soon-label">AI 분석</div>
      <div class="soon-grid">
        <button class="soon-mini act" @click="go('journal')">
          <span class="soon-ic"><AppIcon name="sparkle" :size="22" /></span>
          <span class="soon-t">AI 일지</span>
        </button>
        <button class="soon-mini act" @click="go('children')">
          <span class="soon-ic"><AppIcon name="me" :size="22" /></span>
          <span class="soon-t">개인 관찰평가</span>
        </button>
      </div>
    </div>

    <!-- 빠른 메모 FAB -->
    <button class="fab" @click="go('memo')"><AppIcon name="plus" :size="24" :stroke="2.6" /> 메모</button>
  </div>
</template>

<style scoped>
/* 공통: CTA / 액션 카드 */
.cta {
  display: flex; align-items: center; gap: 16px; padding: 18px 20px; border-radius: 20px; cursor: pointer;
  background: linear-gradient(135deg, #FFD45E, #F5B940); box-shadow: 0 8px 22px rgba(245, 185, 64, .4); color: var(--text);
}
.cta-ic { width: 48px; height: 48px; border-radius: 16px; flex: 0 0 auto; background: rgba(255, 255, 255, .45); display: flex; align-items: center; justify-content: center; }
.cta-tx { flex: 1; }
.cta-tx .t { font-size: 17px; font-weight: 800; }
.cta-tx .d { font-size: 13.5px; color: #7a5e22; font-weight: 600; margin-top: 2px; }
.action {
  display: flex; align-items: center; gap: 14px; padding: 16px 18px; margin-top: 12px; border-radius: 20px; width: 100%;
  background: var(--surface); box-shadow: var(--shadow-sm); border: 1.5px solid var(--hair); color: var(--text);
  font-family: inherit; text-align: left; cursor: pointer;
}
.action-ic { width: 44px; height: 44px; border-radius: 14px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; }
.action-ic.brand { background: var(--brand-100); color: var(--brand-700); }
.action-ic.soft { background: var(--surface-soft); color: var(--text-sub); }
.action-tx { flex: 1; display: flex; flex-direction: column; }
.action-tx .t { font-size: 16px; font-weight: 800; }
.action-tx .d { font-size: 13px; color: var(--text-sub); font-weight: 600; margin-top: 2px; }

/* 지금까지 만든 일지 리스트 */
.jlist { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }
.jrow {
  display: flex; align-items: center; gap: 12px; width: 100%; padding: 13px 16px; border-radius: 16px; cursor: pointer;
  background: var(--surface); border: 1.5px solid var(--hair); box-shadow: var(--shadow-sm); font-family: inherit; text-align: left;
  transition: border-color .12s, background .12s;
}
.jrow:hover { border-color: var(--brand-500); background: var(--brand-100); }
.jrow-ic { width: 38px; height: 38px; border-radius: 12px; flex: 0 0 auto; display: flex; align-items: center; justify-content: center; background: var(--brand-100); color: var(--brand-700); }
.jrow-body { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.jrow-t { font-size: 14.5px; font-weight: 800; color: var(--text); }
.jrow-d { font-size: 12.5px; color: var(--text-sub); margin-top: 2px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.jrow-st { font-size: 11px; font-weight: 800; padding: 3px 9px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); white-space: nowrap; flex: 0 0 auto; }
.jrow-st.on { background: var(--brand-100); color: var(--brand-700); }
.jempty { font-size: 13px; color: var(--text-faint); line-height: 1.5; padding: 18px; border-radius: 16px; background: var(--surface); box-shadow: var(--shadow-sm); margin-top: 12px; }

/* 데스크톱 */
.dt-head { display: flex; align-items: flex-end; margin-bottom: 28px; }
.dt-sub { font-size: 15px; color: var(--text-sub); margin-top: 6px; }
.dt-head .jr-btn { margin-left: auto; }
.grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 28px; align-items: start; }
/* min-width:0 이 없으면 자식(nowrap 요약 텍스트)이 트랙을 밀어 오른쪽으로 넘친다 */
.col-main { min-width: 0; }
.sec-title { display: flex; align-items: center; margin-bottom: 14px; }
.col-side { position: sticky; top: 40px; min-width: 0; display: flex; flex-direction: column; gap: 18px; }
.col-side .cta { margin: 0; padding: 22px 26px; }
.col-side .cta-ic { width: 56px; height: 56px; }
.col-side .cta-tx .t { font-size: 19px; }
.soon-card { padding: 18px; }
.soon-h { font-size: 14px; font-weight: 800; margin-bottom: 12px; }
.soon-row { display: flex; align-items: center; gap: 10px; padding: 10px 0; font-size: 14.5px; font-weight: 700; color: var(--text-sub); }
.soon-row.act { width: 100%; border: none; background: transparent; font-family: inherit; cursor: pointer; padding: 10px 6px; border-radius: 12px; color: var(--text); transition: background .12s; }
.soon-row.act:hover { background: var(--surface-soft); }
.soon-arrow { margin-left: auto; color: var(--text-faint); }
.soon-row .soon-ic { width: 34px; height: 34px; border-radius: 11px; background: var(--surface-soft); color: var(--text-sub); display: flex; align-items: center; justify-content: center; }
.soon-row.act .soon-ic { background: var(--brand-100); color: var(--brand-700); }

/* 모바일 */
.home-m { display: flex; flex-direction: column; position: relative; }
.m-head { display: flex; align-items: center; gap: 10px; padding: 14px 20px; }
.cls-btn {
  display: flex; align-items: center; gap: 6px; background: var(--surface); border: 1.5px solid var(--hair-strong);
  border-radius: 999px; padding: 7px 12px 7px 14px; font-family: inherit; font-weight: 800; font-size: 15px; color: var(--text); cursor: pointer;
}
.m-date { font-size: 13px; color: var(--text-sub); font-weight: 600; white-space: nowrap; }
.m-body { padding-top: 4px; padding-bottom: 24px; }
.greet { margin: 0 0 4px; }
.greet-sub { font-size: 14px; color: var(--text-sub); margin: 0 0 20px; }
.soon-label { font-size: 13px; font-weight: 800; color: var(--text-sub); margin: 26px 0 10px; }
.soon-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.soon-mini {
  display: flex; flex-direction: column; gap: 8px; padding: 16px; border-radius: 18px;
  background: var(--surface); border: 1.5px solid var(--hair);
}
.soon-mini.act { cursor: pointer; font-family: inherit; text-align: left; transition: border-color .12s, background .12s; }
.soon-mini.act:hover { border-color: var(--brand-500); background: var(--brand-100); }
.soon-mini.act .soon-ic { background: var(--brand-100); color: var(--brand-700); }
.soon-mini .soon-ic { width: 40px; height: 40px; border-radius: 12px; background: var(--surface-soft); color: var(--text-sub); display: flex; align-items: center; justify-content: center; }
.soon-t { font-size: 14.5px; font-weight: 800; color: var(--text-sub); }
.soon-mini.act .soon-t { color: var(--text); }
.fab {
  position: fixed; right: 20px; bottom: 86px; z-index: 9; border: none; cursor: pointer; font-family: inherit;
  display: inline-flex; align-items: center; gap: 8px; height: 56px; padding: 0 22px 0 18px;
  border-radius: 999px; background: var(--brand-500); color: var(--text); font-weight: 800; font-size: 16px;
  box-shadow: 0 8px 22px rgba(245, 185, 64, 0.5);
}
@media (min-width: 540px) and (max-width: 899px) { .fab { right: calc(50% - 260px + 20px); } }
</style>
