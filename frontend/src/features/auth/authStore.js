import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import AuthService from './AuthService.js'

export const useAuthStore = defineStore('auth', () => {
  // State
  const token = ref(null)
  const authStatus = ref('idle')
  const error = ref(null)
  const isInitializing = ref(false)

  // Computed
  const isAuthenticated = computed(() => authStatus.value === 'authenticated')
  const isLoading = computed(() => authStatus.value === 'loading')
  const isInitialized = computed(() => authStatus.value !== 'idle')

  // Actions
  async function initializeAuth() {
    // Prevent multiple simultaneous initializations
    if (isInitializing.value) {
      console.log('Already initializing, skipping...')
      return
    }

    if (authStatus.value !== 'idle') {
      console.log('Already initialized:', authStatus.value)
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
    // Computed
    isAuthenticated,
    isLoading,
    isInitialized,
    // Actions
    initializeAuth,
    loginWithGoogle,
    loginWithGithub,
    logout,
    setToken,
    clearAuth,
  }
})
