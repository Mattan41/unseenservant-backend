<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useCampaignStore } from '@/features/campaign/campaignStore.js'
import { useUserStore } from '@/features/user/userStore.js'
import CampaignSettings from '@/features/campaign/components/CampaignSettings.vue'
import { useNotificationStore } from '@/stores/notificationStore.js'
import ImportCharacterModal from '@/features/campaign/components/ImportCharacterModal.vue'
import CharacterImage from '@/features/character/components/CharacterImage.vue'
import EditCampaignModal from '@/features/campaign/components/EditCampaignModal.vue'
import CampaignImage from "@/features/campaign/components/CampaignImage.vue";

const route = useRoute()
const router = useRouter()
const campaignStore = useCampaignStore()
const userStore = useUserStore()
const campaign = ref(null)
const isLoading = ref(true)
const isCharactersListVisible = ref(false)
const showSettings = ref(false)
const descriptionExpanded = ref(false)
const campaignListRef = ref(null)
const isScrollable = ref(false)
const showImportModal = ref(false)
const showEditModal = ref(false)

// Check if current user is the owner
const isOwner = computed(() => {
  if (!campaign.value || !userStore.currentUser) return false
  return campaign.value.ownerId === userStore.userId
})

// Load user data for ownership check
const loadUserData = async () => {
  if (!userStore.currentUser) {
    await userStore.fetchCurrentUser()
  }
}

const loadCampaignData = async () => {
  const notificationStore = useNotificationStore()
  isLoading.value = true
  const campaignStore = useCampaignStore()

  try {
    // Load all campaigns for the current user
    await campaignStore.fetchAllCampaignsForCurrentUser()
  } catch (error) {
    console.error('Failed to load campaigns:', error)
    notificationStore.addNotification('Failed to load campaigns: ' + error.message, 'error')
    isLoading.value = false
    return
  }

  // Load the campaign with the given id
  const campaignId = route.params.id
  const campaignExists = campaignStore.campaigns.some((c) => c.id === parseInt(campaignId))
  if (!campaignExists) {
    await router.push({ name: 'campaignsView' })
    return
  }

  try {
    campaign.value = await campaignStore.fetchCampaign(campaignId)
    await campaignStore.fetchCharactersForCampaign(campaignId)
  } catch (error) {
    console.error('Failed to load campaign:', error)
    notificationStore.addNotification('Failed to load campaign: ' + error.message, 'error')
  } finally {
    isLoading.value = false
  }
}

// Computed property to fetch characters for the current campaign from the campaign store
const campaignCharacters = computed(() => {
  return campaignStore.getCharactersByCampaignId(parseInt(route.params.id))
})

const isLoadingCharacters = computed(() => campaignStore.loadingCharacters)

// Group characters by participant id
const charactersByParticipant = computed(() => {
  if (!campaign.value?.participants) return {}

  const characterMap = {}

  // All participants in the campaign - viktigt att behålla alla deltagare
  campaign.value.participants.forEach((participant) => {
    characterMap[participant.id] = {
      participant: participant,
      characters: [],
    }
  })

  // add owner to the character map if they are not already included
  if (campaign.value.ownerId && !characterMap[campaign.value.ownerId]) {
    let ownerInfo = campaign.value.participants.find((p) => p.id === campaign.value.ownerId)
    if (!ownerInfo) {
      ownerInfo = { id: campaign.value.ownerId, nickname: 'Campaign Owner' }
    }
    characterMap[ownerInfo.id] = {
      participant: ownerInfo,
      characters: [],
    }
  }

  // Fill characters for participants (only if we have characters)
  if (campaignCharacters.value && campaignCharacters.value.length) {
    campaignCharacters.value.forEach((character) => {
      if (characterMap[character.ownerId]) {
        characterMap[character.ownerId].characters.push(character)
      }
    })
  }

  return characterMap
})

const onCharacterImported = () => {
  loadCampaignData() // perhaps all that is needed?
}

// Remove character function
const removeCharacter = async (characterId) => {
  if (confirm('Are you sure you want to remove this character from the campaign?')) {
    const campaignId = parseInt(route.params.id)
    campaignCharacters.value.filter((character) => character.id !== characterId)
    try {
      await campaignStore.removeCharacterFromCampaign(characterId)
    } catch (error) {
      console.log('Error caught in component:', error)
    }
    await campaignStore.fetchCharactersForCampaign(campaignId)
  }
}

const openEditModal = () => {
  if (!isOwner.value) return
  showEditModal.value = true
}

const handleSaveCampaign = async (updatedCampaign) => {
  const notificationStore = useNotificationStore()

  try {
    // Update campaign info in the store
    await campaignStore.updateCampaignInfo(campaign.value.id, {
      name: updatedCampaign.title,
      description: updatedCampaign.description,
    })

    // Upload image file if one was selected
    if (updatedCampaign.imageFile) {
      const updated = await campaignStore.uploadCampaignImage(campaign.value.id, updatedCampaign.imageFile)
      campaign.value.imageUrl = updated.imageUrl
    }

    // Update local campaign object with edited values
    campaign.value.name = updatedCampaign.title
    campaign.value.description = updatedCampaign.description

    // close the modal
    showEditModal.value = false

    notificationStore.addNotification('Campaign updated successfully', 'success')
  } catch (error) {
    console.error('Failed to update campaign:', error)
    notificationStore.addNotification('Failed to update campaign: ' + error.message, 'error')
  }
}

const toggleCharactersList = () => {
  isCharactersListVisible.value = !isCharactersListVisible.value
}

const toggleSettings = () => {
  showSettings.value = !showSettings.value
}

// Toggle description expanded state
const toggleDescription = () => {
  descriptionExpanded.value = !descriptionExpanded.value
}

// Handle participants updated
const handleParticipantsUpdated = () => {
  loadCampaignData() // Reload full campaign data
}

// Check if the campaign list is scrollable
const checkScrollable = () => {
  if (campaignListRef.value) {
    const element = campaignListRef.value
    isScrollable.value = element.scrollHeight > element.clientHeight
  }
}

onMounted(async () => {
  await loadCampaignData()

  // Check if scrollable after component is mounted and campaigns are loaded
  checkScrollable()

  // Add resize listener to recheck when window size changes
  window.addEventListener('resize', checkScrollable)
})

// Clean up the event listener when component is unmounted
onUnmounted(() => {
  window.removeEventListener('resize', checkScrollable)
  if (route.params.id) {
    campaignStore.clearCampaignCharacters(parseInt(route.params.id))
  }
})

// Watch for route parameter changes to reload data
watch(
  () => route.params.id,
  async (newId) => {
    if (newId) {
      await loadCampaignData()
      // Recheck scrollable after data loads and DOM updates
      setTimeout(checkScrollable, 100)
    }
  },
  { immediate: true },
)

//  watch the campaign store for changes that might affect scrollable
watch(
  () => campaignStore.campaigns.length,
  () => {
    setTimeout(checkScrollable, 100)
  },
)
// watch characters by participant
watch(
  campaignCharacters,
  () => {
    if (campaign.value) {
      checkScrollable()
    }
  },
  { deep: true },
)
</script>

<template>
  <!-- Loading state -->
  <div v-if="isLoading" class="flex flex-col items-center justify-center h-full p-8">
    <div class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
    <p class="mt-2">Loading campaign...</p>
  </div>

  <!-- Campaign loaded successfully -->
  <div v-else-if="campaign" class="flex h-full">
    <!-- Campaign selector sidebar - same for all screen sizes -->
    <aside
      class="w-16 flex flex-col min-h-screen items-center py-4 space-y-4 relative custom-gradient"
    >
      <!-- Scroll hint at top if scrollable -->
      <div
        v-if="isScrollable"
        class="absolute top-2 left-1/2 transform -translate-x-1/2 w-5 h-1 bg-primary-400 rounded-full animate-pulse"
      ></div>

      <div
        ref="campaignListRef"
        class="campaign-list flex-1 flex flex-col items-center space-y-4 max-h-[calc(10*2.5rem+2rem)]"
      >
        <RouterLink
          v-for="userCampaign in campaignStore.campaigns"
          :key="userCampaign.id"
          :to="{ name: 'CampaignView', params: { id: userCampaign.id } }"
          class="w-10 h-10 rounded-md flex items-center justify-center text-primary-500 font-medium relative group no-underline border border-primary-400 hover:scale-110 flex-shrink-0"
          :class="{ 'ring-2 ring-primary-500': parseInt(route.params.id) === userCampaign.id }"
          :style="
            campaignStore.getCampaignImageUrl(userCampaign.id)
              ? {
                  backgroundImage: `url(${campaignStore.getCampaignImageUrl(userCampaign.id)})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                }
              : {}
          "
        >
          <!-- Campaign content -->
          <span
            class="absolute left-full ml-2 px-2 py-1 bg-primary-600 text-white text-xs rounded whitespace-nowrap opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity z-50 pointer-events-none"
          >
            {{ campaignStore.getCampaignTitle(userCampaign.id) }}
          </span>
        </RouterLink>
      </div>
      <div
        class="h-2 rounded-md flex items-center justify-center text-white font-medium relative group"
      >
        <div
          v-if="isScrollable"
          class="absolute bottom-2 left-1/2 transform -translate-x-1/2 w-5 h-1 bg-primary-400 rounded-full animate-pulse"
        ></div>
      </div>
      <RouterLink
        to="/campaigns"
        class="w-10 h-10 bg-primary-200 text-primary-800 rounded-md flex items-center justify-center hover:bg-primary-300 transition-colors no-underline relative group mt-2 hover:scale-110 flex-shrink-0"
      >
        <span class="text-xl">+</span>
        <span
          class="absolute top-1/2 left-full transform -translate-y-1/2 ml-2 w-auto p-2 bg-primary-700 text-white text-xs rounded shadow-lg opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-opacity z-50 whitespace-nowrap pointer-events-none"
        >
          to campaign overview
        </span>
      </RouterLink>
    </aside>

    <!-- Main content area -->
    <div class="flex-1 p-4 overflow-y-auto">
      <!-- Campaign header with edit button -->
      <div class="mb-6">
        <div class="group relative">
          <div class="flex justify-between items-start">
            <h2 class="text-xl sm:text-2xl font-bold">
              {{ campaignStore.getCampaignTitle(campaign.id) }}
            </h2>
            <button
              v-if="isOwner"
              @click="openEditModal"
              class="button button-primary button-small button-outline"
            >
              Edit Campaign
            </button>
          </div>

          <!-- Campaign image -->
          <CampaignImage
            :src="campaignStore.getCampaignImageUrl(campaign.id)"
            :alt="campaignStore.getCampaignTitle(campaign.id)"
            class="w-full h-48 object-cover rounded"
          />

          <!-- Campaign description with line clamp -->
          <div class="mt-3 break-words whitespace-pre-line">
            <p
              v-if="!campaignStore.getCampaignDescription(campaign.id)"
              class="italic text-gray-500 text-sm"
            >
              No description available.
            </p>

            <template v-else>
              <p :class="{ 'line-clamp-2': !descriptionExpanded }" class="text-sm text-gray-700">
                {{ campaignStore.getCampaignDescription(campaign.id) }}
              </p>

              <button
                v-if="
                  campaignStore.getCampaignDescription(campaign.id) &&
                  campaignStore.getCampaignDescription(campaign.id).length > 60
                "
                @click="toggleDescription"
                class="text-xs text-primary-500 mt-1 hover:underline"
              >
                {{ descriptionExpanded ? 'Show less' : 'Read more' }}
              </button>
            </template>
          </div>
        </div>

        <!-- Campaign content -->
        <section class="mb-6">
          <!-- Participants collapsible section -->
          <div class="mb-4 border rounded p-3">
            <h3 class="font-medium cursor-pointer flex items-center" @click="toggleCharactersList">
              <span v-if="isCharactersListVisible" class="transform rotate-90 inline-block mr-1"
                >›</span
              >
              <span v-else class="inline-block mr-1">›</span>
              Participants & Characters
            </h3>

            <div v-if="isCharactersListVisible" class="mt-2">
              <div v-if="isLoadingCharacters" class="pl-4 py-2 text-gray-500">
                Loading characters...
              </div>

              <div v-else-if="!campaign?.participants?.length" class="pl-4 py-2 text-gray-500">
                No participants in this campaign yet.
              </div>

              <!-- For each participants and its characters -->
              <div v-else>
                <div
                  v-for="(data, participantId) in charactersByParticipant"
                  :key="participantId"
                  class="mb-3 border-l-2 border-primary-200"
                >
                  <div class="pl-4 py-1 font-medium flex flex-wrap items-center">
                    <span class="mr-2 flex-shrink-0">•</span>
                    <span
                      class="truncate max-w-[150px] sm:max-w-none"
                      :title="data.participant.nickname"
                    >
                      {{ data.participant.nickname }}
                    </span>
                    <span v-if="data.participant.role" class="text-gray-600 ml-1 truncate">
                      ({{ data.participant.role }})
                    </span>
                    <span
                      v-if="data.participant.id === campaign.ownerId"
                      class="text-primary-600 text-sm ml-1 whitespace-nowrap"
                    >
                      (Campaign Owner)
                    </span>
                  </div>

                  <!-- Show participants characters -->
                  <div v-if="data.characters.length > 0" class="pl-8">
                    <div
                      v-for="character in data.characters"
                      :key="character.id"
                      class="py-1 flex flex-wrap items-center text-gray-700"
                    >
                      <span class="text-primary-500 mr-1 flex-shrink-0">◦</span>
                      <CharacterImage
                        :src="character.imageUrl"
                        :alt="`${character.name} portrait`"
                        class="w-10 h-10 rounded-lg border-2 border-primary-300 shadow-sm flex-shrink-0 object-cover"
                      >
                      </CharacterImage>
                      <span
                        class="truncate max-w-[120px] sm:max-w-[200px] md:max-w-none"
                        :title="character.name"
                      >
                        {{ character.name }}
                      </span>
                      <span
                        v-if="character.characterClass"
                        class="text-sm text-gray-600 ml-1 truncate"
                      >
                        ({{ character.characterClass }}
                        <span v-if="character.level"> , Level {{ character.level }} </span>)
                      </span>

                      <router-link
                        v-if="
                          (userStore.currentUser && userStore.userId === character.ownerId) ||
                          campaignStore.isUserGM(campaign.id, userStore.userId)
                        "
                        :to="{
                          name: 'CharacterView',
                          params: { id: character.id },
                          query: { from: 'campaign', campaignId: campaign.id },
                        }"
                        class="button button-primary"
                      >
                        Open Character Details
                      </router-link>
                      <button
                        v-if="userStore.currentUser && userStore.userId === character.ownerId"
                        @click="removeCharacter(character.id)"
                        class="button button-remove ml-auto mt-1 sm:mt-0"
                        aria-label="Remove character"
                      >
                        Remove
                      </button>
                    </div>
                  </div>

                  <!-- If the participant has no characters yet in the campaign -->
                  <div v-else class="pl-8 py-1 text-gray-500 text-sm italic">No characters</div>
                </div>
              </div>
            </div>
          </div>

          <!-- Action Buttons -->
          <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-5">
            <button class="button button-primary" @click="toggleSettings">Campaign Settings</button>
            <button class="button button-primary" @click="showImportModal = true">
              IMPORT CHARACTER
            </button>
          </div>

          <EditCampaignModal
            v-if="showEditModal && campaign"
            :campaign="{
              id: campaign.id,
              title: campaignStore.getCampaignTitle(campaign.id),
              description: campaignStore.getCampaignDescription(campaign.id),
              imageUrl: campaignStore.getCampaignImageUrl(campaign.id),
            }"
            @close="showEditModal = false"
            @save="handleSaveCampaign"
          />

          <!-- Import modal-component -->
          <ImportCharacterModal
            v-model="showImportModal"
            :campaign-id="campaign.id"
            @character-imported="onCharacterImported"
          />

          <!-- Modal overlay -->
          <div
            v-if="showSettings"
            class="fixed inset-0 z-30 bg-black/50 flex items-center justify-center"
            @click="showSettings = false"
          >
            <div
              class="bg-primary-100 p-6 rounded-lg max-w-2xl max-h-[90vh] overflow-y-auto w-full m-4 shadow-lg border border-primary-300"
              @click.stop
            >
              <div class="flex justify-between items-center mb-4">
                <h3 class="text-2xl font-semibold text-third-800">Campaign Settings</h3>
                <button
                  @click="showSettings = false"
                  class="text-gray-600 hover:text-gray-800 focus:outline-none"
                >
                  &times;
                </button>
              </div>
              <CampaignSettings
                :campaignId="String(campaign.id)"
                @close-modal="showSettings = false"
                @participants-updated="handleParticipantsUpdated"
              />
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-gradient {
  background: linear-gradient(
    to bottom,
    var(--color-primary-100) 0%,
    var(--color-primary-600) 50%,
    var(--color-primary-100) 100%
  );
}

.campaign-list {
  overflow-y: auto;
  overflow-x: hidden;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.campaign-list::-webkit-scrollbar {
  display: none;
}

/* Animation for the scroll hint */
@keyframes pulse {
  0%,
  100% {
    opacity: 0.3;
  }
  50% {
    opacity: 0.8;
  }
}

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}
</style>
