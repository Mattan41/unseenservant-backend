import {defineStore} from 'pinia';
import AuthService from '../services/AuthService.js';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isAuthenticating: false, // new flag to indicate if the user is in process of authentication
    authInitialized: false // New flag to track if authentication is done

  }),
  actions: {

    async checkAuth() {
      this.isAuthenticating = true;
      try {
        this.loadUserFromLocalStorage();
        // If no user in localStorage, fetch from API
        if (!this.user) {
          const user = await AuthService.getCurrentUser();
          this.user = user;

          // Save user data to localStorage if it exists
          if (user) {
            localStorage.setItem('userData', JSON.stringify(user));
          }
        }
        return this.user;

      } catch (error) {
        console.error('Error fetching current user:', error);
        this.user = null;
        return null;
      } finally {
        this.isAuthenticating = false;
        this.authInitialized = true;
      }
    },
    loadUserFromLocalStorage() {
      const userData = localStorage.getItem('userData');
      if (userData) {
        try {
          console.log('Loading user from localStorage');
          this.user = JSON.parse(userData);
        } catch (e) {
          console.error('Error parsing user data from localStorage');
          localStorage.removeItem('userData');
          this.user = null;
        }
      } else {
        console.log('No user data in localStorage');
        this.user = null;
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
  },
  getters: {
    isLoggedIn: (state) => !!state.user,
    getUser: (state) => state.user,
  },
});
