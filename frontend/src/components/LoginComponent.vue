<script setup>
import {ref} from 'vue';
import {useAuthStore} from '@/stores/authStore';

const username = ref('');
const password = ref('');
const errors = ref({});
const authStore = useAuthStore();

const validateForm = () => {
  errors.value = {};

  if (!username.value) {
    errors.value.username = 'Username is required';
  }

  if (!password.value) {
    errors.value.password = 'Password is required';
  }

  return Object.keys(errors.value).length === 0;
};

const handleSubmit = async () => {
  if (validateForm()) {
    try {
      await authStore.login({username: username.value, password: password.value});
      console.log('Login successful');
    } catch (error) {
      console.error('Login failed', error);
    }
  }
};
</script>

<template>
  <div class="login max-w-md mx-auto mt-10 p-6 bg-white rounded-lg shadow-md">
    <h2 class="text-2xl font-bold mb-6">Login</h2>
    <form @submit.prevent="handleSubmit">
      <div class="mb-4">
        <label for="username" class="block text-gray-700">Username</label>
        <input
          id="username"
          v-model="username"
          type="text"
          class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring focus:ring-opacity-50"
        />
        <p v-if="errors.username" class="text-red-500 text-sm mt-1">{{ errors.username }}</p>
      </div>
      <div class="mb-4">
        <label for="password" class="block text-gray-700">Password</label>
        <input
          id="password"
          v-model="password"
          type="password"
          class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm focus:ring focus:ring-opacity-50"
        />
        <p v-if="errors.password" class="text-red-500 text-sm mt-1">{{ errors.password }}</p>
      </div>
      <button
        type="submit"
        class="w-full bg-blue-500 text-white py-2 rounded-md hover:bg-blue-600"
      >
        Login
      </button>
    </form>
  </div>
</template>

<style scoped>
</style>
