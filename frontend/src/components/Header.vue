<script setup>
import {useAuthStore} from '../stores/authStore';
import {RouterLink} from 'vue-router';
import {onMounted} from 'vue';

const authStore = useAuthStore();

onMounted(() => {
  authStore.loadUserFromLocalStorage();
});
</script>

<template>
  <header class="bg-gradient-to-r from-primary-50 to-primary-300 p-4 text-center">
    <nav>
      <div class="flex justify-between items-center">
        <RouterLink to="/"
                    class="flex items-center justify-center bg-primary-500 bg-opacity-70 px-4 rounded-full mx-2">
          <h5 class="p-2 uppercase">Home</h5>
        </RouterLink>
        <RouterLink to="/about"
                    class="flex items-center justify-center bg-primary-500 bg-opacity-70 px-4 rounded-full mx-2">
          <h5 class="p-2 uppercase">About</h5>
        </RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" to="/logout">
          <div
            class="flex items-center justify-center bg-primary-500 bg-opacity-70 px-4 rounded-full mx-2">
            <h5 class="block px-3 py-2 hover:text-primary-300 uppercase">Logout</h5>
          </div>
        </RouterLink>
        <RouterLink v-if="!authStore.isLoggedIn" to="/login">
          <div
            class="flex items-center justify-center bg-primary-500 bg-opacity-70 px-4 rounded-full mx-2">
            <h5 class="block px-3 py-2 hover:text-primary-300 uppercase">Login</h5>
          </div>
        </RouterLink>
      </div>
    </nav>
  </header>
</template>

<style scoped>
</style>
