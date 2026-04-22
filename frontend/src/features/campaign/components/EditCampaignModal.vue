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

      <!-- Campaign Image Upload -->
      <div class="mb-3">
        <label class="block text-sm font-medium text-gray-700 mb-1">Campaign Image</label>
        <div class="flex items-center space-x-4">
          <div class="relative">
            <img
              v-if="previewImageUrl"
              :src="previewImageUrl"
              alt="Campaign image preview"
              class="w-24 h-24 rounded-lg object-cover border-2 border-primary-300"
            />
            <div
              v-if="previewImageUrl"
              @click="triggerFileInput"
              class="absolute inset-0 bg-black bg-opacity-50 rounded-lg flex items-center justify-center opacity-0 hover:opacity-100 transition-opacity cursor-pointer"
            >
              <span class="text-white text-sm">Change</span>
            </div>
          </div>
          <input
            type="file"
            ref="fileInput"
            @change="handleImageChange"
            accept=".jpg,.jpeg,.png,.gif,.webp"
            class="hidden"
          />
          <button type="button" @click="triggerFileInput" class="button button-secondary">
            {{ previewImageUrl ? 'Change image' : 'Upload image' }}
          </button>
        </div>
        <div class="text-xs text-gray-500 mt-1">
          Supported formats: *.jpg, *.png, *.gif, *.webp. Max size: 5 MB.
        </div>
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
import { ref, computed } from 'vue';

const props = defineProps({
  campaign: {
    type: Object,
    required: true
  }
});

const emit = defineEmits(['close', 'save']);

const editedName = ref(props.campaign.title || '');
const editedDescription = ref(props.campaign.description || '');
const fileInput = ref(null);
const selectedFile = ref(null);
const localPreviewUrl = ref(null);
const isUpdating = ref(false);

const previewImageUrl = computed(() => localPreviewUrl.value || props.campaign.imageUrl || null);

function triggerFileInput() {
  fileInput.value.click();
}

function handleImageChange(event) {
  const file = event.target.files[0];
  if (file) {
    selectedFile.value = file;
    localPreviewUrl.value = URL.createObjectURL(file);
  }
}

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
      imageFile: selectedFile.value,
    };

    emit('save', updatedData);
  } catch (error) {
    console.error('Error saving campaign:', error);
  } finally {
    isUpdating.value = false;
  }
}
</script>
