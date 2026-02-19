import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/dashboard',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/RegisterView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/dashboard/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/cv',
    name: 'CVUpload',
    component: () => import('@/views/cv/CvUploadView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/jobs',
    name: 'JobSearch',
    component: () => import('@/views/jobs/JobSearchView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/jobs/:id',
    name: 'JobDetail',
    component: () => import('@/views/jobs/JobDetailView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/motivation-letters',
    name: 'MotivationLetters',
    component: () => import('@/views/motivation/MotivationListView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/applications',
    name: 'Applications',
    component: () => import('@/views/applications/ApplicationsListView.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard — redirect to login if not authenticated
router.beforeEach((to, _from, next) => {
  const requiresAuth = to.meta.requiresAuth !== false
  const token = localStorage.getItem('access_token') // Will be replaced by proper auth store in Sprint 1

  if (requiresAuth && !token) {
    next({ name: 'Login' })
  } else {
    next()
  }
})

export default router
