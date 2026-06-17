import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'

import LoginView from '../views/LoginView.vue'
import ClassroomsView from '../views/ClassroomsView.vue'
import HomeView from '../views/HomeView.vue'
import ChildrenView from '../views/ChildrenView.vue'
import TimelineView from '../views/TimelineView.vue'
import MemoView from '../views/MemoView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/classrooms', name: 'classrooms', component: ClassroomsView },
  { path: '/', name: 'home', component: HomeView, meta: { needsClassroom: true } },
  { path: '/memo', name: 'memo', component: MemoView, meta: { needsClassroom: true } },
  { path: '/children', name: 'children', component: ChildrenView, meta: { needsClassroom: true } },
  { path: '/children/:childId', name: 'timeline', component: TimelineView, meta: { needsClassroom: true } },
  {
    path: '/journal', name: 'journal', component: PlaceholderView,
    meta: { needsClassroom: true, title: '일지·분석', note: 'AI 보육일지/개인평가 (Epic 3·4 예정)' },
  },
  {
    path: '/me', name: 'me', component: PlaceholderView,
    meta: { title: '마이', note: '프로필·반 전환·설정 (예정)' },
  },
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
