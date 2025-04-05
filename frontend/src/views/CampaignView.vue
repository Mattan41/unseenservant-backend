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
const successMessage = ref('');
const descriptionExpanded = ref(false);

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

// Listen for the participants-updated event
const handleParticipantsUpdated = (message) => {
  successMessage.value = message || 'Update successful!';
  loadCampaignData();

  // clear the message after 3 seconds
  setTimeout(() => {
    successMessage.value = '';
  }, 3000);
};
</script>

<template>
  <!-- Loading state -->
  <div v-if="isLoading" class="flex flex-col items-center justify-center h-full p-8">
    <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
    <p class="mt-2">Loading campaign...</p>
  </div>

  <!-- Error state -->
  <div v-else-if="errorMessage" class="error-message p-4 rounded">
    <p>{{ errorMessage }}</p>
  </div>

  <!-- Campaign loaded successfully -->
  <div v-else-if="campaign" class="flex h-full">
    <!-- Campaign selector sidebar - same for all screen sizes -->
    <aside class="w-16 flex flex-col items-center py-4 space-y-4 h-full overflow-y-auto">
      <div class="flex-1 flex flex-col items-center space-y-4">
        <RouterLink
          v-for="userCampaign in campaignStore.campaigns"
          :key="userCampaign.id"
          :to="{ name: 'CampaignView', params: { id: userCampaign.id } }"
          class="w-10 h-10 bg-third-400 rounded-md flex items-center justify-center text-xs text-white font-medium overflow-hidden relative group no-underline"
          :class="{ 'ring ring-primary-500': userCampaign.id === campaign.id }"
        >
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
    </aside>

    <!-- Main content area -->
    <div class="flex-1 p-4 overflow-y-auto">
      <!-- Campaign header -->
      <div class="mb-4">
        <h2 class="text-xl sm:text-2xl font-bold">{{ campaign.name || 'Unseen Servant' }}</h2>

        <!-- Campaign description with line clamp -->
        <div class="mt-2">
          <p v-if="!campaign.description" class="italic text-gray-500 text-sm">
            No description available.
          </p>

          <template v-else>
            <p
              :class="{ 'line-clamp-2': !descriptionExpanded }"
              class="text-sm text-gray-700"
            >
              {{ campaign.description }}
            </p>

            <button
              v-if="campaign.description && campaign.description.length > 60"
              @click="descriptionExpanded = !descriptionExpanded"
              class="text-xs text-primary-500 mt-1 hover:underline"
            >
              {{ descriptionExpanded ? 'Show less' : 'Read more' }}
            </button>
          </template>
        </div>
      </div>

      <!-- Campaign content -->
      <section class="mb-6">
        <!-- Group chat section -->
        <h4 class="text-xl font-bold font-serif mb-2">Group Chat</h4>

        <!-- Participants collapsible section -->
        <div class="mb-4 border rounded p-3">
          <h3 class="font-medium cursor-pointer flex items-center" @click="toggleCharactersList">
            <span v-if="isCharactersListVisible"
                  class="transform rotate-90 inline-block mr-1">›</span>
            <span v-else class="inline-block mr-1">›</span>
            Participants
          </h3>

          <ul v-if="isCharactersListVisible" class="mt-2 space-y-1">
            <li
              v-for="(participant, index) in campaign?.participants || []"
              :key="index"
              class="pl-4 py-1 border-l-2 border-primary-200"
            >
              • {{ participant.nickname }}, {{ participant.role }}
            </li>
          </ul>
        </div>

        <!-- Action Buttons -->
        <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-5">
          <button class="button button-primary" @click="toggleSettings">
            Campaign Settings
          </button>
          <button class="button button-primary">
            IMPORT CHARACTER
          </button>
        </div>

        <!-- Success message from Settings panel -->
        <div v-if="successMessage"
             class="success-message mt-4 p-3 bg-green-100 border border-green-500 text-green-700 rounded">
          {{ successMessage }}
        </div>
        <!-- Settings panel -->
        <div v-if="showSettings" class="mt-4">
          <CampaignSettings :campaignId="String(campaign.id)"
                            @participants-updated="handleParticipantsUpdated"/>
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
