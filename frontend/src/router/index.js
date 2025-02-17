import {createRouter, createWebHistory} from 'vue-router'
import HomeView from '../views/HomeView.vue'
import RegisterComponent from "@/components/RegisterComponent.vue";
import LoginComponent from "@/components/LoginComponent.vue";
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
      path: '/register',
      name: 'register',
      component: RegisterComponent,
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
  ],
})

export default router
