<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../features/auth/authStore.js';
import { useNotificationStore } from '@/stores/notificationStore';

const router = useRouter();
const authStore = useAuthStore();
const notificationStore = useNotificationStore();
const isLoading = ref(true);

onMounted(async () => {
  try {
    console.log('OAuthRedirect: Initializing auth check');
    await authStore.checkAuth();

    if (authStore.isLoggedIn) {
      console.log('OAuthRedirect: Successfully authenticated');
      notificationStore.addNotification('Successfully logged in!', 'success');
      await router.push({ name: 'home' });
    } else {
      console.log('OAuthRedirect: Failed to authenticate');
      notificationStore.addNotification('Authentication failed. Please try again.', 'error');
      await router.push({ name: 'home' });
    }
  } catch (error) {
    console.error('OAuthRedirect: Error during authentication', error);
    notificationStore.addNotification('An error occurred during authentication. Please try again.', 'error');
    await router.push({ name: 'home' });
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen">
    <div v-if="isLoading" class="text-center">
      <div class="mb-4 text-lg font-semibold">Authenticating...</div>
      <div class="w-12 h-12 border-4 border-primary-500 border-t-transparent rounded-full animate-spin mx-auto"></div>
    </div>
  </div>
</template>
