import {createRouter, createWebHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginComponent from "@/components/LoginComponent.vue";
import CampaignView from "@/views/CampaignView.vue";
import {useAuthStore} from "@/stores/authStore.js";

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
      beforeEnter: (to, from, next) => {
        const authStore = useAuthStore();
        authStore.logout();
        next({name: 'home'});
      },
    },
    {
      path: '/redirect',
      name: 'redirect',
      beforeEnter: (to, from, next) => {
        const authStore = useAuthStore();
        authStore.loadUserFromLocalStorage();
        next({name: 'home'});
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
      component: () => import('../views/UserProfileView.vue'),
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
      component: () => import('../views/CampaignsView.vue'),
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

  //Initialize auth store if not already initialized
  if (!authStore.authInitialized) {
    console.log('Router guard: Auth not initialized, initializing...');
    await authStore.checkAuth();
  }

  // Debugging
  console.log('Current route:', to.name);
  console.log('Auth status:', authStore.isLoggedIn);
  console.log('Route requires auth:', to.meta.requiresAuth);

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    console.log('Redirecting to home - auth required but not logged in');
    next({name: 'home'});
  } else if (to.name === 'login' && authStore.isLoggedIn) {
    console.log('Already logged in, redirecting to campaigns');
    next({name: 'CampaignsView'});
  } else {
    console.log('Continuing to requested route');
    next();
  }
});

export default router
