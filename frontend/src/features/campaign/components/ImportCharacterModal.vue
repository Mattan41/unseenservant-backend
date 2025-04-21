<script setup>
import { onMounted, ref, watch } from 'vue'
import { useCharacterStore } from '@/features/character/characterStore.js'
import { useCampaignStore } from '@/features/campaign/campaignStore.js'
import CharacterImage from "@/features/character/components/CharacterImage.vue";

const props = defineProps({
  modelValue: Boolean,
  campaignId: {
    type: [String, Number],
    required: true,
  },
})

const emit = defineEmits(['update:modelValue', 'character-imported'])

const characterStore = useCharacterStore()
const campaignStore = useCampaignStore()

// Initialize an empty array to hold the available characters
const availableCharacters = ref([])
const isImporting = ref(null)

async function loadAvailableCharacters() {
  try {
    const result = await characterStore.fetchCharactersWithoutCampaign()
    availableCharacters.value = Array.isArray(result) ? result : []
  } catch (error) {
    console.error('Failed to load characters:', error)
    availableCharacters.value = []
  }
}

async function importCharacter(characterId) {
  isImporting.value = characterId
  try {
    await campaignStore.importCharacterToCampaign(characterId, props.campaignId)
    // Update the available characters list
    availableCharacters.value = availableCharacters.value.filter((char) => char.id !== characterId)
    emit('character-imported')
    // Don't close the modal if there are still characters left that the user can import
    if (availableCharacters.value.length === 0) {
      close()
    }
  } catch (error) {
    console.error('Failed to import character:', error)
  } finally {
    isImporting.value = null
  }
}

function close() {
  emit('update:modelValue', false)
}

onMounted(loadAvailableCharacters)

// If the modal is opened, reload the characters
watch(
  () => props.modelValue,
  (newValue) => {
    if (newValue) {
      loadAvailableCharacters()
    }
  },
)
</script>

<template>
  <div
    v-if="modelValue"
    class="fixed inset-0 w-full h-full bg-black/50 flex justify-center items-center z-[100]"
    @click.self="close"
  >
    <div class="bg-primary-50 p-8 rounded-lg max-w-[600px] w-[90%] max-h-[80vh] overflow-y-auto">
      <h4 class="mb-4 text-primary-800">Select a character to import to the campaign</h4>

      <div v-if="characterStore.isLoading" class="text-center py-8">
        <div
          class="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-500"
        ></div>
        <p class="mt-2 text-gray-600">Loading available characters...</p>
      </div>

      <div v-else-if="availableCharacters.length === 0" class="text-center py-8">
        <p class="text-gray-600">You don't have any characters available to import.</p>
        <router-link :to="{ name: 'CreateCharacter' }" class="button button-add">
          Create a new character
        </router-link>
      </div>

      <!-- Character grid -->
      <div v-else class="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-[calc(80vh-200px)] overflow-y-auto">
        <div
          v-for="character in availableCharacters"
          :key="character.id"
          class="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 border border-gray-100 flex flex-col"
        >
          <div class="p-4 flex-grow">
            <div class="flex items-start space-x-3">
              <CharacterImage
                :src="character.imageUrl"
                :alt="`${character.name} portrait`"
                class="w-14 h-14 rounded-lg border-2 border-primary-300 shadow-sm flex-shrink-0 object-cover"
              />

              <div class="flex-1 min-w-0">
                <h5
                  class="text-base font-semibold text-primary-700 line-clamp-2 break-words"
                  :title="character.name"
                >
                  {{ character.name }}
                </h5>

                <!-- Character basic info -->
                <div class="flex items-center text-xs text-gray-600 mt-1">
                  <span>{{ character.race }}</span>
                  <span class="mx-1">•</span>
                  <span>{{ character.characterClass }} (Level {{ character.level }})</span>
                </div>
              </div>
            </div>
          </div>

          <div class="bg-primary-50 px-4 py-2 flex justify-end">
            <button
              @click="importCharacter(character.id)"
              :disabled="isImporting === character.id"
              class="button button-primary text-sm"
            >
              <span v-if="isImporting === character.id">
                <span
                  class="inline-block animate-spin h-4 w-4 border-t-2 border-b-2 border-white rounded-full mr-1"
                ></span>
                Importing...
              </span>
              <span v-else>Import to campaign</span>
            </button>
          </div>
        </div>
      </div>

      <div class="mt-6 flex justify-end">
        <button @click="close" class="button button-secondary">Cancel</button>
      </div>
    </div>
  </div>
</template>


<style scoped>
</style>
