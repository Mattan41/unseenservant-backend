<template>
  <div class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50">
    <div class="bg-white rounded-lg shadow-lg max-w-md w-full p-5 max-h-[90vh] overflow-y-auto">
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-medium">Edit Campaign</h3>
        <button @click="emitClose" class="text-gray-500 hover:text-gray-700">
          <span class="sr-only">Close</span>
          <svg class="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <!-- Campaign Name -->
      <div class="mb-3">
        <label for="campaign-name" class="block text-sm font-medium text-gray-700 mb-1">
          Campaign Name
        </label>
        <input
          id="campaign-name"
          v-model="editedName"
          type="text"
          class="input input-bordered w-full mb-3"
          placeholder="Enter campaign name"
        />
      </div>

      <!-- Campaign Description -->
      <div class="mb-3">
        <label for="campaign-description" class="block text-sm font-medium text-gray-700 mb-1">
          Description
        </label>
        <textarea
          id="campaign-description"
          v-model="editedDescription"
          class="textarea textarea-bordered w-full"
          rows="4"
          placeholder="Enter campaign description"
        ></textarea>
      </div>

      <!-- Campaign Image URL -->
      <div class="mb-3">
        <label for="campaign-image-url" class="block text-sm font-medium text-gray-700 mb-1">
          Image URL
        </label>
        <input
          id="campaign-image-url"
          v-model="editedImageUrl"
          type="text"
          class="input input-bordered w-full mb-3"
          placeholder="Enter image URL"
        />
      </div>

      <!-- Preview if URL exists -->
      <div v-if="editedImageUrl" class="mb-3">
        <p class="text-sm font-medium mb-1">Preview:</p>
        <img
          :src="editedImageUrl"
          alt="Preview"
          class="max-h-32 rounded object-contain bg-gray-100"
          @error="(e) => (e.target.src = 'https://via.placeholder.com/150?text=Invalid+Image+URL')"
        />
      </div>

      <div class="flex space-x-3">
        <button
          @click="emitClose"
          class="button button-secondary"
          :disabled="isUpdating"
        >
          Cancel
        </button>
        <button @click="saveChanges" class="button button-primary" :disabled="isUpdating">
          {{ isUpdating ? 'Saving...' : 'Save Changes' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

const props = defineProps({
  campaign: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'save']);

const editedName = ref(props.campaign.title || '');
const editedDescription = ref(props.campaign.description || '');
const editedImageUrl = ref(props.campaign.imageUrl || '');
const isUpdating = ref(false);

function emitClose() {
  emit('close');
}

async function saveChanges() {
  isUpdating.value = true;

  try {
    const updatedData = {
      id: props.campaign.id,
      title: editedName.value,
      description: editedDescription.value,
      imageUrl: editedImageUrl.value
    };

    emit('save', updatedData);
  } catch (error) {
    console.error('Error saving campaign:', error);
  } finally {
    isUpdating.value = false;
  }
}
</script>
