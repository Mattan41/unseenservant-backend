<script setup>
import { onMounted } from 'vue'
import { useAuthStore } from '@/features/auth/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const notificationStore = useNotificationStore()
const router = useRouter()

onMounted(async () => {
  try {
    await authStore.logout()
    notificationStore.addNotification('You have been logged out!', 'success')
    router.push({ name: 'home' })
  } catch (error) {
    console.error('Logout failed:', error)
    notificationStore.addNotification('Failed to log out', 'error')
    router.push({ name: 'home' })
  }
})
</script>

<template>
  <div class="p-8 text-center">
    <h2>Logging you out...</h2>
  </div>
</template>
