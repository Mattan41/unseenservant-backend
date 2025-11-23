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
    // TODO: JWT IMPLEMENTATION. When switching backend to JWT, uncomment this block to capture the token from the URL query parameter and store it in Pinia/localStorage.
    // const routeToken = router.currentRoute.value.query.token
    // if (routeToken) {
    //   authStore.setToken(routeToken)
    // }
    // --- END JWT IMPLEMENTATION ---

    // --- Session Cookie Logic ---
    await authStore.initializeAuth()

    if (authStore.isAuthenticated) {
      // If authentication was successful based on the new cookie
      await userStore.fetchCurrentUser()
      notificationStore.addNotification('Successfully logged in!', 'success')
      await router.push({ name: 'home' })
    } else {
      // If initializeAuth failed (e.g., server error or cookie rejection)
      notificationStore.addNotification('Authentication failed. Please try again.', 'error')
      await router.push({ name: 'login' })
    }
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
