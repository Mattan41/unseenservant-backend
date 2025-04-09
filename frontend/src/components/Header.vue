<script setup>
import {useAuthStore} from '../stores/authStore';
import {RouterLink} from 'vue-router';
import {onBeforeMount, onMounted, ref} from 'vue';

const authStore = useAuthStore();
const mobileMenuOpen = ref(false);

const closeMenu = () => {
  mobileMenuOpen.value = false;
};

const handleClickOutside = (event) => {
  const menu = document.querySelector('.mobile-menu');
  if (menu && !menu.contains(event.target)) {
    closeMenu();
  }
};

onMounted(() => {
  authStore.loadUserFromLocalStorage();
  document.addEventListener('click', handleClickOutside);
});

onBeforeMount(() => {
  document.removeEventListener('click', handleClickOutside);
});


</script>
<template>
  <header class="bg-gradient-to-r from-primary-100 to-primary-300 p-4 text-center">
    <nav>
      <!-- Mobile menu -->
      <div class="flex md:hidden justify-between items-center">
        <RouterLink @click="closeMenu" to="/" class="block">
          <h5 class="uppercase hover:text-primary-300 ">Unseen Servant</h5>
        </RouterLink>
        <button @click="mobileMenuOpen = !mobileMenuOpen"
                class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 py-2 rounded-full">
          <span>Menu</span>
        </button>
      </div>

      <!-- Mobile menu dropdown -->
      <div v-if="mobileMenuOpen"
           class="md:hidden flex flex-col space-y-2 mt-2 transition-all duration-300">
        <RouterLink @click="closeMenu" to="/"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Home</h5>
        </RouterLink>
        <RouterLink @click="closeMenu" to="/about"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">About</h5>
        </RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" @click="closeMenu" to="/user-profile"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">User Profile</h5>
        </RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" @click="closeMenu" to="/campaigns"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Campaigns</h5>
        </RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" @click="closeMenu" to="/logout"
                    class="flex items-center justify-center bg-primary-700 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Logout</h5>
        </RouterLink>
        <RouterLink v-if="!authStore.isLoggedIn" @click="closeMenu" to="/login"
                    class="flex items-center justify-center bg-primary-700 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Login</h5>
        </RouterLink>
      </div>

      <!-- Desktop menu -->
      <div class="hidden md:flex md:flex-wrap justify-center items-center space-x-2">
        <RouterLink @click="closeMenu" to="/"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Home</h5>
        </RouterLink>
        <RouterLink @click="closeMenu" to="/about"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">About</h5>
        </RouterLink>
        <RouterLink @click="closeMenu" v-if="authStore.isLoggedIn" to="/user-profile"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">User Profile</h5>
        </RouterLink>
        <RouterLink v-if="authStore.isLoggedIn" @click="closeMenu" to="/campaigns"
                    class="flex items-center justify-center bg-primary-500 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Campaigns</h5>
        </RouterLink>
        <RouterLink @click="closeMenu" v-if="authStore.isLoggedIn" to="/logout"
                    class="flex items-center justify-center bg-primary-700 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Logout</h5>
        </RouterLink>
        <RouterLink @click="closeMenu" v-if="!authStore.isLoggedIn" to="/login"
                    class="flex items-center justify-center bg-primary-700 hover:bg-primary-600 bg-opacity-70 px-4 rounded-full">
          <h5 class="p-2 uppercase">Login</h5>
        </RouterLink>
      </div>
    </nav>
  </header>
</template>

