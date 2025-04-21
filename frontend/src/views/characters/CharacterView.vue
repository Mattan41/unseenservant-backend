<script setup>
import { ref, onMounted, computed } from 'vue';
import { useCharacterStore } from '@/stores/characterStore.js';
import { useRoute, useRouter } from 'vue-router';
import CharacterImage from "@/views/characters/components/CharacterImage.vue";

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
      <p class="mt-2 text-third-600">Loading character...</p>
    </div>

    <div v-else-if="!character" class="text-center py-8">
      <p class="text-third-600">Character not found.</p>
      <router-link :to="{ name: 'CharactersView' }" class="mt-4 inline-block bg-primary-500 hover:bg-primary-600 text-white font-bold py-2 px-4 rounded">
        Back to Character List
      </router-link>
    </div>

    <div v-else>
      <div class="bg-primary-50 rounded-lg shadow-lg overflow-hidden">
        <div class="p-6 border-b border-third-200">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 items-start">
            <!-- Left side: Image and name -->
            <div class="flex flex-col items-center md:items-start">
<!--              <img-->
<!--                :src="characterImageUrl"-->
<!--                alt="Character Image"-->
<!--                class="w-32 h-32 rounded-lg border-2 border-primary-300 shadow-md mb-2"-->
<!--              />-->
              <CharacterImage
                :src="characterStore.currentCharacter?.imageUrl"
                alt="Character portrait"
                class="w-64 h-64 rounded-lg border-2 border-primary-300 shadow-md mb-2"

              />
              <h3 class="text-xl font-bold text-third-700">{{ character.name }}</h3>
            </div>

            <!-- Middle: Character Details -->
            <div class="flex flex-col justify-center text-third-700 md:col-span-1">
              <div class="space-y-2">
                <p><strong>Race:</strong> {{ character.race }}</p>
                <p><strong>Class:</strong> {{ character.characterClass }}</p>
                <p><strong>Level:</strong> {{ character.level }}</p>
              </div>
            </div>

            <!-- Right side: Action buttons -->
            <div class="flex md:flex-col md:items-end space-x-2 md:space-x-0 md:space-y-2 justify-center md:justify-start">
              <router-link
                :to="{ name: 'EditCharacter', params: { id: character.id } }"
                class="button button-secondary"
              >
                Edit
              </router-link>
              <button
                @click="deleteCharacter"
                class="button button-remove"
              >
                Delete
              </button>
            </div>
          </div>

        </div>

        <!-- Stats Section -->
        <div class="p-6 bg-third-50">
          <h2 class="text-xl font-semibold mb-4 text-primary-700">Character Stats</h2>
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
            <div v-if="character.playerCharacterData">
              <div
                v-for="(value, stat) in character.playerCharacterData"
                :key="stat"
                class="bg-third-200 p-2 rounded-lg shadow text-center"
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
