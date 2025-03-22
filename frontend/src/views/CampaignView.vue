<script setup>
import {onMounted, ref} from 'vue';
import {useRoute} from 'vue-router';
import {useCampaignStore} from '@/stores/campaignStore';

const route = useRoute();
const campaignStore = useCampaignStore();
const campaign = ref(null);
const isLoading = ref(true);
const errorMessage = ref('');

onMounted(async () => {
  try {
    const campaignId = route.params.id;
    campaign.value = await campaignStore.fetchCampaign(campaignId);
  } catch (error) {
    errorMessage.value = error.message || 'Failed to load campaign';
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <div class="container mx-auto p-6 max-w-4xl">
    <div v-if="isLoading" class="text-center p-8">
      <div
        class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
      <p class="mt-2">Loading campaign...</p>
    </div>

    <div v-else-if="errorMessage"
         class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
      <p>{{ errorMessage }}</p>
    </div>

    <div v-else-if="campaign" class="rounded-lg shadow-lg overflow-hidden">
      <div class="bg-primary-500 p-6">
        <h2 class="text-2xl font-bold">{{ campaign.name }}</h2>
        <p>{{ campaign.description }}</p>
      </div>

      <div class="p-6">
        <h3 class="text-xl font-semibold mb-4">Participants</h3>
        <ul>
          <li v-for="participant in campaign.participants" :key="participant.id" class="mb-2">
            {{ participant.nickname }} ({{ participant.role }})
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Add any component-specific styles here */
</style>
