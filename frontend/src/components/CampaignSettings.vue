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

const toggleRole = (participant) => {
  // Logic to toggle role between player and GM
};

const addParticipant = () => {
  // Logic to add a new participant to the campaign
};

const removeParticipant = (participant) => {
  // Logic to remove participant from campaign
};

const deleteCampaign = () => {
  // Logic to delete the campaign
};

const transferOwnership = () => {
  // Logic to transfer ownership of the campaign
};

</script>


<template>
  <div>
    <h4>Campaign Settings</h4>
    <div v-if="errorMessage" class="error-message">{{ errorMessage }}</div>
    <div>
      <label class="block text-sm font-medium text-primary-800 mb-1">Update your Nickname</label>
      <div class="p-2">
        <input
          v-model="nickname"
          type="text"
          class="bg-primary-50 p-3 rounded flex-grow"
          :readonly="!isEditingNickname"
          :disabled="isSaving"
          :placeholder="currentUserNickname || 'Enter your nickname'"
          @click="startEditingNickname"
        />
      </div>
      <div v-if="isEditingNickname" class="space-y-2">
        <div class="flex space-x-2">
          <button @click="saveNickname" class="button button-add" :disabled="isSaving">
            <span v-if="isSaving">Saving...</span>
            <span v-else>Save</span>
          </button>
          <button @click="cancelEditingNickname" class="button button-primary" :disabled="isSaving">
            Cancel
          </button>
        </div>
      </div>
    </div>

    <div v-if="isOwner">
      <button class="button button-add" @click="addParticipant">Add Player</button>
      <button class="button button-remove" @click="deleteCampaign">Delete Campaign</button>
      <button class="button button-update" @click="transferOwnership">Transfer Ownership</button>
      <h4>Manage Participants</h4>
      <ul>
        <li v-for="participant in campaign?.participants || []" :key="participant.id">
          <div v-if="editingParticipantId !== participant.id">
            {{ participant.nickname }} - {{ participant.role }}
            <button class="button button-update" @click="toggleRole(participant)">Set Role
              (Player/GM)
            </button>
            <button class="button button-remove" @click="removeParticipant(participant)">Remove from
              campaign
            </button>
            <button class="button button-update" @click="updateNicknameForParticipant(participant)">
              Update Nickname
            </button>
          </div>
          <div v-else>
            <input
              v-model="participant.nickname"
              type="text"
              class="bg-primary-50 p-3 rounded flex-grow"
              :disabled="isSaving"
              placeholder="Enter new nickname"
            />
            <button @click="saveParticipantNickname(participant)" class="button button-add"
                    :disabled="isSaving">
              <span v-if="isSaving">Saving...</span>
              <span v-else>Save</span>
            </button>
            <button @click="editingParticipantId = null" class="button button-primary"
                    :disabled="isSaving">
              Cancel
            </button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
/* Add your styles here */
</style>
