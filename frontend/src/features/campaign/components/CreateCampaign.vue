<script setup>
import {ref} from 'vue';
import {useCampaignStore} from '@/features/campaign/campaignStore.js';

const campaignStore = useCampaignStore();

const showCreateCampaignModal = ref(false);
const newCampaignName = ref('');
const newCampaignDescription = ref('');
const isCreating = ref(false);
const errorMessage = ref('');

const createCampaign = async () => {
  isCreating.value = true;
  try {
    await campaignStore.createCampaign(newCampaignName.value, newCampaignDescription.value);
    showCreateCampaignModal.value = false;
    newCampaignName.value = '';
    newCampaignDescription.value = '';
    errorMessage.value = '';
  } catch (error) {
    console.error(error);
    errorMessage.value = error.message || 'Failed to create campaign';
  } finally {
    isCreating.value = false;
  }
};
</script>

<template>
  <div>
    <button @click="showCreateCampaignModal = true" class="button button-add">
      Create new campaign
    </button>
    <div v-if="showCreateCampaignModal"
         class="fixed top-0 left-0 w-full h-full flex items-center justify-center bg-primary-600 bg-opacity-50">
      <div class="bg-white p-8 rounded">
        <h3 class="text-lg font-bold mb-4">Create a new campaign</h3>
        <form @submit.prevent="createCampaign">
          <div class="mb-4">
            <label for="name" class="block text-sm font-medium text-gray-700">Name of your
              campaign</label>
            <input id="name" v-model="newCampaignName" type="text"
                   class="mt-1 block w-full rounded-md shadow-sm">
          </div>
          <div class="mb-4">
            <label for="description"
                   class="block text-sm font-medium text-gray-700">Description</label>
            <textarea id="description" v-model="newCampaignDescription"
                      class="mt-1 block w-full rounded-md shadow-sm" rows="5">
            </textarea>
          </div>
          <button type="submit" class="button button-add">
            create
          </button>
          <button @click="showCreateCampaignModal = false" class="button button-secondary">
            Cancel
          </button>
        </form>
        <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
      </div>
    </div>
  </div>
</template>

