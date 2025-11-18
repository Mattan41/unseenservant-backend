// features/auth/authStore.js
import { defineStore } from 'pinia'
import AuthService from './AuthService.js'
import { useUserStore } from '@/features/user/userStore.js'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    isLoggedIn: false,
    isLoggingOut: false,
    isAuthenticating: false,
    authInitialized: false,
    error: null,
    // Prepare for JWT-based auth; token is optional for current cookie-based flow
    token: null,
  }),

  actions: {
    clearUser() {
      this.user = null
      this.isLoggedIn = false
      this.isLoggingOut = false
      this.error = null
      this.token = null
    },

    async fetchCurrentUser() {
      try {
        const userData = await AuthService.getCurrentUser()
        if (userData) {
          this.user = userData
          this.isLoggedIn = true
          return userData
        } else {
          this.clearUser()
          return null
        }
      } catch (error) {
        console.error('Error fetching user in store:', error)
        this.error = 'Failed to fetch user data'
        this.clearUser()
        return null
      }
    },

    // This matches what  App.vue is calling
    async checkAuth() {
      this.isAuthenticating = true
      this.authInitialized = false

      try {
        const userData = await this.fetchCurrentUser()
        this.isLoggedIn = !!userData
      } catch (error) {
        console.error('Auth check failed:', error)
        this.isLoggedIn = false
      } finally {
        this.isAuthenticating = false
        this.authInitialized = true
      }
    },

    async logout() {
      if (this.isLoggingOut) return
      this.isLoggingOut = true
      try {
        await AuthService.logoutAPI()
      } catch (error) {
        console.error('Logout failed:', error)
      } finally {
        this.clearUser()

        const userStore = useUserStore()
        userStore.clearUserInfo()

        localStorage.removeItem('auth')
        localStorage.removeItem('userData')
        sessionStorage.removeItem('userData')
        localStorage.removeItem('token')

        window.dispatchEvent(new Event('storage'))
      }
    },

    async loginWithGoogle() {
      return AuthService.loginWithGoogle()
    },

    async loginWithGithub() {
      return AuthService.loginWithGithub()
    },

    // Set token for future JWT-based flow (e.g., after exchange)
    setToken(token) {
      this.token = token
      if (token) {
        localStorage.setItem('token', token)
      } else {
        localStorage.removeItem('token')
      }
    },
  },

  persist: {
    key: 'auth',
    storage: localStorage,
    // Persist token to allow attaching Authorization header on app reloads
    paths: ['user', 'isLoggedIn', 'token'],
  },
})
