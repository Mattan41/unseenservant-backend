<script setup>
import { RouterView } from 'vue-router'
import HeaderComponent from './components/HeaderComponent.vue'
import FooterComponent from './components/FooterComponent.vue'
import { useUserStore } from '@/features/user/userStore.js'
import { useAuthStore } from '@/features/auth/authStore.js'
import { onMounted, ref, watch } from 'vue'
import Notification from '@/components/NotificationComponent.vue'

const authStore = useAuthStore()
const userStore = useUserStore()
const isLoading = ref(true)

onMounted(async () => {
  await authStore.checkAuth()
  if (authStore.isLoggedIn) {
    await userStore.fetchCurrentUserInfo()
  }
  isLoading.value = false
})

watch(
  () => authStore.isLoggedIn,
  async (isLoggedIn) => {
    if (isLoggedIn) {
      await userStore.fetchCurrentUserInfo()
    } else {
      userStore.clearUserInfo()
    }
  },
)
</script>
<template>
  <div v-if="isLoading" class="flex items-center justify-center h-screen">
    <div class="loader">Loading...</div>
  </div>
  <div v-else>
    <div class="flex flex-col min-h-screen overflow-x-hidden">
      <Notification />
      <HeaderComponent />
      <main class="flex-grow bg-gradient-to-b from-primary-100 via-primary-300 to-primary-100">
        <RouterView />
      </main>
      <FooterComponent />
    </div>
  </div>
</template>
