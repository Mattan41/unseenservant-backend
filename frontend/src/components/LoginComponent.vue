<script setup>
import {useAuthStore} from '@/features/auth/authStore.js';

const authStore = useAuthStore();

const handleGoogleLogin = async () => {
  try {
    await authStore.loginWithGoogle();
  } catch (error) {
    console.error('Google login failed', error);
  }
};

const handleGithubLogin = async () => {
  try {
    await authStore.loginWithGithub();
  } catch (error) {
    console.error('GitHub login failed', error);
  }
};

</script>

<template>
  <div class="login max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6">Login</h2>
    <form>
      <p class="text-center mt-4">
        Sign in with Google <a @click.prevent="handleGoogleLogin"
                               class="text-blue-500 cursor-pointer">here</a>
      </p>
      <p class="text-center mt-4">
        Sign in with GitHub <a @click.prevent="handleGithubLogin"
                               class="text-blue-500 cursor-pointer">here</a>
      </p>
    </form>
  </div>
</template>

<style scoped>
</style>
