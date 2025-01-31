<script setup>
import {computed, onMounted} from 'vue';
import {useAuthStore} from '../stores/authStore';

const authStore = useAuthStore();

onMounted(() => {
  authStore.loadUserFromLocalStorage();
});

const username = computed(() => authStore.getUser?.username || '');
</script>

<template>
  <main>
    <section class="text-center">
      <h1 class="text-4xl font-bold">Welcome to UnseenServant</h1>
      <p class="text-lg">A service that does not get in your way</p>
      <p v-if="username" class="text-lg">Welcome, {{ username }}</p>
    </section>
  </main>
</template>

<style scoped>
</style>
