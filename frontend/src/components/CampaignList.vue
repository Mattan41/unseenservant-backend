<script setup>
import {onMounted, ref} from 'vue';
import {useCampaignStore} from '@/stores/campaignStore';
import CreateCampaign from '@/components/CreateCampaign.vue';

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
          <h3 class="text-lg font-bold">{{ campaign.name }}</h3>

          <!-- image component -->
          <div class="my-3">
            <img v-if="campaign.imageUrl"
                 :src="campaign.imageUrl"
                 :alt="campaign.name"
                 class="w-full h-48 object-cover rounded"/>
            <div v-else class="w-full h-32 bg-gray-200 flex items-center justify-center rounded">
              <p class="text-gray-500 italic">No image has been set for this campaign</p>
            </div>
          </div>

          <!-- Description with read more/less -->
          <div>
            <p v-if="campaign.description.length <= 150 || expandedDescriptions[campaign.id]">
              {{ campaign.description }}
            </p>
            <p v-else>
              {{ campaign.description.substring(0, 150) }}...
            </p>
            <button
              v-if="campaign.description.length > 150"
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
