<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../features/auth/authStore.js'
import { useUserStore } from '../features/user/userStore.js'
import { useNotificationStore } from '@/stores/notificationStore'

const router = useRouter()
const authStore = useAuthStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const isLoading = ref(true)

onMounted(async () => {
  try {
    console.log('OAuthRedirect: Starting')
    const routeToken = router.currentRoute.value.query.token
    if (routeToken) {
      authStore.setToken(routeToken)
      authStore.authStatus = 'authenticated'
      await userStore.fetchCurrentUser()
    } else {
      authStore.authStatus = 'idle'
      await authStore.initializeAuth()
    }
    if (authStore.isAuthenticated) {
      notificationStore.addNotification('Successfully logged in!', 'success')
    } else {
      notificationStore.addNotification('Authentication failed. Please try again.', 'error')
    }
    await router.push({ name: authStore.isAuthenticated ? 'home' : 'login' })
  } catch (error) {
    console.error('OAuthRedirect: Error during authentication', error)
    notificationStore.addNotification('An error occurred during authentication.', 'error')
    await router.push({ name: 'login' })
  } finally {
    isLoading.value = false
  }
})
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen">
    <div v-if="isLoading" class="text-center">
      <div class="mb-4 text-lg font-semibold">Authenticating...</div>
      <div
        class="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto"
      ></div>
    </div>
  </div>
</template>
