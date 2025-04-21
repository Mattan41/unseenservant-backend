<script setup>
import {onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {useAuthStore} from '../features/auth/authStore.js';

const router = useRouter();
const authStore = useAuthStore();

onMounted(async () => {
  await authStore.checkAuth();
  await router.push({name: 'home'});
});
</script>

<template>
  <div>Redirecting...</div>
</template>
