import AuthService from '@/features/auth/AuthService.js'
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('auth_token'))
  const authStatus = ref(token.value ? 'authenticated' : 'idle')
  const isInitializing = ref(false)
  const isAuthChecked = ref(false)

  const isAuthenticated = computed(() => authStatus.value === 'authenticated')

  function setToken(newToken) {
    token.value = newToken
    if (newToken) {
      localStorage.setItem('auth_token', newToken)
      authStatus.value = 'authenticated'
    } else {
      localStorage.removeItem('auth_token')
      authStatus.value = 'unauthenticated'
    }
  }

  async function initializeAuth() {
    console.log('Starting auth check...')
    if (isAuthChecked.value || isInitializing.value) return

    if (!token.value) {
      console.log('No token found, skipping...')
      authStatus.value = 'unauthenticated'
      isAuthChecked.value = true
      return
    }

    isInitializing.value = true
    try {
      console.log('Fetching user from backend...')
      const userData = await AuthService.getCurrentUser()
      authStatus.value = userData ? 'authenticated' : 'unauthenticated'
    } catch (err) {
      console.error('Auth verification failed:', err)
      clearAuth()
    } finally {
      isAuthChecked.value = true
      isInitializing.value = false
      console.log('Auth check complete. isReady should now be true.')
    }
  }

  function handleOAuthRedirect(tokenFromUrl) {
    setToken(tokenFromUrl)
  }

  function clearAuth() {
    setToken(null)
    localStorage.removeItem('userData')
    isAuthChecked.value = true
  }

  async function logout() {
    clearAuth()
  }

  async function loginWithGoogle() {
    return AuthService.loginWithGoogle()
  }

  async function loginWithGithub() {
    return AuthService.loginWithGithub()
  }
  return {
    // State & Computed
    token,
    authStatus,
    isAuthChecked,
    isAuthenticated,

    // Actions
    initializeAuth,
    loginWithGoogle,
    loginWithGithub,
    handleOAuthRedirect,
    logout,
    clearAuth,
    setToken,
  }
})
