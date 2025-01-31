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
  <header class="bg-gray-800 text-white">
    <nav>
      <div class="nav-container">
        <RouterLink to="/" class="nav-link">
          <h5 class="nav-text">Home</h5>
        </RouterLink>
        <RouterLink to="/about" class="nav-link">
          <h5 class="nav-text">About</h5>
        </RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" to="/logout">
          <div
            class="flex items-center justify-center bg-secondary bg-opacity-70 px-4 rounded-full">
            <h5 class="block px-3 py-2 hover:text-gray-300 uppercase">Logout</h5>
          </div>
        </RouterLink>
        <RouterLink v-if="!authStore.isLoggedIn" to="/login">
          <div
            class="flex items-center justify-center bg-secondary bg-opacity-70 px-4 rounded-full mx-2">
            <h5 class="block px-4 py-2 hover:text-gray-300 uppercase">Login</h5>
          </div>
        </RouterLink>
      </div>
    </nav>
  </header>
</template>

<style scoped>
.nav-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-link {
  display: flex;
  align-items: center;
  background-color: #6b7280; /* Example secondary color */
  padding: 0.5rem 1rem;
  border-radius: 9999px;
  margin: 0 0.5rem;
  text-decoration: none;
  color: inherit;
}

.nav-text {
  padding: 0.5rem 1rem;
  text-transform: uppercase;
  color: #d1d5db; /* Example hover color */
}

.nav-link:hover .nav-text {
  color: #9ca3af; /* Example hover color */
}
</style>
