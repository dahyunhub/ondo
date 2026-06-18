import { createRouter, createWebHistory } from 'vue-router'
import { auth } from '../stores/auth'
import { session } from '../stores/session'

import LoginView from '../views/LoginView.vue'
import ClassroomsView from '../views/ClassroomsView.vue'
import HomeView from '../views/HomeView.vue'
import ChildrenView from '../views/ChildrenView.vue'
import TimelineView from '../views/TimelineView.vue'
import MemoView from '../views/MemoView.vue'
import JournalView from '../views/JournalView.vue'
import MeView from '../views/MeView.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/classrooms', name: 'classrooms', component: ClassroomsView },
  { path: '/', name: 'home', component: HomeView, meta: { needsClassroom: true, shell: true } },
  { path: '/memo', name: 'memo', component: MemoView, meta: { needsClassroom: true, shell: true } },
  { path: '/children', name: 'children', component: ChildrenView, meta: { needsClassroom: true, shell: true } },
  { path: '/children/:childId', name: 'timeline', component: TimelineView, meta: { needsClassroom: true, shell: true } },
  { path: '/journal', name: 'journal', component: JournalView, meta: { needsClassroom: true, shell: true } },
  { path: '/me', name: 'me', component: MeView, meta: { shell: true } },
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
