<script setup>
import { useAuthStore } from '@/features/auth/authStore.js';

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
    <div class="flex flex-col items-center gap-4">
      <!-- Google Sign-in Button -->
      <button
        @click.prevent="handleGoogleLogin"
        class="w-64 h-10 px-3 flex items-center justify-center bg-white border border-gray-300 rounded text-sm text-gray-800 font-medium relative transition duration-200 hover:shadow-md hover:border-blue-100 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
      >
        <div class="flex items-center justify-center">
          <div class="w-5 h-5 mr-3 flex-shrink-0">
            <img src="@/assets/google.svg" alt="Google logo" class="w-full h-full" />
          </div>
          <span class="font-medium">Sign in with Google</span>
        </div>
      </button>

      <!-- GitHub Sign-in Button -->
      <button
        @click.prevent="handleGithubLogin"
        class="w-64 h-10 px-3 flex items-center justify-center bg-gray-900 rounded text-sm text-white font-medium relative transition duration-200 hover:bg-gray-800 hover:shadow-md focus:outline-none focus:ring-2 focus:ring-gray-600"
      >
        <div class="flex items-center justify-center">
          <div class="w-5 h-5 mr-3 flex-shrink-0">
            <img src="@/assets/github.svg" alt="GitHub logo" class="w-full h-full filter invert" />
          </div>
          <span class="font-medium">Sign in with GitHub</span>
        </div>
      </button>
    </div>
  </div>
</template>

<style scoped>
/* Minimal custom styles that can't be achieved with Tailwind alone */
</style>
