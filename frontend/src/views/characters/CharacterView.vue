<script setup>
import { ref, onMounted, computed } from 'vue';
import { useCharacterStore } from '@/stores/characterStore.js';
import { useRoute, useRouter } from 'vue-router';

const characterStore = useCharacterStore();
const route = useRoute();
const router = useRouter();
const loading = ref(true);

const characterId = computed(() => route.params.id);

onMounted(async () => {
  try {
    await characterStore.fetchCharacter(characterId.value);
  } finally {
    loading.value = false;
  }
});

const character = computed(() =>
  characterStore.currentCharacter || characterStore.getCharacterById(characterId.value)
);

const deleteCharacter = async () => {
  if (confirm('Are you sure you want to delete this character? This action cannot be undone.')) {
    const success = await characterStore.deleteCharacter(characterId.value);
    if (success) {
      await router.push({name: 'CharacterView'});
    }
  }
};
</script>

<template>
  <div class="container mx-auto p-4 max-w-4xl">
    <div v-if="characterStore.isLoading || loading" class="text-center py-8">
      <div class="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-500"></div>
      <p class="mt-2 text-gray-600">Loading character...</p>
    </div>

    <div v-else-if="characterStore.error" class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
      <p>{{ characterStore.error }}</p>
    </div>

    <div v-else-if="!character" class="text-center py-8">
      <p class="text-gray-600">Character not found.</p>
      <router-link :to="{ name: 'CharactersView' }" class="mt-4 inline-block bg-primary-500 hover:bg-primary-600 text-white font-bold py-2 px-4 rounded">
        Back to Character List
      </router-link>
    </div>

    <div v-else>
      <div class="bg-white rounded-lg shadow-lg overflow-hidden">
        <div class="p-6 border-b border-gray-200">
          <div class="flex justify-between items-center">
            <h1 class="text-2xl font-bold text-primary-700">{{ character.name }}</h1>
            <div class="flex space-x-2">
              <router-link
                :to="{ name: 'EditCharacter', params: { id: character.id } }"
                class="bg-primary-500 hover:bg-primary-600 text-white px-4 py-2 rounded"
              >
                Edit
              </router-link>
              <button
                @click="deleteCharacter"
                class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded"
              >
                Delete
              </button>
            </div>
          </div>

          <div class="flex flex-wrap mt-4 text-gray-700">
            <div class="w-full md:w-1/2 mb-4">
              <p><strong>Race:</strong> {{ character.race }}</p>
              <p><strong>Class:</strong> {{ character.characterClass }}</p>
              <p><strong>Level:</strong> {{ character.level }}</p>
            </div>
          </div>
        </div>

        <!-- Stats Section -->
        <div class="p-6 bg-gray-50">
          <h2 class="text-xl font-semibold mb-4 text-primary-700">Character Stats</h2>
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
            <div v-if="character.playerCharacterData">
              <div
                v-for="(value, stat) in character.playerCharacterData"
                :key="stat"
                class="bg-white p-4 rounded-lg shadow text-center"
              >
                <div class="text-lg font-bold text-primary-600">{{ value }}</div>
                <div class="text-xs uppercase tracking-wide text-gray-500">{{ stat }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Additional Character Information -->
        <div class="p-6 border-t border-gray-200">
          <h2 class="text-xl font-semibold mb-4 text-primary-700">Additional Information</h2>
          <p><strong>Created:</strong> {{ new Date(character.createdAt).toLocaleDateString() }}</p>
          <p><strong>Last Updated:</strong> {{ new Date(character.updatedAt).toLocaleDateString() }}</p>
        </div>
      </div>

      <div class="mt-6">
        <router-link :to="{ name: 'CharactersView' }" class="text-primary-500 hover:text-primary-700">
          ← Back to Character List
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped>
</style>
