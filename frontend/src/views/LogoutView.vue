<script setup>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/features/auth/authStore'
import { useUserStore } from '@/features/user/userStore'
import { useNotificationStore } from '@/stores/notificationStore'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const router = useRouter()
const hasLoggedOut = ref(false)

onMounted(async () => {
  if (hasLoggedOut.value) return
  hasLoggedOut.value = true
  try {
    await authStore.logout()
    userStore.clearUser()
    notificationStore.addNotification('You have been logged out!', 'success')
  } catch (error) {
    console.error('Logout failed:', error)
    notificationStore.addNotification('Failed to log out', 'error')
  }
  router.push({ name: 'home' }).catch((err) => {
    console.log('Navigation cancelled:', err)
  })
})
</script>

<template>
  <div class="p-8 text-center">
    <h2>Logging you out...</h2>
  </div>
</template>
