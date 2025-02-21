<script setup>
import {computed, onMounted} from 'vue';
import {useAuthStore,} from '../stores/authStore';
import {useUserStore} from "@/stores/userStore.js";
import Hello from './Hello.vue';

const authStore = useAuthStore();
const userStore = useUserStore();

onMounted(async () => {
  authStore.loadUserFromLocalStorage();

  if (authStore.isLoggedIn) {
    //await userStore.fetchUserInfo(authStore.getUser.id);
    await userStore.fetchCurrentUser();
  }

});

const username = computed(() => userStore.getDisplayName || 'Guest');
</script>

<template>
  <main>
    <section class="text-center">
      <h1 class="text-4xl font-bold">Welcome to Unseen Servant</h1>
      <p class="text-lg">A service that does not get in your way</p>
      <Hello v-if="username" :msg="username"/>
    </section>
  </main>
</template>

<style scoped>
</style>
