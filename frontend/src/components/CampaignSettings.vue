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

const campaignStore = useCampaignStore();
const userStore = useUserStore();

const campaign = ref(null);

// Load campaign data before rendering the component
const loadCampaignData = async () => {
  try {
    campaign.value = await campaignStore.fetchCampaign(props.campaignId);
  } catch (error) {
    console.error('Failed to load campaign:', error);
  }
};

// Use a computed property determine ownership based on the campaign data
const isOwner = computed(() => {
  if (!campaign.value || !userStore.userInfo) return false;
  return campaign.value.ownerId === userStore.userInfo.id;
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


const nickname = ref('');

const toggleRole = (participant) => {
  // Logic to toggle role between player and GM
};

const removeParticipant = (participant) => {
  // Logic to remove participant from campaign
};

const addParticipant = () => {
  // Logic to add a new participant to the campaign
};

const deleteCampaign = () => {
  // Logic to delete the campaign
};

const transferOwnership = () => {
  // Logic to transfer ownership of the campaign
};

const updateNickname = () => {
  // Logic to update the user's nickname
};
</script>


<template>
  <div>
    <h4>Campaign Settings</h4>
    <button class="button button-primary">
      IMPORT CHARACTER
    </button>
    <div v-if="isOwner">
      <button class="button button-primary">
        ADD PLAYERS
      </button>
      <h4>Manage Participants</h4>
      <ul>
        <li v-for="participant in campaign?.participants || []" :key="participant.id">
          {{ participant.nickname }} - {{ participant.role }}
          <button @click="toggleRole(participant)">Toggle Role</button>
          <button @click="removeParticipant(participant)">Remove</button>
        </li>
      </ul>
      <button @click="addParticipant">Add Participant</button>
      <button @click="deleteCampaign">Delete Campaign</button>
      <button @click="transferOwnership">Transfer Ownership</button>
    </div>
    <div>
      <h3>Update Nickname</h3>
      <input v-model="nickname" placeholder="Enter new nickname"/>
      <button @click="updateNickname">Update</button>
    </div>
  </div>
</template>

<style scoped>
/* Add your styles here */
</style>
