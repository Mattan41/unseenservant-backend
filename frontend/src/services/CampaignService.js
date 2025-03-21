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
};

export default CampaignService;
