<script setup>
import { RouterView } from 'vue-router'
import { watch } from 'vue'
import HeaderComponent from './components/HeaderComponent.vue'
import FooterComponent from './components/FooterComponent.vue'
import NotificationComponent from '@/components/NotificationComponent.vue'
import { useAuthStore } from '@/features/auth/authStore.js'
import { useUserStore } from '@/features/user/userStore.js'

const authStore = useAuthStore()
const userStore = useUserStore()

watch(
  () => authStore.isAuthenticated,
  async (isAuth) => {
    if (isAuth) {
      await userStore.fetchCurrentUser()
    } else {
      userStore.clearUser()
    }
  },
  { immediate: true },
)
</script>

<template>
  <div
    v-if="!authStore.isAuthChecked"
    class="flex flex-col items-center justify-center h-screen bg-primary-100"
  >
    <div
      class="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin"
    ></div>
    <p class="mt-4 text-primary-800 font-medium">Loading...</p>
  </div>

  <div v-else class="flex flex-col min-h-screen overflow-x-hidden">
    <NotificationComponent />
    <HeaderComponent />
    <main class="flex-grow bg-gradient-to-b from-primary-100 via-primary-300 to-primary-100">
      <RouterView />
    </main>
    <FooterComponent />
  </div>
</template>
