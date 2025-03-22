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
};

export default CampaignService;
