<script setup>
import {onMounted} from 'vue';
import {useCampaignStore} from '@/stores/campaignStore';
import CreateCampaign from '@/components/CreateCampaign.vue';

const campaignStore = useCampaignStore();

onMounted(async () => {
  await campaignStore.fetchAllCampaignsForCurrentUser();
});
</script>

<template>
  <div>
    <h2 class="mb-4 flex flex-col sm:flex-row justify-between items-start sm:items-center">
      Campaigns
      <CreateCampaign/>
    </h2>
    <div v-if="campaignStore.isLoading" class="text-center p-8">
      <div
        class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
      <p class="mt-2">Loading campaigns...</p>
    </div>
    <div v-else-if="campaignStore.error"
         class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
      <p>{{ campaignStore.error }}</p>
    </div>
    <ul v-else>
      <li v-for="campaign in campaignStore.campaigns" :key="campaign.id"
          class="mb-4 p-4 bg-gray-100 rounded">
        <h3 class="text-lg font-bold">{{ campaign.name }}</h3>
        <p>{{ campaign.description }}</p>
        <ul class="mt-2">
          <li v-for="participant in campaign.participants" :key="participant.id">
            {{ participant.nickname }} ( {{ participant.role }} )
          </li>
        </ul>
      </li>
    </ul>
  </div>
</template>
