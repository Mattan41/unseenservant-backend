import {defineStore} from 'pinia';
import AuthService from '../services/AuthService.js';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
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
      try {
        const userData = await AuthService.login(user); // AuthService returnerar direkt data
        this.user = userData;
        localStorage.setItem('userData', JSON.stringify(this.user));
        window.dispatchEvent(new Event('storage')); // Synkronisera event
      } catch (error) {
        console.error("Login failed in authStore:", error);
        throw error; // Bubblar upp felet till `handleSubmit`
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
      }
    },
  },
  getters: {
    isLoggedIn: (state) => !!state.user,
    getUser: (state) => state.user,
  },
});
