<script setup>
import { computed, onMounted, ref } from 'vue'
import { useCharacterStore } from '@/features/character/characterStore.js'
import { useUserStore } from '@/features/user/userStore.js'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import CharacterImage from '@/features/character/components/CharacterImage.vue'

const characterStore = useCharacterStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()

const loading = ref(true)
const characterId = computed(() => route.params.id)
const from = route.query.from || 'characterList'
const campaignId = route.query.campaignId || null

const { userId } = storeToRefs(userStore)
const { currentCharacter } = storeToRefs(characterStore)

onMounted(async () => {
  try {
    await characterStore.fetchCharacter(characterId.value)
  } finally {
    loading.value = false
  }
})

const isOwner = computed(
  () =>
    !!currentCharacter.value &&
    !!userId.value &&
    String(currentCharacter.value.ownerId) === String(userId.value),
)

const deleteCharacter = async () => {
  if (confirm('Are you sure you want to delete this character? This action cannot be undone.')) {
    const success = await characterStore.deleteCharacter(characterId.value)
    if (success) {
      await router.push({ name: 'CharactersView' })
    }
  }
}
</script>

<template>
  <div class="container mx-auto p-4 max-w-4xl">
    <div v-if="loading" class="text-center py-8">
      <div
        class="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-500"
      ></div>
      <p class="mt-2 text-third-600">Loading character...</p>
    </div>

    <div v-else-if="!currentCharacter" class="text-center py-8">
      <p class="text-third-600">Character not found.</p>
      <router-link
        :to="{ name: 'CharactersView' }"
        class="mt-4 inline-block bg-primary-500 hover:bg-primary-600 text-white font-bold py-2 px-4 rounded"
      >
        Back to Character List
      </router-link>
    </div>

    <div v-else>
      <div class="bg-primary-50 rounded-lg shadow-lg overflow-hidden">
        <!-- Top bar for actions (visible only if allowed) -->
        <div v-if="isOwner" class="flex justify-end p-2 space-x-2">
          <router-link
            :to="{
              name: 'EditCharacter',
              params: { id: currentCharacter.id },
              query: from === 'campaign' && campaignId ? { from, campaignId } : {},
            }"
            class="button button-secondary"
          >
            Edit
          </router-link>
          <button @click="deleteCharacter" class="button button-remove">Delete</button>
        </div>

        <!-- Grid for main content -->
        <div class="p-6 border-b border-third-200">
          <div class="grid grid-cols-1 md:grid-cols-3 gap-4 items-start">
            <!-- Left side: Image and name -->
            <div class="flex flex-col items-center md:items-start">
              <CharacterImage
                :src="currentCharacter.imageUrl"
                alt="Character portrait"
                class="w-64 h-64 rounded-lg border-2 border-primary-300 shadow-md mb-2"
              />
              <h3 class="text-xl font-bold text-third-700">
                {{ currentCharacter.name }}
              </h3>
            </div>
            <!-- Right side: Character Details -->
            <div class="flex flex-col justify-center text-third-700 md:col-span-1">
              <div class="space-y-2">
                <p><strong>Race:</strong> {{ currentCharacter.race }}</p>
                <p><strong>Class:</strong> {{ currentCharacter.characterClass }}</p>
                <p><strong>Level:</strong> {{ currentCharacter.level }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Stats Section -->
        <div class="p-6 bg-third-50">
          <h2 class="text-xl font-semibold mb-4 text-primary-700">Character Stats</h2>
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
            <div v-if="currentCharacter.playerCharacterData">
              <div
                v-for="(value, stat) in currentCharacter.playerCharacterData"
                :key="stat"
                class="bg-third-200 p-2 rounded-lg shadow text-center"
              >
                <div class="text-lg font-bold text-primary-700">{{ value }}</div>
                <div class="text-xs uppercase tracking-wide text-third-600">{{ stat }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Additional Character Information -->
        <div class="p-6 border-t border-gray-200">
          <h2 class="text-xl font-semibold mb-4 text-primary-700">Additional Information</h2>
          <p>
            <strong>Created:</strong>
            {{ new Date(currentCharacter.createdAt).toLocaleDateString() }}
          </p>
          <p>
            <strong>Last Updated:</strong>
            {{ new Date(currentCharacter.updatedAt).toLocaleDateString() }}
          </p>
        </div>
      </div>

      <div class="mt-6">
        <router-link
          v-if="from === 'campaign' && campaignId"
          :to="{ name: 'CampaignView', params: { id: campaignId } }"
          class="text-primary-500 hover:text-primary-700"
        >
          ← Back to Campaign
        </router-link>
        <router-link
          v-else
          :to="{ name: 'CharactersView' }"
          class="text-primary-500 hover:text-primary-700"
        >
          ← Back to Character List
        </router-link>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
