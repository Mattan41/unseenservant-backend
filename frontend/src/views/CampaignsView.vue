<script setup>
import CampaignList from '@/components/CampaignList.vue';
import {computed} from 'vue';
import {useUserStore} from '@/stores/userStore';

const userStore = useUserStore();
const user = computed(() => userStore.userInfo());

</script>

<template>
  <div class="container mx-auto p-6 max-w-4xl">
    <section class="bg-primary-500 p-4 rounded-lg">
      <CampaignList/>
    </section>
  </div>
</template>

<style scoped>
</style>
