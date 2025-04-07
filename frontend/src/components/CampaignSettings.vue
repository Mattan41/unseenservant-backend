<script setup>
import {computed, ref, watch} from 'vue';
import {useCampaignStore} from '@/stores/campaignStore';
import {useUserStore} from "@/stores/userStore.js";

const props = defineProps({
  campaignId: {
    type: String,
    required: true
  }
});

const emit = defineEmits(['participants-updated']);

const campaignStore = useCampaignStore();
const userStore = useUserStore();

const campaign = ref(null);
const nickname = ref('');
const isEditingNickname = ref(false);
const isSaving = ref(false);
const errorMessage = ref('');
const editingParticipantId = ref(null);
const updatingRoles = ref(new Set());

// Search functionality
const searchTerm = ref('');
const searchResults = ref([]);
const isSearching = ref(false);

// Load campaign data before rendering the component
const loadCampaignData = async () => {
  try {
    campaign.value = await campaignStore.fetchCampaign(props.campaignId);
    nickname.value = campaign.value.nickname;
  } catch (error) {
    console.error('Failed to load campaign:', error);
  }
};

// Use a computed property determine ownership based on the campaign data
const isOwner = computed(() => {
  if (!campaign.value || !userStore.userInfo) return false;
  return campaign.value.ownerId === userStore.userInfo.id;
});

// Computed property to get the current user's nickname
const currentUserNickname = computed(() => {
  if (!campaign.value || !userStore.userInfo) return '';
  const participant = campaign.value.participants.find(p => p.id === userStore.userInfo.id);
  return participant ? participant.nickname : '';
});

// Make sure user data is loaded
const loadUserData = async () => {
  if (!userStore.userInfo) {
    await userStore.fetchCurrentUser();
  }
};

// Load data when the component is mounted and when the campaign ID changes
loadUserData();
watch(() => props.campaignId, loadCampaignData, {immediate: true});

// Search for users by username or email
const searchUsers = async () => {
  if (!searchTerm.value.trim()) return;

  try {
    isSearching.value = true;
    errorMessage.value = '';

    const users = await campaignStore.searchUsers(searchTerm.value);

    // Filter out users who are already participants
    searchResults.value = users.filter(user =>
      !campaign.value.participants.some(p => p.id === user.id)
    );
    // If no users found, set an error message - display for  3 seconds
    if (searchResults.value.length === 0) {
      errorMessage.value = `No users found matching "${searchTerm.value}"`;
      setTimeout(() => {
        errorMessage.value = '';
      }, 3000);
    }

  } catch (error) {
    errorMessage.value = error.message || 'Failed to search users';
    console.error('Search error:', error);
  } finally {
    isSearching.value = false;
  }
};

// nickname editing functions
const startEditingNickname = () => {
  isEditingNickname.value = true;
  errorMessage.value = '';
};
const cancelEditingNickname = () => {
  isEditingNickname.value = false;
  nickname.value = campaign.value.nickname;
};

// Function to save the updated the nickname for a participant
const saveParticipantNickname = async (participant) => {
  if (!participant.nickname.trim()) {
    errorMessage.value = 'Nickname cannot be empty';
    return;
  }

  try {
    isSaving.value = true;
    errorMessage.value = '';
    await campaignStore.updateParticipantNickname(props.campaignId, participant.id, participant.nickname.trim());

    editingParticipantId.value = null;

    emit('participants-updated', 'Nickname updated successfully!');

  } catch (error) {
    errorMessage.value = error.message || 'Failed to update participant nickname';
  } finally {
    isSaving.value = false;
  }
};
// Function to save the current user's nickname
const saveNickname = async () => {
  const participant = {
    id: userStore.userInfo.id,
    nickname: nickname.value
  };
  try {
    await saveParticipantNickname(participant);
    isEditingNickname.value = false;
  } catch (error) {
    errorMessage.value = error.message || 'Failed to update nickname';
  }
};
const updateNicknameForParticipant = (participant) => {
  editingParticipantId.value = participant.id;
};

const addParticipant = async (user) => {

  try {
    // Convert campaignId to number if needed
    const campaignIdNum = Number(props.campaignId);

    // call the store method to add the participant
    await campaignStore.addParticipantsToCampaign(
      campaignIdNum,
      [
        {id: user, nickname: user.username, role: 'PLAYER'}]
    );

    // remove the added user from the search results
    searchResults.value = searchResults.value.filter(u => u.id !== user.id);

    emit('participants-updated', `Participant ${user.displayName || user.username} added successfully!`);


  } catch (error) {
    errorMessage.value = error.message || 'Failed to add participant';
    console.error('Failed to add participant:', error);
  }

};

const removeParticipant = async (participant) => {
  if (!confirm(`Are you sure you want to remove ${participant.nickname || 'this participant'}?`))
    return;
  try {
    // Convert campaignId to number if needed
    const campaignIdNum = Number(props.campaignId);

    await campaignStore.removeParticipantsFromCampaign(
      campaignIdNum,
      [participant.id]
    );
    emit('participants-updated', `Participant ${participant.nickname || participant.displayName || participant.username || participant.name} removed successfully!`);

  } catch (error) {
    errorMessage.value = error.message || 'Failed to remove participant';
    console.error('Failed to remove participant:', error);
  }
};

const toggleRole = async (participant) => {

  try {
    // set a participant as updating
    updatingRoles.value.add(participant.id);

    const newRole = participant.role === 'PLAYER' ? 'GM' : 'PLAYER';

    // call the store method to update the participant's role
    await campaignStore.updateParticipantRole(
      props.campaignId,
      participant.id,
      newRole
    );

    emit('participants-updated', `${participant.nickname || participant.displayName || participant.username || participant.name} updated to ${newRole}`);
  } catch (error) {
    errorMessage.value = error.message || `Failed to update role for ${participant.displayName || participant.username}`;
    console.error('Failed to toggle role:', error);
  } finally {
    updatingRoles.value.delete(participant.id);
  }
};


const deleteCampaign = () => {
  // todo: add Logic to delete the campaign
};

const transferOwnership = () => {
  // todo: add Logic to transfer ownership of the campaign
};
// todo: add possibility to add picture to the campaign, and use a generic picture if it is not set

</script>

<template>
  <div class="bg-primary-200 rounded-lg shadow-md p-4">
    <h3 class="text-xl font-bold mb-4">Campaign Settings</h3>

    <!-- Error message -->
    <div v-if="errorMessage" class="error-message bg-red-100 text-red-700 p-3 rounded mb-4">
      {{ errorMessage }}
    </div>

    <!-- Non-owner settings -->
    <div v-if="!isOwner" class="mb-6">
      <div class="border rounded-lg p-4">
        <h5 class="font-medium mb-2">Your Nickname in Campaign</h5>

        <input
          v-model="nickname"
          type="text"
          class="bg-primary-50 p-3 rounded w-full mb-3"
          :readonly="!isEditingNickname"
          :disabled="isSaving"
          :placeholder="currentUserNickname || 'Enter your nickname'"
          @click="startEditingNickname"
        />

        <div v-if="isEditingNickname" class="flex gap-2">
          <button @click="saveNickname" class="button button-add flex-1" :disabled="isSaving">
            <span v-if="isSaving">Saving...</span>
            <span v-else>Save</span>
          </button>
          <button @click="cancelEditingNickname" class="button button-primary flex-1"
                  :disabled="isSaving">
            Cancel
          </button>
        </div>
      </div>
    </div>

    <!-- Owner-only settings -->
    <div v-if="isOwner" class="space-y-6">
      <!-- Add Participants Section -->
      <div class="border rounded-lg p-4">
        <h4 class="text-lg font-semibold mb-3">Add Participants</h4>

        <div class="flex flex-col sm:flex-row gap-2 mb-4">
          <input
            v-model="searchTerm"
            type="text"
            class="bg-primary-50 p-3 rounded flex-grow"
            placeholder="username or email"
            @keyup.enter="searchUsers"
          />

          <button @click="searchUsers" class="button button-primary" :disabled="isSearching">
            <span v-if="isSearching">Searching...</span>
            <span v-else>Search</span>
          </button>
        </div>

        <!-- Search results -->
        <div v-if="searchResults.length" class="mt-3 border-t pt-3">
          <h5 class="font-medium mb-2">Search Results</h5>

          <ul class="divide-y divide-gray-200">
            <li v-for="user in searchResults" :key="user.id"
                class="py-3 flex flex-col sm:flex-row sm:items-center sm:justify-between">
              <div class="mb-2 sm:mb-0">
                <div class="font-medium">{{ user.displayName || user.username }}</div>
                <div class="text-sm text-gray-500">{{ user.email }}</div>
              </div>
              <!--             todo: can we have a checkbox here instead of button? and add all selected users with a button -->
              <button @click="addParticipant(user.id)"
                      class="button button-add self-end sm:self-auto"
                      :disabled="isSaving">
                Add to Campaign
              </button>
            </li>
          </ul>
        </div>
      </div>

      <!-- Manage Participants Section -->
      <div class="border rounded-lg p-4">
        <h4 class="text-lg font-semibold mb-3">Manage Participants</h4>

        <ul class="space-y-4">
          <li v-for="participant in campaign?.participants || []"
              :key="participant.id"
              class="border rounded p-3">

            <!-- View mode -->
            <div v-if="editingParticipantId !== participant.id" class="space-y-3">
              <!-- Participant info -->
              <div class="flex justify-between items-center">
                <div class="font-medium">{{ participant.nickname }}</div>
                <div class="px-2 py-1 bg-primary-300 rounded text-sm">{{ participant.role }}</div>
              </div>

              <!-- Action buttons -->
              <div class="flex flex-wrap gap-2">
                <button class="button button-update text-sm py-1"
                        @click="toggleRole(participant)">
                  change to {{ participant.role === 'PLAYER' ? 'GM' : 'PLAYER' }}
                </button>
                <button class="button button-update text-sm py-1"
                        @click="updateNicknameForParticipant(participant)">
                  Edit Nickname
                </button>
                <button class="button button-remove text-sm py-1"
                        @click="removeParticipant(participant)">
                  Remove
                </button>
              </div>
            </div>

            <!-- Edit nickname mode -->
            <div v-else class="space-y-3">
              <input
                v-model="participant.nickname"
                type="text"
                class="bg-primary-50 p-2 rounded w-full"
                :disabled="isSaving"
                placeholder="Enter new nickname"
              />

              <div class="flex gap-2">
                <button @click="saveParticipantNickname(participant)"
                        class="button button-add text-sm py-1 flex-1"
                        :disabled="isSaving">
                  <span v-if="isSaving">Saving...</span>
                  <span v-else>Save</span>
                </button>
                <button @click="editingParticipantId = null"
                        class="button button-primary text-sm py-1 flex-1"
                        :disabled="isSaving">
                  Cancel
                </button>
              </div>
            </div>
          </li>
        </ul>

        <div v-if="(campaign?.participants || []).length === 0"
             class="text-center py-3 text-gray-500">
          No participants in this campaign yet.
        </div>
      </div>

      <!-- Campaign Management Buttons -->
      <div class="border rounded-lg p-4">
        <h4 class="text-lg font-semibold mb-3">Campaign Management</h4>

        <div class="flex flex-col sm:flex-row gap-3">
          <button class="button button-update" @click="transferOwnership">Transfer Ownership
          </button>
          <button class="button button-remove" @click="deleteCampaign">Delete Campaign</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
</style>
