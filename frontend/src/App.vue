<script setup>
import {RouterView} from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import {useUserStore} from "@/stores/userStore.js";
import {useAuthStore} from "@/stores/authStore.js";
import {onMounted, ref, watch} from "vue";

const authStore = useAuthStore();
const userStore = useUserStore();
// Loading status (for loading UI at startup)
const isLoading = ref(true);

onMounted(async () => {
  console.log('App mounted. Checking authentication...');

  // Sätt igång en autentiseringskontroll
  await authStore.checkAuth();
  console.log('AuthStore after checkAuth:', authStore.user);

  // Om användaren redan är inloggad, hämta deras användardata
  if (authStore.isLoggedIn) {
    console.log('Fetching user info after authentication.');
    await userStore.fetchCurrentUser();
  } else {
    console.log('No authenticated user found.');
    userStore.clearUserInfo();
  }

  // Flagga så att sidan inte längre är i "loading"-läge
  isLoading.value = false;
});

// Watch to observe changes of isAuthenticating, react every change of authentication status
watch(
  () => authStore.isAuthenticating,
  async (isAuthenticating) => {
    if (!isAuthenticating) {
      if (authStore.isLoggedIn) {
        console.log('Auth verification complete. Fetching current user info...');
        await userStore.fetchCurrentUser();
      } else {
        console.log('Auth verification complete. No user logged in.');
        userStore.clearUserInfo();
      }
    }
  },
  {immediate: false} // Kör inte detta direkt, eftersom onMounted redan har hanterat det
);

</script>
<template>
  <div class="flex flex-col min-h-screen">
    <Header/>
    <main class="flex-grow">
      <RouterView/>
    </main>
    <Footer/>
  </div>
</template>
