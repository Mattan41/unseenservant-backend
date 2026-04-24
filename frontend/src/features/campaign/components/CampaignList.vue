<script setup>
import {onMounted, ref} from 'vue';
import {useCampaignStore} from '@/features/campaign/campaignStore.js';
import CreateCampaign from '@/features/campaign/components/CreateCampaign.vue';
import CampaignImage from "@/features/campaign/components/CampaignImage.vue";

const campaignStore = useCampaignStore();
const expandedDescriptions = ref({});

const toggleDescription = (campaignId) => {
  expandedDescriptions.value[campaignId] = !expandedDescriptions.value[campaignId];
}

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
          class="mb-4 p-4 bg-primary-100 rounded">

        <RouterLink :to="{ name: 'CampaignView', params: { id: campaign.id } }"
                    class="element-link">
          <h3 class="text-lg font-bold break-words">{{ campaignStore.getCampaignTitle(campaign.id) }}</h3>

          <CampaignImage
            :src="campaignStore.getCampaignImageUrl(campaign.id)"
            :alt="campaignStore.getCampaignTitle(campaign.id)"
            class="w-full h-48 object-cover rounded"
          />

          <!-- Description with read more/less -->
          <div class="break-words">
            <p
              v-if="campaignStore.getCampaignDescription(campaign.id).length <= 150 || expandedDescriptions[campaign.id]">
              {{ campaignStore.getCampaignDescription(campaign.id) }}
            </p>
            <p v-else>
              {{ campaignStore.getCampaignDescription(campaign.id).substring(0, 150) }}...
            </p>
            <button
              v-if="campaignStore.getCampaignDescription(campaign.id).length > 150"
              @click.prevent="toggleDescription(campaign.id)"
              class="text-primary-600 hover:text-primary-800 text-sm mt-1"
            >
              {{ expandedDescriptions[campaign.id] ? 'Show Less' : 'Read More' }}
            </button>
          </div>

          <ul class="mt-2">
            <li v-for="participant in campaign.participants" :key="participant.id">
              {{ participant.nickname }} ( {{ participant.role }} )
            </li>
          </ul>
        </RouterLink>

      </li>
    </ul>
  </div>
</template>


<style scoped>
</style>
