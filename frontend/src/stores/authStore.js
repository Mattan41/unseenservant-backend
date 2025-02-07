import {defineStore} from 'pinia';
import AuthService from '../AuthService';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
  }),
  actions: {
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
    logout() {
      this.user = null;
      localStorage.removeItem('userData');
      window.dispatchEvent(new Event('storage'));
    },
    loadUserFromLocalStorage() {
      const userData = JSON.parse(localStorage.getItem('userData'));
      if (userData) {
        this.user = userData;
      }
    }
  },
  getters: {
    isLoggedIn: (state) => !!state.user,
    getUser: (state) => state.user,
  },
});
