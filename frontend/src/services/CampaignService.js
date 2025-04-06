import axios from '@/lib/axios.js';

const CampaignService = {
  async fetchAllCampaigns() {
    const response = await axios.get('api/campaigns');
    return response.data;
  },

  async fetchAllCampaignsForCurrentUser() {
    const response = await axios.get('api/campaigns/me');
    return response.data;
  },

  async createCampaign(name, description) {
    const response = await axios.post('api/campaigns', {name, description});
    return response.data;
  },

  async fetchCampaign(id) {
    const response = await axios.get(`api/campaigns/${id}`);
    return response.data;
  },

  // Method for searching users to add to campaign uses the users endpoint
  async searchUsers(query) {
    const response = await axios.get(`api/users/search?query=${encodeURIComponent(query)}`);
    return response.data;
  },

  // remove this method if not used
  async updateCampaignField(campaignId, field, value) {
    const response = await axios.patch(`api/campaigns/${campaignId}`, {field, value});
    return response.data;
  },

  async addParticipants(campaignId, participantsToAdd) {
    const response = await axios.patch(`api/campaigns/${campaignId}/participants`, {
      participantsToAdd: participantsToAdd,
      participantIdsToRemove: []
    });
    return response.data;
  },

  async removeParticipants(campaignId, participantIdsToRemove) {
    const response = await axios.patch(`api/campaigns/${campaignId}/participants`, {
      participantsToAdd: [],
      participantIdsToRemove: participantIdsToRemove
    });
    return response.data;
  },

  async updateParticipantNickname(campaignId, participantId, nickname) {
    const response = await axios.patch(`api/campaigns/${campaignId}/participants/${participantId}/nickname`, nickname, {
    });
    return response.data;
  },

  async updateParticipantRole(campaignId, participantId, role) {
    const response = await axios.patch(`api/campaigns/${campaignId}/participants/${participantId}/role`, role, {});
    return response.data;
  }

};

export default CampaignService;
