<script setup>
import { useRouter } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'
import Avatar from '../components/Avatar.vue'
import AppIcon from '../components/AppIcon.vue'

const router = useRouter()
const teacherName = auth.teacher?.name || '선생님'
</script>

<template>
  <div class="home">
    <header class="hdr screen">
      <button class="cls-btn" @click="router.push({ name: 'classrooms' })">
        {{ session.classroom?.name }} <AppIcon name="chevD" :size="16" :stroke="2.4" />
      </button>
      <span class="year">{{ session.classroom?.year }}학년도</span>
      <Avatar :name="teacherName" size="sm" style="margin-left:auto" />
    </header>

    <div class="screen body">
      <h1 class="jr-h1 greet">{{ teacherName }} 선생님,<br />좋은 하루예요 ☀️</h1>
      <p class="sub">우리 반 아이들을 관리하고 기록을 남겨보세요.</p>

      <RouterLink to="/memo" class="cta">
        <span class="cta-ic"><AppIcon name="plus" :size="26" :stroke="2.4" /></span>
        <span class="cta-txt">
          <span class="t">빠른 메모</span>
          <span class="d">아이 고르고 한 줄 · 10초면 충분해요</span>
        </span>
        <AppIcon name="chevR" :size="22" />
      </RouterLink>

      <RouterLink to="/children" class="cta2">
        <span class="cta2-ic"><AppIcon name="children" :size="24" /></span>
        <span class="cta2-txt">
          <span class="t">아이들 관리</span>
          <span class="d">명단 · 등록 · 수정 · 타임라인</span>
        </span>
        <AppIcon name="chevR" :size="20" />
      </RouterLink>

      <div class="soon-label">곧 추가돼요</div>
      <div class="soon">
        <div class="soon-card">
          <span class="soon-ic"><AppIcon name="sparkle" :size="22" /></span>
          <span class="soon-t">AI 보육일지</span>
          <span class="soon-badge">Epic 3 예정</span>
        </div>
        <div class="soon-card">
          <span class="soon-ic"><AppIcon name="me" :size="22" /></span>
          <span class="soon-t">개인 관찰평가</span>
          <span class="soon-badge">Epic 4 예정</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home { display: flex; flex-direction: column; }
.hdr { display: flex; align-items: center; gap: 10px; padding-top: 14px; padding-bottom: 14px; }
.cls-btn {
  display: flex; align-items: center; gap: 6px; background: var(--surface);
  border: 1.5px solid var(--hair-strong); border-radius: 999px; padding: 7px 12px 7px 14px;
  font-family: inherit; font-weight: 800; font-size: 15px; color: var(--text); cursor: pointer;
}
.year { font-size: 13px; color: var(--text-sub); font-weight: 600; }
.body { padding-top: 4px; padding-bottom: 24px; }
.greet { margin: 0 0 4px; }
.sub { font-size: 14px; color: var(--text-sub); margin: 0 0 20px; }
.cta {
  display: flex; align-items: center; gap: 16px; padding: 18px 20px; border-radius: 20px;
  background: linear-gradient(135deg, #FFD45E, #F5B940); box-shadow: 0 8px 22px rgba(245, 185, 64, .4);
  color: var(--text); text-decoration: none;
}
.cta-ic {
  width: 48px; height: 48px; border-radius: 16px; flex: 0 0 auto;
  background: rgba(255, 255, 255, .45); display: flex; align-items: center; justify-content: center;
}
.cta-txt { flex: 1; display: flex; flex-direction: column; }
.cta-txt .t { font-size: 17px; font-weight: 800; }
.cta-txt .d { font-size: 13.5px; color: #7a5e22; font-weight: 600; margin-top: 2px; }
.cta2 {
  display: flex; align-items: center; gap: 14px; padding: 16px 18px; margin-top: 12px; border-radius: 20px;
  background: var(--surface); box-shadow: var(--shadow-sm); border: 1.5px solid var(--hair);
  color: var(--text); text-decoration: none;
}
.cta2-ic { width: 44px; height: 44px; border-radius: 14px; flex: 0 0 auto; background: var(--brand-100); color: var(--brand-700); display: flex; align-items: center; justify-content: center; }
.cta2-txt { flex: 1; display: flex; flex-direction: column; }
.cta2-txt .t { font-size: 16px; font-weight: 800; }
.cta2-txt .d { font-size: 13px; color: var(--text-sub); font-weight: 600; margin-top: 2px; }
.soon-label { font-size: 13px; font-weight: 800; color: var(--text-sub); margin: 26px 0 10px; }
.soon { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.soon-card {
  display: flex; flex-direction: column; gap: 8px; padding: 16px; border-radius: 18px;
  background: var(--surface); border: 1.5px solid var(--hair); color: var(--text-faint);
}
.soon-ic {
  width: 40px; height: 40px; border-radius: 12px; background: var(--surface-soft);
  display: flex; align-items: center; justify-content: center;
}
.soon-t { font-size: 14.5px; font-weight: 800; color: var(--text-sub); }
.soon-badge {
  align-self: flex-start; font-size: 11px; font-weight: 700; padding: 3px 8px;
  border-radius: 999px; background: var(--surface-soft); color: var(--text-faint);
}
</style>
