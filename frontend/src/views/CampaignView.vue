<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import {useCampaignStore} from '@/stores/campaignStore';
import CampaignSettings from "@/components/CampaignSettings.vue";

const route = useRoute();
const campaignStore = useCampaignStore();
const campaign = ref(null);
const isLoading = ref(true);
const errorMessage = ref('');

const loadCampaignData = async () => {
  isLoading.value = true;
  errorMessage.value = '';

  try {
    // Load all campaigns for the current user
    await campaignStore.fetchAllCampaignsForCurrentUser();

  } catch (error) {
    errorMessage.value = 'Could not retrieve campaigns: ' + error.message;
    isLoading.value = false;
    return;
  }

  // Load the campaign with the given id
  const campaignId = route.params.id;

  try {
    campaign.value = await campaignStore.fetchCampaign(campaignId);
  } catch (error) {
    errorMessage.value = error.message + 'Failed to load campaign';
  } finally {
    isLoading.value = false;
  }
};

// show settings for the campaign with the id from the route

const showSettings = ref(false);

const toggleSettings = () => {
  showSettings.value = !showSettings.value;
};

onMounted(loadCampaignData);

watch(() => route.params.id, (newId) => {
  if (newId) loadCampaignData();
});

const isCharactersListVisible = ref(false);

const toggleCharactersList = () => {
  isCharactersListVisible.value = !isCharactersListVisible.value;
};
</script>

<template>
  <div v-if="isLoading" class="flex flex-col items-center justify-center h-full p-8">
    <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
    <p class="mt-2">Loading campaign...</p>
  </div>

  <div v-else-if="errorMessage" class="error-message">
    <p>{{ errorMessage }}</p>
  </div>

  <div v-else-if="campaign" class="flex h-full">
    <!-- Left sidebar. For now text placeholders instead of images from the campaign and titles when hoovering  -->
    <aside
      class="w-16 bg-primary-300 flex flex-col items-center py-4 space-y-4 h-full overflow-y-auto">
      <div class="flex-1 flex flex-col justify-between">
        <div class="flex-1 flex flex-col items-center space-y-4">
          <RouterLink
            v-for="userCampaign in campaignStore.campaigns"
            :key="userCampaign.id"
            :to="{ name: 'CampaignView', params: { id: userCampaign.id } }"
            class="w-10 h-10 bg-third-400 rounded-md flex items-center justify-center text-xs text-white font-medium overflow-hidden relative group no-underline"
            :class="{ 'ring ring-primary-500': userCampaign.id === campaign.id }"
          >
            <!-- Generic icon or the first letter in the name of the campaign -->
            <span v-if="userCampaign.name" class="uppercase">
            {{ userCampaign.name.charAt(0) }}
          </span>
            <span v-else>C</span>
            <div
              class="absolute left-full ml-2 px-2 py-1 bg-gray-800 text-white text-xs rounded whitespace-nowrap opacity-0 group-hover:opacity-100 transition-opacity z-10">
              {{ userCampaign.name }}
            </div>
          </RouterLink>
        </div>
      </div>
    </aside>

    <!-- main content area -->
    <div class="flex-1 p-4 overflow-y-auto">
      <div class="flex justify-between items-center mb-4">
        <h2>{{ campaign.name || 'Unseen Servant' }}</h2>
      </div>
      <section class="mb-6">
        <!-- replace h4 with link to the MessageboardView -->
        <h4 class="text-xl font-bold font-serif mb-2">Group Chat</h4>

        <div class="mb-3">
          <h3 class="font-medium cursor-pointer" @click="toggleCharactersList">› Participants</h3>
          <ul v-if="isCharactersListVisible" class="ml-2 mt-1">
            <li
              v-for="(participant, index) in campaign?.participants || [{nickname: 'Gusten'}, {nickname: 'Tengi'}]"
              :key="index"
              class="mb-1">
              • {{ participant.nickname }}, {{ participant.role }}
            </li>
          </ul>
        </div>

        <div class="space-y-3 mt-5">
          <button class="button button-primary" @click="toggleSettings">
            Campaign Settings
          </button>
          <button class="button button-primary">
            IMPORT CHARACTER
          </button>
        </div>
        <div class="flex items-center space-x-2">
          <div v-if="showSettings">
            <CampaignSettings v-if="showSettings" :campaignId="String(campaign.id)"/>
          </div>
        </div>

      </section>
    </div>
  </div>
</template>

<style scoped>

aside {
  min-height: calc(100vh - 4rem); /* secure full height */
}


</style>
