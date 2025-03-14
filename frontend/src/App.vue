<script setup>
import {RouterView} from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import {useUserStore} from "@/stores/userStore.js";
import {useAuthStore} from "@/stores/authStore.js";
import {onMounted, ref, watch} from "vue";

const authStore = useAuthStore();
const userStore = useUserStore();
const isLoading = ref(true);

onMounted(async () => {
  console.log('App mounted. Checking authentication...');

  // Start an authentication check
  await authStore.checkAuth();
  console.log('AuthStore after checkAuth:', authStore.user);

  // If the user is authenticated, fetch the user info
  if (authStore.isLoggedIn) {
    console.log('Fetching user info after authentication.');
    await userStore.fetchCurrentUser();
  } else {
    console.log('No authenticated user found.');
    userStore.clearUserInfo();
  }

  // Flag that the app is no longer loading
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
  {immediate: false} // Do not run the watcher immediately, as we already did that in onMounted
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
