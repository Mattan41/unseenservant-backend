import {defineStore} from 'pinia';
import AuthService from '../services/AuthService.js';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isAuthenticating: false, // new flag to indicate if the user is in process of authentication
  }),
  actions: {

    async fetchCsrfToken() {
      return await AuthService.fetchCsrfToken();
    },

    async checkAuth() {
      this.isAuthenticating = true;
      try {
        const user = await AuthService.getCurrentUser();
        this.user = user;
        return user;
      } catch (error) {
        this.user = null;
        return null;
      } finally {
        this.isAuthenticating = false;
      }
    },
    async logout() {
      await AuthService.logout();
    },
    async loginWithGoogle(idToken) {
      const response = await AuthService.loginWithGoogle(idToken);
      if (response.data) {
        this.user = response.data;
        localStorage.setItem('userData', JSON.stringify(this.user));
        window.dispatchEvent(new Event('storage'));
      }
    },
    async loginWithGithub() {
      try {
        await AuthService.loginWithGithub();
      } catch (error) {
        console.error('GitHub login failed', error);
      }
    },
    loadUserFromLocalStorage() {
      const userData = JSON.parse(localStorage.getItem('userData'));
      if (userData) {
        console.log('Loading user from localStorage:', userData);
        this.user = userData;
      } else {
        console.log('No user data in localStorage, fallback to checkAuth');
        this.user = null;
      }
    },
  },
  getters: {
    isLoggedIn: (state) => !!state.user,
    getUser: (state) => state.user,
  },
});
