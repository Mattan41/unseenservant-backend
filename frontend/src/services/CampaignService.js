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


  // this method updates the campaign name and description together
  async updateCampaignInfo(campaignId, {name, description}) {
    const response = await axios.put(`api/campaigns/${campaignId}`, {name, description});
    return response.data;
  },

  // for updating a single field, such as name, description, etc. TODO remove this method
  // async updateCampaignField(campaignId, field, value) {
  //   const payload = {};
  //   payload[field] = value;
  //   const response = await axios.patch(`api/campaigns/${campaignId}`, payload);
  //   return response.data;
  // },

  // Image url
  async updateCampaignImage(campaignId, imageUrl) {
    const response = await axios.patch(`/api/campaigns/${campaignId}/image`,
      {imageUrl}, {}
    );
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
    const response = await axios.patch(`api/campaigns/${campaignId}/participants/${participantId}/nickname`,
      nickname,
      {
        headers: {'Content-Type': 'text/plain'}
      }
    );
    return response.data;
  },

  async updateParticipantRole(campaignId, participantId, role) {
    const response = await axios.patch(`api/campaigns/${campaignId}/participants/${participantId}/role`, role, {
      headers: {'Content-Type': 'text/plain'}
    });
    return response.data;
  }

};

export default CampaignService;
