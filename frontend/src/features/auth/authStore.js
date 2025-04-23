// features/auth/authStore.js
import { defineStore } from 'pinia';
import authService from './authService.js';
import router from '@/router/index.js';
import { useUserStore } from "@/features/user/userStore.js";

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isLoggedIn: false,
    isAuthenticating: false,
    authInitialized: false,
    error: null
  }),

  actions: {
    clearUser() {
      this.user = null;
      this.isLoggedIn = false;
      this.error = null;
    },

    async fetchCurrentUser() {
      try {
        const userData = await authService.getCurrentUser();
        if (userData) {
          this.user = userData;
          this.isLoggedIn = true;
          return userData;
        } else {
          this.clearUser();
          return null;
        }
      } catch (error) {
        console.error('Error fetching user in store:', error);
        this.error = 'Failed to fetch user data';
        this.clearUser();
        return null;
      }
    },

    // This matches what  App.vue is calling
    async checkAuth() {
      this.isAuthenticating = true;
      this.authInitialized = false;

      try {
        const userData = await this.fetchCurrentUser();
        this.isLoggedIn = !!userData;
      } catch (error) {
        console.error('Auth check failed:', error);
        this.isLoggedIn = false;
      } finally {
        this.isAuthenticating = false;
        this.authInitialized = true;
      }
    },

    // This is being called from HeaderComponent
    loadUserFromLocalStorage() {
      try {
        const userData = localStorage.getItem('userData');
        if (userData) {
          this.user = JSON.parse(userData);
          this.isLoggedIn = true;
        }
      } catch (error) {
        console.error('Failed to load user from localStorage', error);
      }
    },

    async logout() {
      try {
        const success = await authService.logoutAPI();

        if (success) {
          // Clear auth store
          this.clearUser();

          // Clear user store
          const userStore = useUserStore();
          userStore.clearUserInfo();

          // Clear storage
          localStorage.removeItem('userData');
          sessionStorage.removeItem('userData');

          // Notify other tabs
          window.dispatchEvent(new Event('storage'));

          // Navigate to home page
          router.push('/');
        }
      } catch (error) {
        console.error('Logout failed:', error);
        this.error = 'Failed to log out';
      }
    },

    async loginWithGoogle() {
      return authService.loginWithGoogle();
    },

    async loginWithGithub() {
      return authService.loginWithGithub();
    }
  },

  persist: {
    key: 'auth',
    storage: localStorage,
    paths: ['user', 'isLoggedIn']
  }
});
