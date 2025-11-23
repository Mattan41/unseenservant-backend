<script setup>
import {ref} from 'vue'
import {useUserStore} from './userStore.js'
import {storeToRefs} from 'pinia'
import {useNotificationStore} from '@/stores/notificationStore.js'
import CampaignList from '@/features/campaign/components/CampaignList.vue'
import CharacterList from '@/features/character/components/CharacterList.vue'

const userStore = useUserStore()
const notificationStore = useNotificationStore()

const { displayName, currentUser, isLoading, userRole } = storeToRefs(userStore)

const editForm = ref({
  displayName: '',
  isEditing: false,
  isSaving: false,
})

const startEditing = () => {
  editForm.value.displayName = displayName.value
  editForm.value.isEditing = true
}

const cancelEditing = () => {
  editForm.value.isEditing = false
  editForm.value.displayName = ''
}

const saveDisplayName = async () => {
  if (!editForm.value.displayName.trim()) {
    notificationStore.addNotification('Display name cannot be empty', 'error')
    return
  }

  editForm.value.isSaving = true
  const success = await userStore.updateUserField('displayName', editForm.value.displayName.trim())

  if (success) {
    editForm.value.isEditing = false
    editForm.value.displayName = ''
  }
  editForm.value.isSaving = false
}
</script>

<template>
  <div class="container mx-auto p-6 max-w-4xl">
    <div class="rounded-lg shadow-lg overflow-hidden">
      <div class="bg-primary-500 p-6">
        <h2>User Profile</h2>
      </div>

      <div class="p-6">
        <!-- Loading state -->
        <div v-if="isLoading" class="text-center p-8">
          <div
            class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"
          ></div>
          <p class="mt-2">Loading your profile...</p>
        </div>

        <!-- Profile content -->
        <div v-else-if="currentUser" class="space-y-6">
          <!-- Basic info section -->
          <section class="bg-gray-50 p-4 rounded-lg">
            <h2 class="text-xl font-semibold mb-4 text-primary-700">Basic Information</h2>

            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Display Name</label>

                <!-- View mode - uppdateras automatiskt! -->
                <div v-if="!editForm.isEditing" class="flex justify-between items-center">
                  <div class="bg-gray-100 p-3 rounded flex-grow">
                    {{ displayName }}
                  </div>
                  <button @click="startEditing" class="button button-primary ml-2">Edit</button>
                </div>

                <!-- Edit mode -->
                <div v-else class="space-y-2">
                  <input
                    v-model="editForm.displayName"
                    type="text"
                    class="w-full p-2 border rounded focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    :disabled="editForm.isSaving"
                    placeholder="Enter display name"
                  />

                  <div class="flex space-x-2">
                    <button
                      @click="saveDisplayName"
                      class="button button-add"
                      :disabled="editForm.isSaving"
                    >
                      {{ editForm.isSaving ? 'Saving...' : 'Save' }}
                    </button>

                    <button
                      @click="cancelEditing"
                      class="button button-primary"
                      :disabled="editForm.isSaving"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </div>

              <!-- Email -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <div class="bg-gray-100 p-3 rounded">
                  {{ currentUser.email || 'No email provided' }}
                </div>
              </div>
            </div>
          </section>

          <!-- Account details section -->
          <section class="bg-gray-50 p-4 rounded-lg">
            <h2 class="text-xl font-semibold mb-4 text-primary-700">Account Details</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Account Type</label>
                <div class="bg-gray-100 p-3 rounded">{{ userRole }}</div>
              </div>
            </div>
          </section>

          <!-- Campaigns section -->
          <section class="bg-primary-500 p-4 rounded-lg">
            <CampaignList />
          </section>

          <!-- Characters section -->
          <section class="bg-primary-500 p-4 rounded-lg">
            <h2 class="text-xl font-semibold mb-4 text-primary-700">Your Characters</h2>
            <CharacterList />
          </section>
        </div>
      </div>
    </div>
  </div>
</template>
