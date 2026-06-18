import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import DashboardView from '@/views/DashboardView.vue'
import LoginView from '@/views/LoginView.vue'
import OAuthCallbackView from '@/views/OAuthCallbackView.vue'
import DeploymentsView from '@/views/DeploymentsView.vue'
import DomainsView from '@/views/DomainsView.vue'
import TeamView from '@/views/TeamView.vue'
import SiteDetailView from '@/views/SiteDetailView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
    { path: '/auth/callback', name: 'oauth-callback', component: OAuthCallbackView, meta: { public: true } },
    { path: '/', name: 'dashboard', component: DashboardView },
    { path: '/deployments', name: 'deployments', component: DeploymentsView },
    { path: '/domains', name: 'domains', component: DomainsView },
    { path: '/team', name: 'team', component: TeamView },
    { path: '/sites/:id', name: 'site-detail', component: SiteDetailView },
  ],
})

router.beforeEach(async (to) => {
  const auth = useAuth()
  await auth.initialize()
  if (!to.meta.public && !auth.isAuthenticated.value) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isAuthenticated.value) {
    return { name: 'dashboard' }
  }
})

export default router

