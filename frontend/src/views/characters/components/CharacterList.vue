<script setup>
import { ref, onMounted } from 'vue';
import { useCharacterStore } from '@/stores/characterStore.js';
import { useRouter } from 'vue-router';
import {useCampaignStore} from "@/stores/campaignStore.js";

const router = useRouter();
const characterStore = useCharacterStore();
const campaignStore = useCampaignStore();

const loading = ref(false);
const getCampaignName = (campaignId) => {
  if (!campaignId) return 'No campaign';
  return campaignStore.getCampaignTitle(campaignId) || 'Unknown campaign';
};

onMounted(async () => {
  loading.value = true;
  await characterStore.fetchAllCharactersForCurrentUser();
  await campaignStore.fetchAllCampaignsForCurrentUser();
  loading.value = false;
});

const viewCharacter = (id) => {
  router.push({ name: 'CharacterView', params: { id } });
};
</script>

<template>
  <div class="container mx-auto p-4">
    <div v-if="characterStore.isLoading || loading" class="text-center py-8">
      <div class="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-500"></div>
      <p class="mt-2 text-gray-600">Loading characters...</p>
    </div>

    <div v-else-if="characterStore.characters.length === 0" class="text-center py-8">
      <p class="text-gray-600">You don't have any characters yet.</p>
      <router-link :to="{ name: 'CreateCharacter' }" class="mt-4 inline-block bg-primary-500 hover:bg-primary-600 text-white font-bold py-2 px-4 rounded">
        Create your first character
      </router-link>
    </div>

    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="character in characterStore.characters"
        :key="character.id"
        class="bg-primary-50 rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 cursor-pointer"
        @click="viewCharacter(character.id)"
      >
        <div class="p-4 border-b border-gray-200">
          <h3 class="text-lg font-semibold text-primary-600">{{ character.name }}</h3>
          <div class="flex items-center mt-2">
            <span>{{ getCampaignName(character.campaignId) }}</span>
          </div>
          <div class="flex justify-between text-sm text-gray-600 mt-1">
            <span>{{ character.race }}</span>
            <span>{{ character.characterClass }} (Level {{ character.level }})</span>
          </div>
        </div>
        <div class="bg-gray-50 px-4 py-2 text-right">
          <router-link
            :to="{ name: 'CharacterView', params: { id: character.id } }"
            class="text-primary-500 hover:text-primary-700 font-medium text-sm"
          >
            View Details →
          </router-link>
        </div>
      </div>
    </div>

    <div class="mt-6 text-center">
      <router-link
        :to="{ name: 'CreateCharacter' }"
        class="bg-primary-500 hover:bg-primary-600 text-white font-bold py-2 px-4 rounded inline-flex items-center"
      >
        <span class="mr-2">+</span> Create New Character
      </router-link>
    </div>
  </div>
</template>

<style scoped>
</style>
