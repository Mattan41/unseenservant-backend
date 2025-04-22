<script setup>
import {computed, onMounted, ref} from 'vue';
import {useUserStore} from './userStore.js';
import CampaignList from '@/features/campaign/components/CampaignList.vue';
import {useNotificationStore} from "@/stores/notificationStore.js";
import CharacterList from "@/features/character/components/CharacterList.vue";


const userStore = useUserStore();
const displayName = ref('');
const isEditing = ref(false);
const isSaving = ref(false);
const notificationStore = useNotificationStore();

// Get current user information
const user = computed(() => userStore.userInfo);

onMounted(async () => {
  if (!userStore.userInfo) {
    await userStore.fetchCurrentUser();
  }
  displayName.value = userStore.getDisplayName;
});

const startEditing = () => {
  displayName.value = userStore.getDisplayName;
  isEditing.value = true;
};

const cancelEditing = () => {
  isEditing.value = false;
  displayName.value = userStore.getDisplayName;
};

const saveDisplayName = async () => {
  if (!displayName.value.trim()) {
    notificationStore.addNotification("Display name cannot be empty", "error");
    return;
  }

  isSaving.value = true;
  const success = await userStore.updateProfileField('displayName', displayName.value.trim());

  if (success) {
    isEditing.value = false;
  }
  isSaving.value = false;
};
</script>

<template>
  <div class="container mx-auto p-6 max-w-4xl">
    <div class="rounded-lg shadow-lg overflow-hidden">
      <div class="bg-primary-500 p-6">
        <h2>User Profile</h2>
      </div>

      <div class="p-6">
        <!-- Loading state -->
        <div v-if="userStore.isLoadingProfile" class="text-center p-8">
          <div
            class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
          <p class="mt-2">Loading your profile...</p>
        </div>

        <!-- Profile content -->
        <div v-if="user" class="space-y-6">
          <!-- Basic info section -->
          <section class="bg-gray-50 p-4 rounded-lg">
            <h2 class="text-xl font-semibold mb-4 text-primary-700">Basic Information</h2>

            <div class="space-y-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Display Name</label>
                <div v-if="!isEditing" class="flex justify-between items-center">
                  <div class="bg-gray-100 p-3 rounded flex-grow">{{
                      userStore.getDisplayName
                    }}
                  </div>
                  <button @click="startEditing" class="button button-primary">
                    Edit
                  </button>
                </div>

                <div v-else class="space-y-2">
                  <input
                    v-model="displayName"
                    type="text"
                    class="w-full p-2 border rounded focus:ring-2 focus:ring-primary-500 focus:border-primary-500"
                    :disabled="isSaving"
                    placeholder="Enter display name"
                  />

                  <div class="flex space-x-2">
                    <button
                      @click="saveDisplayName"
                      class="button button-add"
                      :disabled="isSaving"
                    >
                      <span v-if="isSaving">Saving...</span>
                      <span v-else>Save</span>
                    </button>

                    <button
                      @click="cancelEditing"
                      class="button button-primary"
                      :disabled="isSaving"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              </div>

              <!-- Email -->
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Email</label>
                <div class="bg-gray-100 p-3 rounded">{{ user.email || 'No email provided' }}</div>
              </div>
            </div>
          </section>

          <!-- Account details section -->
          <section class="bg-gray-50 p-4 rounded-lg">
            <h2 class="text-xl font-semibold mb-4 text-primary-700">Account Details</h2>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="block text-sm font-medium text-gray-700 mb-1">Account Type</label>
                <div class="bg-gray-100 p-3 rounded">{{ userStore.getRole }}</div>
              </div>
            </div>
          </section>

          <!-- Campaigns section -->
          <section class="bg-primary-500 p-4 rounded-lg">
            <CampaignList/>
          </section>
          <section class="bg-primary-500 p-4 rounded-lg">
            <h2 class="text-xl font-semibold mb-4 text-primary-700">Your Characters</h2>
            <CharacterList/>
          </section>

        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Add any component-specific styles here */
</style>
