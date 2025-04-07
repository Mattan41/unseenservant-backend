<script setup>
import {onMounted, ref, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {useCampaignStore} from '@/stores/campaignStore';
import CampaignSettings from "@/components/CampaignSettings.vue";

const route = useRoute();
const router = useRouter(); // Added for navigation if needed
const campaignStore = useCampaignStore();
const campaign = ref(null);
const isLoading = ref(true);
const errorMessage = ref('');
const successMessage = ref('');
const isCharactersListVisible = ref(false);
const showSettings = ref(false);
const descriptionExpanded = ref(false);

// variables for inline editing
const isEditing = ref(false);
const editedName = ref('');
const editedDescription = ref('');
const isUpdating = ref(false);


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

// Start editing function
const startEditing = () => {
  editedName.value = campaign.value.name || '';
  editedDescription.value = campaign.value.description || '';
  isEditing.value = true;
};

// Cancel editing function
const cancelEditing = () => {
  isEditing.value = false;
};

// Save changes function
const saveChanges = async () => {
  isUpdating.value = true;

  try {
    await campaignStore.updateCampaignInfo(campaign.value.id, {
      name: editedName.value,
      description: editedDescription.value
    });

    // Update local campaign object with edited values
    campaign.value.name = editedName.value;
    campaign.value.description = editedDescription.value;

    isEditing.value = false;

    // Optional: Show temporary success message
    successMessage.value = 'Campaign updated successfully!';
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (error) {
    console.error('Failed to update campaign:', error);
  } finally {
    isUpdating.value = false;
  }
};

const toggleCharactersList = () => {
  isCharactersListVisible.value = !isCharactersListVisible.value;
};

const toggleSettings = () => {
  showSettings.value = !showSettings.value;
};

// Handle participants updated
const handleParticipantsUpdated = (message) => {
  loadCampaignData(); // Reload full campaign data
  successMessage.value = message || 'Update successful!';

  // clear the message after 3 seconds
  setTimeout(() => {
    successMessage.value = '';
  }, 3000);
};

// Toggle description expanded state
const toggleDescription = () => {
  descriptionExpanded.value = !descriptionExpanded.value;
};

onMounted(loadCampaignData);

// Critical: Watch for route parameter changes to reload data
watch(() => route.params.id, (newId) => {
  if (newId) loadCampaignData();
});

</script>

<template>
  <!-- Loading state -->
  <div v-if="isLoading" class="flex flex-col items-center justify-center h-full p-8">
    <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
    <p class="mt-2">Loading campaign...</p>
  </div>

  <!-- Error state -->
  <div v-else-if="errorMessage"
       class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
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
          class="w-10 h-10 bg-primary-400 rounded-md flex items-center justify-center text-xs text-white font-medium overflow-hidden relative group no-underline"
          :class="{ 'ring-2 ring-primary-500': parseInt(route.params.id) === userCampaign.id }"
        >
          {{ userCampaign.name?.[0]?.toUpperCase() || '?' }}

          <!-- Tooltip on hover -->
          <span
            class="absolute left-12 w-auto p-2 bg-primary-700 text-white text-xs rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity z-10 whitespace-nowrap">
            {{ userCampaign.name }}
          </span>
        </RouterLink>
      </div>

      <RouterLink
        to="/campaigns"
        class="w-10 h-10 bg-primary-200 text-primary-800 rounded-md flex items-center justify-center hover:bg-primary-300 transition-colors no-underline relative group"
      >
        <span class="text-xl">+</span>

        <!-- Tooltip on hover -->
        <span
          class="absolute left-12 w-auto p-2 bg-primary-700 text-white text-xs rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity z-10 whitespace-nowrap">
          New Campaign
        </span>
      </RouterLink>
    </aside>

    <!-- Main content area -->
    <div class="flex-1 p-4 overflow-y-auto">
      <!-- Campaign header with edit button -->
      <div class="mb-6">
        <div v-if="!isEditing" class="group relative">
          <div class="flex justify-between items-start">
            <h2 class="text-xl sm:text-2xl font-bold">{{ campaign.name }}</h2>
            <button @click="startEditing" class="button button-small button-outline">
              Edit
            </button>
          </div>

          <!-- Campaign image -->
          <div class="my-3">
            <img
              v-if="campaign.imageUrl"
              :src="campaign.imageUrl"
              :alt="campaign.name"
              class="w-full h-48 object-cover rounded"
            >
            <div
              v-else
              class="w-full h-48 bg-gray-200 flex items-center justify-center rounded text-gray-500 text-sm"
            >
              No image has been set for this campaign
            </div>
          </div>

          <!-- Campaign description with line clamp -->
          <div class="mt-3">
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
                @click="toggleDescription"
                class="text-xs text-primary-500 mt-1 hover:underline"
              >
                {{ descriptionExpanded ? 'Show less' : 'Read more' }}
              </button>
            </template>
          </div>
        </div>

        <!-- Edit mode -->
        <div v-else class="bg-gray-50 p-4 rounded-lg border border-gray-200">
          <h3 class="text-lg font-medium mb-4">Edit Campaign</h3>

          <div class="mb-3">
            <label for="campaign-name" class="block text-sm font-medium text-gray-700 mb-1">
              Campaign Name
            </label>
            <input
              id="campaign-name"
              v-model="editedName"
              type="text"
              class="input input-bordered w-full mb-3"
              placeholder="Enter campaign name"
            />
          </div>

          <div class="mb-3">
            <label for="campaign-description" class="block text-sm font-medium text-gray-700 mb-1">
              Description
            </label>
            <textarea
              id="campaign-description"
              v-model="editedDescription"
              class="textarea textarea-bordered w-full"
              rows="4"
              placeholder="Enter campaign description"
            ></textarea>
          </div>

          <div class="flex space-x-3">
            <button @click="cancelEditing" class="button button-secondary" :disabled="isUpdating">
              Cancel
            </button>
            <button @click="saveChanges" class="button button-primary" :disabled="isUpdating">
              {{ isUpdating ? 'Saving...' : 'Save Changes' }}
            </button>
          </div>
        </div>

        <!-- Success message for campaign update -->
        <div v-if="successMessage" class="mt-3 p-2 bg-green-100 text-green-700 rounded-md text-sm">
          {{ successMessage }}
        </div>
      </div>

      <!-- Campaign content -->
      <section class="mb-6">
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
