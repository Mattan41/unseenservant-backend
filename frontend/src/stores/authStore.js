import {defineStore} from 'pinia';
import AuthService from '../AuthService';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null
  }),
  actions: {
    async checkAuth() {
      try {
        const user = await AuthService.getCurrentUser();
        this.user = user;
        return user;
      } catch (error) {
        this.user = null;
        return null;
      }
    },
    async logout() {
      await AuthService.logout();
    },

    async login(user) {
      const response = await AuthService.login(user);
      if (response.data) {
        this.user = response.data;
        localStorage.setItem('userData', JSON.stringify(this.user));
        window.dispatchEvent(new Event('storage'));
      }
    },
    async loginWithGoogle(idToken) {
      const response = await AuthService.loginWithGoogle(idToken);
      if (response.data) {
        this.user = response.data;
        localStorage.setItem('userData', JSON.stringify(this.user));
        window.dispatchEvent(new Event('storage'));
      }
    },

    loadUserFromLocalStorage() {
      const userData = JSON.parse(localStorage.getItem('userData'));
      if (userData) {
        this.user = userData;
        this.token = userData.token;
      }
    },
  },
  getters: {
    isLoggedIn: (state) => !!state.user,
    getUser: (state) => state.user,
  },
});
