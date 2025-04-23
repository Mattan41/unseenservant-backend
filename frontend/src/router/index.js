import {createRouter, createWebHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginComponent from "@/components/LoginComponent.vue";
import CampaignView from "@/features/campaign/views/CampaignView.vue";
import {useAuthStore} from "@/features/auth/authStore.js";
import CharacterView from "@/features/character/views/CharacterView.vue";
import CreateCharacter from "@/features/character/components/CreateCharacter.vue";
import EditCharacter from "@/features/character/components/EditCharacter.vue";
import {useNotificationStore} from "@/stores/notificationStore.js";

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
      beforeEnter: async (to, from, next) => {
        try {
          const authStore = useAuthStore();
          const notificationStore = useNotificationStore();

          await authStore.logout();
          notificationStore.addNotification('You have been logged out successfully', 'success');
          next({ name: 'home' });
        } catch (error) {
          console.error('Logout error:', error);
          const notificationStore = useNotificationStore();
          notificationStore.addNotification('Failed to log out properly', 'error');
          next({ name: 'home' });
        }
      },
    },
    {
      path: '/redirect',
      name: 'redirect',
      beforeEnter: async (to, from, next) => {
        try {
          const authStore = useAuthStore();
          const notificationStore = useNotificationStore();

          authStore.loadUserFromLocalStorage();

          if (!authStore.isLoggedIn) {
            await authStore.checkAuth();
          }

          if (authStore.isLoggedIn) {
            notificationStore.addNotification('Successfully redirected!', 'success');
            next({ name: 'home' });
          } else {
            notificationStore.addNotification('Session expired. Please login again.', 'warning');
            next({ name: 'home' });
          }
        } catch (error) {
          console.error('Error in redirect route:', error);
          next({ name: 'home' });
        }
      },
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
      path: '/:catchAll(.*)*',
      name: 'notFound',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore();

  try {
    // Initialize auth store if not already initialized
    if (!authStore.authInitialized) {
      console.log('Router guard: Auth not initialized, initializing...');
      await authStore.checkAuth();
    }

    // Route navigation logic
    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
      console.log('Redirecting to login - auth required but not logged in');
      next({ name: 'login' });
    } else if (to.name === 'login' && authStore.isLoggedIn) {
      console.log('Already logged in, redirecting to campaigns');
      next({ name: 'home' });
    } else {
      console.log('Continuing to requested route:', to.path);
      next();
    }
  } catch (error) {
    console.error('Error in router guard:', error);
    // Safely continue to requested route even if auth check fails
    next();
  }
});


export default router
