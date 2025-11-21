import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginComponent from '@/components/LoginComponent.vue'
import CampaignView from '@/features/campaign/views/CampaignView.vue'
import { useAuthStore } from '@/features/auth/authStore.js'
import CharacterView from '@/features/character/views/CharacterView.vue'
import CreateCharacter from '@/features/character/components/CreateCharacter.vue'
import EditCharacter from '@/features/character/components/EditCharacter.vue'

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
      component: () => import('../components/OAuthRedirect.vue')
    },
    {
      path: '/user-profile',
      name: 'user-profile',
      component: () => import('../features/user/UserProfileView.vue'),
      meta: {requiresAuth: true}
    },
    {
      path: '/campaign/:id',
      name: 'CampaignView',
      component: CampaignView,
      props: true,
      meta: {requiresAuth: true}
    },
    {
      path: '/campaigns',
      name: 'CampaignsView',
      component: () => import('../features/campaign/views/CampaignsView.vue'),
      meta: {requiresAuth: true}
    },
    {
      path: '/characters/create',
      name: 'CreateCharacter',
      component: CreateCharacter,
      meta: {requiresAuth: true}
    },
    {
      path: '/characters/:id/edit',
      name: 'EditCharacter',
      component: EditCharacter,
      props: true,
      meta: {requiresAuth: true}
    },
    {
      path: '/characters/:id',
      name: 'CharacterView',
      component: CharacterView,
      props: true,
      meta: {requiresAuth: true}
    },
    {
      path: '/characters',
      name: 'CharactersView',
      component: () => import('@/features/character/views/CharactersView.vue'),
      meta: {requiresAuth: true}
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

  if (!authStore.isInitialized) {
    await authStore.initializeAuth()
  }

  if (to.meta?.requiresAuth && !authStore.isAuthenticated) {
    next({ name: 'login' })
  } else if (to.name === 'login' && authStore.isAuthenticated) {
    next({ name: 'home' })
  } else {
    next()
  }
})

export default router
