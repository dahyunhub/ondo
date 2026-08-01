import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'

import LoginView from '../views/LoginView.vue'
import KakaoCallbackView from '../views/KakaoCallbackView.vue'
import PasswordResetView from '../views/PasswordResetView.vue'
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
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/oauth/kakao/callback', name: 'kakao-callback', component: KakaoCallbackView, meta: { public: true } },
  // 메일 링크로 들어오는 화면이라 로그인 없이 접근할 수 있어야 한다(비밀번호를 잊은 사람은 토큰이 없다).
  { path: '/reset-password', name: 'reset-password', component: PasswordResetView, meta: { public: true } },
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
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login' }
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: session.classroom ? 'home' : 'classrooms' }
  }
  if (to.meta.needsClassroom && !session.classroom) {
    return { name: 'classrooms' }
  }
  return true
})

export default router
