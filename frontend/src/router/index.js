import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'

import IntroView from '../views/IntroView.vue'
import LoginView from '../views/LoginView.vue'
import KakaoCallbackView from '../views/KakaoCallbackView.vue'
import PasswordResetView from '../views/PasswordResetView.vue'
import LegalView from '../views/LegalView.vue'
import ClassroomsView from '../views/ClassroomsView.vue'
import HomeView from '../views/HomeView.vue'
import ChildrenView from '../views/ChildrenView.vue'
import TimelineView from '../views/TimelineView.vue'
import MemoView from '../views/MemoView.vue'
import JournalView from '../views/JournalView.vue'
import AnalysisView from '../views/AnalysisView.vue'
import MeView from '../views/MeView.vue'
import HelpView from '../views/HelpView.vue'

const routes = [
  // 서비스에 처음 들어온(로그아웃) 사용자가 보는 소개 페이지. CTA는 로그인 화면으로 넘긴다.
  { path: '/intro', name: 'intro', component: IntroView, meta: { public: true } },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/oauth/kakao/callback', name: 'kakao-callback', component: KakaoCallbackView, meta: { public: true } },
  // 메일 링크로 들어오는 화면이라 로그인 없이 접근할 수 있어야 한다(비밀번호를 잊은 사람은 토큰이 없다).
  { path: '/reset-password', name: 'reset-password', component: PasswordResetView, meta: { public: true } },
  // 법적 고지 — 로그인 전에도 접근 가능해야 한다(가입 동의 링크·소개 화면에서 진입).
  { path: '/privacy', name: 'privacy', component: LegalView, meta: { public: true, doc: 'privacy' } },
  { path: '/terms', name: 'terms', component: LegalView, meta: { public: true, doc: 'terms' } },
  { path: '/classrooms', name: 'classrooms', component: ClassroomsView },
  { path: '/', name: 'home', component: HomeView, meta: { needsClassroom: true, shell: true } },
  { path: '/memo', name: 'memo', component: MemoView, meta: { needsClassroom: true, shell: true } },
  { path: '/children', name: 'children', component: ChildrenView, meta: { needsClassroom: true, shell: true } },
  { path: '/children/:childId', name: 'timeline', component: TimelineView, meta: { needsClassroom: true, shell: true } },
  { path: '/journal', name: 'journal', component: JournalView, meta: { needsClassroom: true, shell: true } },
  { path: '/analysis', name: 'analysis', component: AnalysisView, meta: { needsClassroom: true, shell: true } },
  { path: '/me', name: 'me', component: MeView, meta: { shell: true } },
  { path: '/help', name: 'help', component: HelpView, meta: { shell: true } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  // 로그아웃 상태에서 보호된 화면에 접근하면 로그인으로 보낸다.
  // 단, 루트(홈)로 처음 들어온 사용자는 소개 페이지(/intro)로 — 홍보 유입의 첫인상은
  // 로그인 벽이 아니라 서비스 소개여야 한다. 그 외 깊은 링크는 로그인으로.
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: to.name === 'home' ? 'intro' : 'login' }
  }
  // 이미 로그인한 사용자는 소개·로그인 화면을 건너뛰고 바로 앱으로.
  if ((to.name === 'login' || to.name === 'intro') && auth.isAuthenticated) {
    return { name: session.classroom ? 'home' : 'classrooms' }
  }
  if (to.meta.needsClassroom && !session.classroom) {
    return { name: 'classrooms' }
  }
  return true
})

export default router
