import {createRouter, createWebHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginComponent from '@/components/LoginComponent.vue'
import {useAuthStore} from '@/features/auth/authStore.js'

/**
 * @typedef {Object} RouteMeta
 * @property {boolean} [requiresAuth] - Whether the route requires authentication
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/about',
      name: 'about',
      component: () => import('../views/AboutView.vue'),
    },
    {
      path: '/login',
      name: 'login',
      component: LoginComponent,
    },
    {
      path: '/logout',
      name: 'logout',
      component: () => import('@/views/LogoutView.vue'),
    },
    {
      path: '/oauth-redirect',
      name: 'oauth-redirect',
      component: () => import('../components/OAuthRedirect.vue'),
    },
    {
      path: '/user-profile',
      name: 'user-profile',
      component: () => import('../features/user/UserProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/campaign/:id',
      name: 'CampaignView',
      component: () => import('@/features/campaign/views/CampaignView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/campaigns',
      name: 'CampaignsView',
      component: () => import('../features/campaign/views/CampaignsView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/characters/create',
      name: 'CreateCharacter',
      component: () => import('@/features/character/components/CreateCharacter.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/characters/:id/edit',
      name: 'EditCharacter',
      component: () => import('@/features/character/components/EditCharacter.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/characters/:id',
      name: 'CharacterView',
      component: () => import('@/features/character/views/CharacterView.vue'),
      props: true,
      meta: { requiresAuth: true },
    },
    {
      path: '/characters',
      name: 'CharactersView',
      component: () => import('@/features/character/views/CharactersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/under-construction',
      name: 'underConstructionView',
      component: () => import('@/views/UnderConstructionView.vue'),
    },
    {
      path: '/:catchAll(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  if (!authStore.isAuthChecked) {
    await authStore.initializeAuth()
  }

  const isAuthenticated = authStore.isAuthenticated

  if (to.meta?.requiresAuth && !isAuthenticated) {
    next({ name: 'login' })
  } else if (to.name === 'login' && isAuthenticated) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
