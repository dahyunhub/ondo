<script setup>
import { useRouter } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import { useViewport } from '../lib/useViewport'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const { isDesktop } = useViewport()
const teacherName = auth.teacher?.name || '선생님'
const shortTeacher = teacherName.length > 2 ? teacherName.slice(1) : teacherName

const today = new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'long' }).format(new Date())

function go(name) { router.push({ name }) }
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
      </div>

      <div class="col-side">
        <div class="cta" @click="go('journal')">
          <span class="cta-ic"><AppIcon name="sparkle" :size="30" /></span>
          <div class="cta-tx"><div class="t">AI 일지 쓰기</div><div class="d">오늘 메모로 하루 일지를 만들어요</div></div>
          <AppIcon name="chevR" :size="24" />
        </div>
        <div class="jr-card soon-card">
          <div class="soon-h">곧 제공돼요</div>
          <div class="soon-row"><span class="soon-ic"><AppIcon name="sparkle" :size="18" /></span> AI 보육일지 <span class="soon-b">Epic 3</span></div>
          <div class="soon-row"><span class="soon-ic"><AppIcon name="me" :size="18" /></span> 개인 관찰평가 <span class="soon-b">Epic 4</span></div>
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
      <Avatar :name="teacherName" size="sm" style="margin-left:auto" />
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

      <div class="soon-label">곧 추가돼요</div>
      <div class="soon-grid">
        <div class="soon-mini">
          <span class="soon-ic"><AppIcon name="sparkle" :size="22" /></span>
          <span class="soon-t">AI 보육일지</span><span class="soon-b">Epic 3</span>
        </div>
        <div class="soon-mini">
          <span class="soon-ic"><AppIcon name="me" :size="22" /></span>
          <span class="soon-t">개인 관찰평가</span><span class="soon-b">Epic 4</span>
        </div>
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

/* 데스크톱 */
.dt-head { display: flex; align-items: flex-end; margin-bottom: 28px; }
.dt-sub { font-size: 15px; color: var(--text-sub); margin-top: 6px; }
.dt-head .jr-btn { margin-left: auto; }
.grid { display: grid; grid-template-columns: 1.5fr 1fr; gap: 28px; align-items: start; }
.sec-title { display: flex; align-items: center; margin-bottom: 14px; }
.col-side { position: sticky; top: 40px; display: flex; flex-direction: column; gap: 18px; }
.col-side .cta { margin: 0; padding: 22px 26px; }
.col-side .cta-ic { width: 56px; height: 56px; }
.col-side .cta-tx .t { font-size: 19px; }
.soon-card { padding: 18px; }
.soon-h { font-size: 14px; font-weight: 800; margin-bottom: 12px; }
.soon-row { display: flex; align-items: center; gap: 10px; padding: 10px 0; font-size: 14.5px; font-weight: 700; color: var(--text-sub); }
.soon-row .soon-ic { width: 34px; height: 34px; border-radius: 11px; background: var(--surface-soft); color: var(--text-sub); display: flex; align-items: center; justify-content: center; }
.soon-b { margin-left: auto; font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }

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
.soon-mini .soon-ic { width: 40px; height: 40px; border-radius: 12px; background: var(--surface-soft); color: var(--text-sub); display: flex; align-items: center; justify-content: center; }
.soon-t { font-size: 14.5px; font-weight: 800; color: var(--text-sub); }
.soon-b { align-self: flex-start; font-size: 11px; font-weight: 700; padding: 3px 8px; border-radius: 999px; background: var(--surface-soft); color: var(--text-faint); }
.fab {
  position: fixed; right: 20px; bottom: 86px; z-index: 9; border: none; cursor: pointer; font-family: inherit;
  display: inline-flex; align-items: center; gap: 8px; height: 56px; padding: 0 22px 0 18px;
  border-radius: 999px; background: var(--brand-500); color: var(--text); font-weight: 800; font-size: 16px;
  box-shadow: 0 8px 22px rgba(245, 185, 64, 0.5);
}
@media (min-width: 540px) and (max-width: 899px) { .fab { right: calc(50% - 260px + 20px); } }
</style>
