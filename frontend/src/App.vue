<script setup>
import {RouterView} from 'vue-router'
import Header from './components/Header.vue'
import Footer from './components/Footer.vue'
import {useUserStore} from "@/stores/userStore.js";
import {useAuthStore} from "@/stores/authStore.js";
import {onMounted, ref, watch} from "vue";
import router from "@/router/index.js";

const authStore = useAuthStore();
const userStore = useUserStore();
const isLoading = ref(true);

onMounted(async () => {
  window.addEventListener("storage", () => {
    if (!localStorage.getItem("userData")) {
      router.push("/");
    }
  });

  console.log('App mounted. Checking authentication...');

  if (!authStore.authInitialized) {
    await authStore.checkAuth();
  }
  console.log('AuthStore after checkAuth:', authStore.user);

  if (authStore.isLoggedIn) {
    console.log('Fetching user info after authentication.');
    await userStore.fetchCurrentUser();
  } else {
    console.log('No authenticated user found.');
    userStore.clearUserInfo();
  }

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
  <div class="flex flex-col min-h-screen overflow-x-hidden">
    <Header/>
    <main class="flex-grow bg-gradient-to-b from-primary-100 via-primary-300 to-primary-100">
      <RouterView/>
    </main>
    <Footer/>
  </div>
</template>
