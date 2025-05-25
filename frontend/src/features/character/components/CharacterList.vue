<script setup>
import { onMounted, ref } from 'vue'
import { useCharacterStore } from '@/features/character/characterStore.js'
import { useRouter } from 'vue-router'
import { useCampaignStore } from '@/features/campaign/campaignStore.js'
import CharacterImage from '@/features/character/components/CharacterImage.vue'

const router = useRouter()
const characterStore = useCharacterStore()
const campaignStore = useCampaignStore()

const loading = ref(false)
const getCampaignName = (campaignId) => {
  if (!campaignId) return 'No campaign'
  return campaignStore.getCampaignTitle(campaignId) || 'Unknown campaign'
}

onMounted(async () => {
  loading.value = true
  await characterStore.fetchAllCharactersForCurrentUser()
  await campaignStore.fetchAllCampaignsForCurrentUser()
  loading.value = false
})

const viewCharacter = (id) => {
  router.push({ name: 'CharacterView', params: { id } })
}
</script>

<template>
  <div class="container mx-auto p-4">
    <h1 class="bg-primary-100 text-primary-900 p-4 mb-4 rounded-lg font-bold">Characters</h1>
    <div v-if="characterStore.isLoading || loading" class="text-center py-8">
      <div
        class="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-500"
      ></div>
      <p class="mt-2 text-gray-600">Loading characters...</p>
    </div>

    <div v-else-if="characterStore.characters.length === 0" class="text-center py-8">
      <p class="text-gray-600">You don't have any characters yet.</p>
      <router-link
        :to="{ name: 'CreateCharacter' }"
        class="mt-4 inline-block bg-primary-500 hover:bg-primary-600 text-white font-bold py-2 px-4 rounded"
      >
        Create your first character
      </router-link>
    </div>
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="character in characterStore.characters"
        :key="character.id"
        class="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-lg transition-shadow duration-300 cursor-pointer border border-gray-100 flex flex-col h-full"
        @click="viewCharacter(character.id)"
      >
        <!-- Character card -->
        <div class="p-4 flex-grow">
          <div class="flex items-start space-x-3">
            <CharacterImage
              :src="character.imageUrl"
              :alt="`${character.name} portrait`"
              class="w-16 h-16 rounded-lg border-2 border-primary-300 shadow-sm flex-shrink-0 object-cover"
            />

            <div class="flex-1 min-w-0">
              <h5
                class="text-base font-semibold text-primary-700 line-clamp-2 hover:text-primary-800 break-words"
                :title="character.name"
              >
                {{ character.name }}
              </h5>

              <!-- Campaign with icon-->
              <div class="flex items-center text-xs text-gray-600 mt-1">
                <span class="inline-flex items-center truncate w-full">
                  <span class="mr-1 flex-shrink-0">🏰</span>
                  <span
                    class="truncate"
                    :title="getCampaignName(character.campaignId) || 'No Campaign'"
                  >
                    {{ getCampaignName(character.campaignId) || 'No Campaign' }}
                  </span>
                </span>
              </div>
            </div>
          </div>

          <!-- Character information -->
          <div class="mt-3 pt-2 border-t border-third-100">
            <div class="flex flex-wrap gap-2">
              <span
                class="inline-block bg-primary-50 text-primary-700 text-xs px-2 py-1 rounded-full"
              >
                {{ character.race }}
              </span>
              <span
                class="inline-block bg-primary-50 text-primary-700 text-xs px-2 py-1 rounded-full"
              >
                {{ character.characterClass }}
              </span>
              <span
                class="inline-block bg-secondary-100 text-secondary-800 text-xs px-2 py-1 rounded-full"
              >
                Level {{ character.level }}
              </span>
            </div>
          </div>
        </div>

        <!-- Field for actions -->
        <div
          class="bg-third-50 px-4 py-2 border-t border-third-100 flex justify-between items-center"
        >
          <div class="text-xs text-gray-500 truncate mr-2">
            <span v-if="character.lastUpdated"
              >Updated {{ new Date(character.lastUpdated).toLocaleDateString() }}</span
            >
          </div>
          <router-link
            :to="{ name: 'CharacterView', params: { id: character.id }, query: { from: 'characterList' } }"
            class="text-primary-600 hover:text-primary-800 font-medium text-sm transition-colors inline-flex items-center flex-shrink-0"
            @click.stop
          >
            View Details
            <svg
              xmlns="http://www.w3.org/2000/svg"
              class="h-3 w-3 ml-1"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M9 5l7 7-7 7"
              />
            </svg>
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

<style scoped></style>
