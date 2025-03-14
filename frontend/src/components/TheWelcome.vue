<script setup>
import {computed} from 'vue';
import {useUserStore} from "@/stores/userStore.js";
import Hello from './Hello.vue';

const userStore = useUserStore();
const username = computed(() => userStore.getDisplayName);

</script>

<template>
  <main>
    <section class="text-center">
      <h1 class="text-4xl font-bold">Unseen Servant</h1>
      <p class="text-lg">A service that does not get in your way</p>
      <Hello v-if="username" :msg="username"/>
    </section>
  </main>
</template>

<style scoped>
</style>
