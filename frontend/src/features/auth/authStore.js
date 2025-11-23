import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import AuthService from './AuthService.js'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(null)
  const authStatus = ref('idle')
  const error = ref(null)
  const isInitializing = ref(false)
  const isAuthChecked = ref(false)

  // Computed
  const isAuthenticated = computed(() => authStatus.value === 'authenticated')
  const isLoading = computed(() => authStatus.value === 'loading')

  // Actions
  async function initializeAuth() {
    if (isAuthChecked.value) {
      return
    }

    if (isInitializing.value) {
      console.warn('Already initializing, skipping...')
      return
    }

    isInitializing.value = true
    authStatus.value = 'loading'

    try {
      const userData = await AuthService.getCurrentUser()
      if (userData) {
        authStatus.value = 'authenticated'
      } else {
        authStatus.value = 'unauthenticated'
      }
    } catch (err) {
      console.error('Auth initialization failed:', err)
      authStatus.value = 'unauthenticated'
      error.value = err.message
    } finally {
      isAuthChecked.value = true
      isInitializing.value = false
    }
  }

  async function loginWithGoogle() {
    error.value = null
    return AuthService.loginWithGoogle()
  }

  async function loginWithGithub() {
    error.value = null
    return AuthService.loginWithGithub()
  }

  async function logout() {
    authStatus.value = 'loading'

    try {
      await AuthService.logoutAPI()
    } catch (err) {
      console.error('Logout API call failed:', err)
    } finally {
      clearAuth()
    }
  }

  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('auth_token', newToken)
    } else {
      localStorage.removeItem('auth_token')
    }
  }

  function clearAuth() {
    setToken(null)
    authStatus.value = 'unauthenticated'
    error.value = null
  }

  return {
    // State
    token,
    authStatus,
    error,
    isAuthChecked,
    // Computed
    isAuthenticated,
    isLoading,
    // Actions
    initializeAuth,
    loginWithGoogle,
    loginWithGithub,
    logout,
    setToken,
    clearAuth,
  }
})
