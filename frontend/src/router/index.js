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
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next({name: 'home'});
  } else {
    next();
  }
});
export default router
