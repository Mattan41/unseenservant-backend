<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute} from 'vue-router';
import {useCampaignStore} from '@/stores/campaignStore';

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

onMounted(loadCampaignData);

watch(() => route.params.id, (newId) => {
  if (newId) loadCampaignData();
});


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
            class="w-10 h-10 bg-gray-400 rounded-md flex items-center justify-center text-xs text-white font-medium overflow-hidden relative group no-underline"
            :class="{ 'ring ring-primary-500': userCampaign.id === campaign.id }"
          >
            <!-- Generic icon or the first letter in the name of the campaign -->
            <span v-if="userCampaign.name" class="uppercase">
            {{ userCampaign.name.charAt(0) }}
          </span>
            <span v-else>C</span>

            <!-- Tooltip shown on hover -->
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
        <div class="flex items-center space-x-2">
          <span class="text-sm">Campaign settings</span>
          <button class="p-1">
            <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
                 stroke-width="1.5" stroke="currentColor" class="size-6">
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M10.343 3.94c.09-.542.56-.94 1.11-.94h1.093c.55 0 1.02.398 1.11.94l.149.894c.07.424.384.764.78.93.398.164.855.142 1.205-.108l.737-.527a1.125 1.125 0 0 1 1.45.12l.773.774c.39.389.44 1.002.12 1.45l-.527.737c-.25.35-.272.806-.107 1.204.165.397.505.71.93.78l.893.15c.543.09.94.559.94 1.109v1.094c0 .55-.397 1.02-.94 1.11l-.894.149c-.424.07-.764.383-.929.78-.165.398-.143.854.107 1.204l.527.738c.32.447.269 1.06-.12 1.45l-.774.773a1.125 1.125 0 0 1-1.449.12l-.738-.527c-.35-.25-.806-.272-1.203-.107-.398.165-.71.505-.781.929l-.149.894c-.09.542-.56.94-1.11.94h-1.094c-.55 0-1.019-.398-1.11-.94l-.148-.894c-.071-.424-.384-.764-.781-.93-.398-.164-.854-.142-1.204.108l-.738.527c-.447.32-1.06.269-1.45-.12l-.773-.774a1.125 1.125 0 0 1-.12-1.45l.527-.737c.25-.35.272-.806.108-1.204-.165-.397-.506-.71-.93-.78l-.894-.15c-.542-.09-.94-.56-.94-1.109v-1.094c0-.55.398-1.02.94-1.11l.894-.149c.424-.07.765-.383.93-.78.165-.398.143-.854-.108-1.204l-.526-.738a1.125 1.125 0 0 1 .12-1.45l.773-.773a1.125 1.125 0 0 1 1.45-.12l.737.527c.35.25.807.272 1.204.107.397-.165.71-.505.78-.929l.15-.894Z"/>
              <path stroke-linecap="round" stroke-linejoin="round"
                    d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z"/>
            </svg>
          </button>
        </div>
      </div>

      <section class="mb-6">
        <!-- replace h4 with link to the MessageboardView -->
        <h4 class="text-xl font-bold font-serif mb-2">Message board</h4>

        <div class="mb-3">
          <h3 class="font-medium">› Characters</h3>
          <ul class="ml-2 mt-1">
            <li
              v-for="(participant, index) in campaign?.participants || [{nickname: 'Gusten'}, {nickname: 'Tengi'}]"
              :key="index"
              class="mb-1">
              • {{ participant.nickname }}, {{ participant.role }}
            </li>
          </ul>
        </div>

        <div class="space-y-3 mt-5">
          <button class="button button-primary">
            IMPORT CHARACTER
          </button>
          <button class="button button-primary">
            ADD PLAYERS
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>

/* CampaignView.vue styles */
aside {
  min-height: calc(100vh - 4rem); /* Säkerställ full höjd */
}

.group:hover .tooltip {
  @apply opacity-100;
  z-index: 20; /* Säkerställ att tooltips är över innehåll */
}

/* För att ta bort RouterLink's standard textdekorationer */
.no-underline {
  color: inherit;
  text-decoration: none;
}

</style>
