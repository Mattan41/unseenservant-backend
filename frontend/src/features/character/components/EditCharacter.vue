<script setup>
import {computed, onMounted, ref} from 'vue'
import {useCharacterStore} from '@/features/character/characterStore.js'
import {useRoute, useRouter} from 'vue-router'
import CharacterImage from "@/features/character/components/CharacterImage.vue";

const characterStore = useCharacterStore()
const route = useRoute()
const router = useRouter()

const characterId = computed(() => route.params.id)
const campaignId = computed(() => route.query.campaignId || null)
const from = computed(() => route.query.from || null)

const loading = ref(true)
const formError = ref('')
const isSubmitting = ref(false)

const fileInput = ref(null)
const previewImage = ref(null)
const selectedFile = ref(null)

const character = ref({
  name: '',
  race: '',
  characterClass: '',
  level: 1,
  imageUrl: null,
  playerCharacterData: {
    strength: 10,
    dexterity: 10,
    constitution: 10,
    intelligence: 10,
    wisdom: 10,
    charisma: 10,
  },
})

const characterImageUrl = computed(() => {
  if (previewImage.value) return previewImage.value
  return character.value.imageUrl
})

const races = [
  'Human', 'Elf', 'Dwarf', 'Halfling', 'Gnome', 'Half-Elf', 'Half-Orc', 'Dragonborn', 'Tiefling'
]
const characterClasses = [
  'Fighter', 'Wizard', 'Rogue', 'Cleric', 'Ranger', 'Paladin', 'Barbarian', 'Bard', 'Druid', 'Monk', 'Sorcerer', 'Warlock'
]

onMounted(async () => {
  try {
    const fetchedCharacter = await characterStore.fetchCharacter(characterId.value)
    if (fetchedCharacter) {
      character.value = {...fetchedCharacter}
      if (!character.value.playerCharacterData) {
        character.value.playerCharacterData = {
          strength: 10,
          dexterity: 10,
          constitution: 10,
          intelligence: 10,
          wisdom: 10,
          charisma: 10,
        }
      }
    } else {
      formError.value = 'Character not found'
    }
  } catch (error) {
    formError.value = error.message || 'Failed to load character'
  } finally {
    loading.value = false
  }
})

// Image
function triggerFileInput() {
  fileInput.value.click()
}

function handleImageChange(event) {
  const file = event.target.files[0]
  if (file) {
    selectedFile.value = file
    previewImage.value = URL.createObjectURL(file)
  }
}

// Navigation helper:
function goToCharacterView() {
  const base = {name: "CharacterView", params: {id: characterId.value}}
  if (from.value && campaignId.value) {
    return {...base, query: {from: from.value, campaignId: campaignId.value}}
  }
  return base
}

const submitCharacter = async () => {
  if (!character.value.name) {
    formError.value = 'Character name is required'
    return
  }
  if (!character.value.race) {
    formError.value = 'You must select a race'
    return
  }
  if (!character.value.characterClass) {
    formError.value = 'You must select a class'
    return
  }

  isSubmitting.value = true
  formError.value = ''

  try {
    if (selectedFile.value) {
      const updatedCharacter = await characterStore.uploadCharacterImage(characterId.value, selectedFile.value)
      character.value.imageUrl = updatedCharacter.imageUrl || updatedCharacter
    }
    const updatedCharacter = await characterStore.updateCharacter(characterId.value, character.value)
    if (updatedCharacter) {
      await router.push(goToCharacterView())
    }
  } catch (error) {
    formError.value = error.message || 'Failed to update character'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<template>
  <div class="container mx-auto p-4 max-w-2xl">
    <div v-if="loading" class="text-center py-8">
      <div
        class="inline-block animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-primary-500"></div>
      <p class="mt-2 text-gray-600">Loading character...</p>
    </div>

    <div v-else class="bg-white rounded-lg shadow-lg overflow-hidden">
      <div class="p-6 border-b border-gray-200">
        <h1 class="text-2xl font-bold text-primary-700">Edit Character</h1>
      </div>

      <form @submit.prevent="submitCharacter" class="p-6">
        <div v-if="formError"
             class="mb-4 bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          {{ formError }}
        </div>

        <!-- Character Image -->
        <div class="mb-6">
          <h5 class="text-lg font-semibold mb-3 text-primary-600">Character Image</h5>
          <div class="flex items-center space-x-4">
            <div class="relative">
              <CharacterImage :src="characterImageUrl" alt="Character Image"
                              class="w-24 h-24 rounded-lg object-cover border-2 border-primary-300"/>
              <div
                @click="triggerFileInput"
                class="absolute inset-0 bg-black bg-opacity-50 rounded-lg flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity cursor-pointer">
                <span class="text-white text-sm">Change</span>
              </div>
            </div>
            <input
              type="file"
              ref="fileInput"
              @change="handleImageChange"
              accept=".jpg,.jpeg,.png,.gif,.webp"
              class="hidden"/>
            <button type="button" @click="triggerFileInput" class="button button-secondary">
              Upload new image
            </button>
          </div>
          <div class="text-xs text-gray-500 mt-1">
            Supported formats: *.jpg, *.png, *.gif, *.webp. Max size: 5 MB.
          </div>
        </div>

        <!-- Basic Info -->
        <div class="mb-6">
          <h4 class="text-lg font-semibold mb-3 text-primary-600">Basic Information</h4>
          <div class="mb-4">
            <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Character
              Name</label>
            <input id="name" v-model="character.name" type="text"
                   class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"
                   placeholder="Enter character name"/>
          </div>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div class="mb-4">
              <label for="race" class="block text-sm font-medium text-gray-700 mb-1">Race</label>
              <select id="race" v-model="character.race"
                      class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option value="" disabled>Select a race</option>
                <option v-for="race in races" :key="race" :value="race">{{ race }}</option>
              </select>
            </div>
            <div class="mb-4">
              <label for="class" class="block text-sm font-medium text-gray-700 mb-1">Class</label>
              <select id="class" v-model="character.characterClass"
                      class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500">
                <option value="" disabled>Select a class</option>
                <option v-for="charClass in characterClasses" :key="charClass" :value="charClass">
                  {{ charClass }}
                </option>
              </select>
            </div>
          </div>
          <div class="mb-4">
            <label for="level" class="block text-sm font-medium text-gray-700 mb-1">Level
              (1-20)</label>
            <input id="level" v-model.number="character.level" type="number" min="1" max="20"
                   class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"/>
          </div>
        </div>

        <!-- Character Stats -->
        <div class="mb-6">
          <h4 class="text-lg font-semibold mb-3 text-primary-600">Character Stats</h4>
          <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
            <div class="mb-4" v-for="(value, stat) in character.playerCharacterData" :key="stat">
              <label :for="stat" class="block text-sm font-medium text-gray-700 mb-1 capitalize">{{
                  stat
                }}</label>
              <input :id="stat" v-model.number="character.playerCharacterData[stat]" type="number"
                     min="1" max="30"
                     class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500"/>
            </div>
          </div>
        </div>

        <!-- Buttons -->
        <div class="flex justify-end space-x-3 mt-8">
          <button
            type="button"
            @click="router.push(goToCharacterView())"
            class="px-4 py-2 border rounded-md border-third-300 button-secondary"
            :disabled="isSubmitting"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="px-4 py-2 border rounded-md border-third-300 button-add"
            :disabled="isSubmitting"
          >
            <span v-if="isSubmitting">Saving...</span>
            <span v-else>Save Changes</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
