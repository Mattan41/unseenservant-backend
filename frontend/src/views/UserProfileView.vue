<script setup>
import {computed, onMounted, ref} from 'vue';
import {useUserStore} from '../stores/userStore';

const userStore = useUserStore();
const displayName = ref('');
const isEditing = ref(false);
const isSaving = ref(false);
const errorMessage = ref('');
const successMessage = ref('');

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
  errorMessage.value = '';
  successMessage.value = '';
};

const cancelEditing = () => {
  isEditing.value = false;
  displayName.value = userStore.getDisplayName;
};

const saveDisplayName = async () => {
  if (!displayName.value.trim()) {
    errorMessage.value = 'Display name cannot be empty';
    return;
  }

  try {
    isSaving.value = true;
    errorMessage.value = '';

    await userStore.updateProfileField('displayName', displayName.value.trim());

    isEditing.value = false;
    successMessage.value = 'Display name updated successfully!';

    // Clear success message after 3 seconds
    setTimeout(() => {
      successMessage.value = '';
    }, 3000);
  } catch (error) {
    errorMessage.value = error.message || 'Failed to update display name';
  } finally {
    isSaving.value = false;
  }
};
</script>

<template>
  <div class="container mx-auto p-6 max-w-4xl">
    <div class="bg-white rounded-lg shadow-lg overflow-hidden">
      <div class="bg-primary-500 p-6 text-white">
        <h1 class="text-2xl font-bold">User Profile</h1>
      </div>

      <div class="p-6">
        <!-- Loading state -->
        <div v-if="userStore.isLoadingProfile" class="text-center p-8">
          <div
            class="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-primary-500"></div>
          <p class="mt-2">Loading your profile...</p>
        </div>

        <!-- Error state -->
        <div v-else-if="userStore.error"
             class="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded">
          <p>{{ userStore.error }}</p>
          <button @click="userStore.fetchCurrentUser()"
                  class="mt-2 text-white bg-red-500 hover:bg-red-600 px-4 py-2 rounded">
            Try Again
          </button>
        </div>

        <!-- Success message -->
        <div v-if="successMessage"
             class="mb-4 bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded">
          {{ successMessage }}
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
                  <button @click="startEditing"
                          class="ml-3 bg-primary-500 text-white px-4 py-2 rounded hover:bg-primary-600 focus:outline-none focus:ring-2 focus:ring-primary-500">
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

                  <div v-if="errorMessage" class="text-red-600 text-sm">{{ errorMessage }}</div>

                  <div class="flex space-x-2">
                    <button
                      @click="saveDisplayName"
                      class="bg-green-500 text-white px-4 py-2 rounded hover:bg-green-600 focus:outline-none focus:ring-2 focus:ring-green-500 disabled:opacity-50"
                      :disabled="isSaving"
                    >
                      <span v-if="isSaving">Saving...</span>
                      <span v-else>Save</span>
                    </button>

                    <button
                      @click="cancelEditing"
                      class="bg-gray-300 text-gray-800 px-4 py-2 rounded hover:bg-gray-400 focus:outline-none focus:ring-2 focus:ring-gray-400"
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
                <div class="bg-gray-100 p-3 rounded">{{ user.role || 'Standard User' }}</div>
              </div>
            </div>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* Add any component-specific styles here */
</style>
