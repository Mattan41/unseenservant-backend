<script setup>
import { RouterView } from 'vue-router'
import { computed, watch } from 'vue'
import HeaderComponent from './components/HeaderComponent.vue'
import FooterComponent from './components/FooterComponent.vue'
import NotificationComponent from '@/components/NotificationComponent.vue'
import { useAuthStore } from '@/features/auth/authStore.js'
import { useUserStore } from '@/features/user/userStore.js'

const authStore = useAuthStore()
const userStore = useUserStore()
const isReady = computed(() => authStore.isAuthChecked)

watch(
  () => authStore.isAuthenticated,
  async (isAuth) => {
    if (isAuth) {
      await userStore.fetchCurrentUser()
    } else {
      userStore.clearUser()
    }
  },
)
</script>

<template>
  <div v-if="!isReady" class="flex items-center justify-center h-screen">
    <div class="loader">Loading...</div>
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
